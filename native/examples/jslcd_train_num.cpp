/**
 * jslcd_train_num.cpp — 车号系统 (native port of train_num.js +
 * draw_num.js from the community "JS LCD" pack).
 *
 * JS behaviour being ported:
 *   - 侧线名 (siding name) 携带车号与车厢号:
 *       "10010/01-02-03-04-05-06" → 车号 10010, 车厢 [01..06]
 *       "10010 Tc1 A1-1"          → 车号 10010, 编组 Tc1
 *   - 每节车厢两侧侧牌 (num_left / num_right, 1120×240) 绘制车厢号;
 *   - 车头牌 (vehicle_num_forwards) 与车尾牌 (backwards) 绘制车号;
 *   - 车厢数与车厢号数量不符 → 两侧牌绘制红色错误提示;
 *   - 透明背景 (AlphaComposite.CLEAR → clear-to-0x00000000);
 *   - 只在侧线名变化时重绘纹理 (JS 已有此优化), 每帧仅 drawCarModel;
 *   - dispose 释放全部 DisplayHelper (→ GraphicsTexture.close)。
 *
 * Native deltas (documented):
 *   - 1120×240 全分辨率保留 (小纹理, 无需降采样);
 *   - 模型四元组与 JS 的 num_leftPos / num_rightPos /
 *     getCubeVertices(...) 静态几何一致, 由宿主按路径引用
 *     (acquire_model refcount), 一侧一个共享 handle。
 *
 * Referenced from mtr_custom_resources.json:
 *   "vehicleScripts": [{ "id": "jslcd:train_num", "language": "cpp",
 *                        "nativeLibrary": "natives/libjslcd_train_num.so" }]
 */
#include "jslcd_common.hpp"

#include <algorithm>
#include <string>
#include <vector>

using namespace mtr;
using namespace jslcd;

namespace {

constexpr int32_t NUM_WIDTH = 1120;
constexpr int32_t NUM_HEIGHT = 240;
constexpr double NUM_FONT_SIZE_CAR = 60;    /* drawNum(g, num, 60)  */
constexpr double NUM_FONT_SIZE_HEAD = 30;   /* drawNum(g, num, 30)  */
const char* NUM_ERROR_TEXT = "请检查侧线名。|Please check the siding name.";

struct CarNumScreens {
    GraphicsTexture left;
    GraphicsTexture right;
};

struct TrainNumState {
    std::vector<CarNumScreens> cars;
    GraphicsTexture fwd;
    GraphicsTexture bwd;
    int32_t modelNumLeft = -1, modelNumRight = -1;
    int32_t modelFwd = -1, modelBwd = -1;
    std::string sidingNumBefore;   /* JS: state.sidingNumBefore */
    bool cleared = false;
};

/* ---- draw_num.js ---- */

/* clearTextureTransparent — AlphaComposite.CLEAR equivalent. */
void clear_texture_transparent(GraphicsTexture& tex) {
    tex.fill(0x00000000u);
}

/* drawNum — centered black text at (1120-w)/2, baseline 240-12. */
void draw_num(Gfx2D& g, const std::string& num, double font_size) {
    if (num.empty()) return;
    const double w = Gfx2D::text_width(font_size, num.c_str());
    const double x = (NUM_WIDTH - w) * 0.5;
    const double y = NUM_HEIGHT - 12;
    g.set_color(BLACK_COLOR);
    g.draw_text(x, y, font_size, num.c_str());
}

/* drawError — red bilingual error line. */
void draw_error(Gfx2D& g) {
    const double w = Gfx2D::text_width(60, NUM_ERROR_TEXT);
    const double x = (NUM_WIDTH - w) * 0.5;
    const double y = NUM_HEIGHT - 12;
    g.set_color(RED_COLOR);
    g.draw_text(x, y, 60, NUM_ERROR_TEXT);
}

struct TrainNumScript : VehicleScript<TrainNumState> {
    static constexpr auto ID = "jslcd:train_num";

