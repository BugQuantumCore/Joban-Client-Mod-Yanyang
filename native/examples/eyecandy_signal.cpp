/**
 * eyecandy_signal.cpp — 红石控制信号灯 + 数码时钟 (eye_candy native port)
 *
 * Mirrors the JS eye_candy surface
 * (com.lx862.mtrscripting.mod.impl.mtr.eyecandy.*):
 *   - ctx.drawModel(model, matrices)      -> native draw_model
 *   - ctx.playSound(id, volume, pitch)    -> native play_sound
 *   - ctx.setOutlineShape / setCollisionShape
 *   - ctx.events().onBlockUse(cb)         -> snapshot block_use_events
 *   - be.redstoneLevel() / facing() / fullBrightness()
 *
 * The eye candy is a 1-block signal lamp that lights up with redstone
 * power, plus a dot-matrix clock face. Clicking (block use) toggles
 * a chime. The JS original would register onBlockUse callbacks and
 * read be.redstoneLevel() every frame — the native port reads the
 * same values from the flat snapshot.
 */
#include <mtr/script.hpp>
#include <mtr/gfx.hpp>
#include <mtr/util.hpp>
#include <cstdio>
#include <cstring>

using namespace mtr;

namespace {

constexpr int32_t FACE_W = 64;
constexpr int32_t FACE_H = 32;

struct SignalState {
    bool announced = false;   /* chime once per power-on */
};

struct SignalEyecandyScript : EyecandyScript<SignalState> {
    static constexpr auto ID = "demo:redstone_signal";

    GraphicsTexture face{};
    LcdCanvas canvas{face, 0xFF00E676, 0xFF062008};
    int32_t lamp_model = -1;

    void create(EyeCandyContext& ctx, SignalState& state,
                const EyeCandy& ec) override {
        (void)state;
        (void)ec;
        face.create(ctx.input(), FACE_W, FACE_H);
        if (ctx.host() && ctx.host()->acquire_model) {
            lamp_model = ctx.host()->acquire_model(
                ctx.host()->user, "demo:models/signal_lamp.json");
        }
    }

    void render(EyeCandyContext& ctx, SignalState& state, const EyeCandy& ec) override {
        const int32_t power = ec.redstone_level();

        /* --- Clock face (dot-matrix HH:MM from in-game time) --- */
        face.fill(0xFF040A04);
        const std::string clock = pids_util::format_time(ec.in_game_time(), true);
        canvas.draw_text(4, 2, clock.c_str());

        /* Powered indicator bar: 15 segments for 0..15. */
        for (int32_t i = 0; i < 15; i++) {
            const uint32_t color = (i < power) ? 0xFF00E676 : 0xFF0A2A12;
            face.set_pixel(4 + i * 4, 24, color);
            face.set_pixel(5 + i * 4, 24, color);
            face.set_pixel(4 + i * 4, 25, color);
            face.set_pixel(5 + i * 4, 25, color);
        }
        face.upload(ctx.frame());

        /* --- Lamp model: brightness pose scales with power --- */
        Matrices matrices{ctx.frame()};
        matrices.push_pose();
        matrices.translate(ec.translate_x(), ec.translate_y(), ec.translate_z());
        matrices.rotate_y_degrees(ec.rotate_y());
        if (power > 0) {
            const float glow = 1.0F + static_cast<float>(power) / 15.0F * 0.25F;
            matrices.scale(glow, glow, glow);
        }
        ctx.draw_model(lamp_model, &matrices);
        matrices.pop_pose();

        /* --- ctx.events().onBlockUse equivalent --- */
        if (ec.block_use_events() != 0) {
            /* JS: state.clicks++ ; play a chime per click */
            ctx.play_sound("demo:signal_click", 0.8F, 1.0F + power / 30.0F);
        }

        /* --- Power-on chime (mirrors JS state tracking) --- */
        if (power > 0 && !state.announced) {
            ctx.play_sound("demo:signal_on", 1.0F, 1.0F);
            state.announced = true;
        } else if (power == 0) {
            state.announced = false;
        }

        /* --- Collision/outline shape (1 block, base only) --- */
        if (first_shape_pass_) {
            static const float collision[6] = {0.0F, 0.0F, 0.0F, 1.0F, 0.4F, 1.0F};
            ctx.set_collision_shape(collision, 1);
            first_shape_pass_ = false;
        }
    }

    void dispose(EyeCandyContext&, SignalState&, const EyeCandy&) override {
        face.close(nullptr);
    }

private:
    bool first_shape_pass_ = true;
};

MTR_REGISTER_EYECANDY_SCRIPT(SignalEyecandyScript)

} /* anonymous namespace */