    void create(VehicleContext& ctx, TrainNumState& state, const Train& train) override {
        ensure_cars(ctx.input(), state, train);
        /* head/tail plates are per-instance (JS: dhForwards/dhBackwards
           created once in create, only carDhsNum rebuilds on car count
           change) */
        if (state.fwd.width() == 0) state.fwd.create(ctx.input(), NUM_WIDTH, NUM_HEIGHT);
        if (state.bwd.width() == 0) state.bwd.create(ctx.input(), NUM_WIDTH, NUM_HEIGHT);

        if (ctx.host() && ctx.host()->acquire_model) {
            state.modelNumLeft = ctx.host()->acquire_model(
                ctx.host()->user, "jslcd:models/num_left.json");
            state.modelNumRight = ctx.host()->acquire_model(
                ctx.host()->user, "jslcd:models/num_right.json");
            state.modelFwd = ctx.host()->acquire_model(
                ctx.host()->user, "jslcd:models/vehicle_num_forwards.json");
            state.modelBwd = ctx.host()->acquire_model(
                ctx.host()->user, "jslcd:models/vehicle_num_backwards.json");
        }

        /* JS create(): 初始清成透明并上传一次 */
        for (CarNumScreens& cs : state.cars) {
            clear_texture_transparent(cs.left);
            clear_texture_transparent(cs.right);
        }
        clear_texture_transparent(state.fwd);
        clear_texture_transparent(state.bwd);
        state.sidingNumBefore.clear();
        state.cleared = true;
    }
    void render(VehicleContext& ctx, TrainNumState& state, const Train& train) override {
        const int carCount = train.car_count();
        ensure_cars(ctx.input(), state, train);

        /* 读取侧线名 (JS: "" + vehicle.getSiding().getName()) */
        std::string sidingNameNow(train.siding_name().str());

        /* 只在侧线名变化时更新纹理内容 (JS 同款短路) */
        if (!sidingNameNow.empty() && state.sidingNumBefore != sidingNameNow) {
            state.sidingNumBefore = sidingNameNow;

            const SidingInfo parsed = parse_siding_name(sidingNameNow);
            const std::string& vehicleNum = parsed.vehicleNum;
            const std::vector<std::string>& carNum = parsed.carNums;

            const bool mismatch = (carCount != (int)carNum.size());

            for (int i = 0; i < carCount && i < (int)state.cars.size(); i++) {
                CarNumScreens& cs = state.cars[i];
                clear_texture_transparent(cs.left);
                clear_texture_transparent(cs.right);

                Gfx2D gL(cs.left, ctx.host());
                Gfx2D gR(cs.right, ctx.host());
                if (mismatch) {
                    draw_error(gL);
                    draw_error(gR);
                } else {
                    draw_num(gL, carNum[i], NUM_FONT_SIZE_CAR);
                    draw_num(gR, carNum[i], NUM_FONT_SIZE_CAR);
                }
            }

            /* 车头牌 / 车尾牌 */
            {
                Gfx2D gF(state.fwd, ctx.host());
                clear_texture_transparent(state.fwd);
                draw_num(gF, vehicleNum, NUM_FONT_SIZE_HEAD);
            }
            {
                Gfx2D gB(state.bwd, ctx.host());
                clear_texture_transparent(state.bwd);
                draw_num(gB, vehicleNum, NUM_FONT_SIZE_HEAD);
            }
        }

        /* upload dirty rects + 每帧挂模型 (JS: dh.upload() + drawCarModel) */
        for (int i = 0; i < carCount && i < (int)state.cars.size(); i++) {
            CarNumScreens& cs = state.cars[i];
            cs.left.upload(ctx.frame());
            cs.right.upload(ctx.frame());
            ctx.draw_car_model(state.modelNumLeft, i, nullptr);
            ctx.draw_car_model(state.modelNumRight, i, nullptr);
        }
        state.fwd.upload(ctx.frame());
        state.bwd.upload(ctx.frame());
        if (carCount > 0) {
            ctx.draw_car_model(state.modelFwd, 0, nullptr);
            ctx.draw_car_model(state.modelBwd, carCount - 1, nullptr);
        }
    }

    void dispose(VehicleContext& ctx, TrainNumState& state, const Train&) override {
        for (CarNumScreens& cs : state.cars) {
            cs.left.close(&ctx.input());
            cs.right.close(&ctx.input());
        }
        state.cars.clear();
        state.fwd.close(&ctx.input());
        state.bwd.close(&ctx.input());
        auto rel = [&](int32_t h) {
            if (h != -1 && ctx.host() && ctx.host()->release_model) {
                ctx.host()->release_model(ctx.host()->user, h);
            }
        };
        rel(state.modelNumLeft);
        rel(state.modelNumRight);
        rel(state.modelFwd);
        rel(state.modelBwd);
        state.modelNumLeft = state.modelNumRight = state.modelFwd = state.modelBwd = -1;
    }

private:
    void ensure_cars(const JcmFrameInput& in, TrainNumState& state, const Train& train) {
        const int carCount = train.car_count();
        if ((int)state.cars.size() == carCount && state.cleared) return;
        state.cars.clear();
        state.sidingNumBefore.clear();
        state.cleared = false;
        for (int i = 0; i < carCount; i++) {
            CarNumScreens cs;
            cs.left.create(in, NUM_WIDTH, NUM_HEIGHT);
            cs.right.create(in, NUM_WIDTH, NUM_HEIGHT);
            state.cars.push_back(std::move(cs));
        }
        state.cleared = true;
    }
};

MTR_REGISTER_VEHICLE_SCRIPT(TrainNumScript)

} /* anonymous namespace */
