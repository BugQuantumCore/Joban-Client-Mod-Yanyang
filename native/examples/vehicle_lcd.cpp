/**
 * vehicle_lcd.cpp — 车侧 LCD 显示屏 + 车次号 (native port)
 *
 * The JS equivalent (MTR Steamloco / JCM style) paints an offscreen
 * GraphicsTexture with java.awt.Graphics2D and maps it onto the car
 * body via a RawMeshBuilder model (see mtrsteamloco/scripts/
 * display_helper.js). This native port paints the same dot-matrix
 * LCD entirely in C++ and ships dirty rects through the frame pixel
 * arena — per-pixel work never crosses the JVM boundary.
 *
 * Rendered content (128x32 dot-matrix, amber):
 *   Row 1: 车次号 (run number, derived from departure index)
 *          + destination (cycles via CycleTracker)
 *   Row 2: NEXT: <next stop>   (CJK falls back to host TTF)
 *   Row 3: door arrows < > (blink while doors open) + speed / arrival
 *
 * This file is compiled into libvehicle_lcd.so / vehicle_lcd.dll and
 * referenced from mtr_custom_resources.json:
 *
 *   "vehicles":    [{ "id": "demo:kcx", "scriptId": "demo:kcx_lcd", ... }],
 *   "vehicleScripts": [{
 *       "id": "demo:kcx_lcd",
 *       "language": "cpp",
 *       "nativeLibrary": "natives/libvehicle_lcd.so"
 *   }]
 */
#include <mtr/script.hpp>
#include <mtr/gfx.hpp>
#include <cstdio>
#include <cstring>

using namespace mtr;

namespace {

constexpr int32_t LCD_W = 128;         /* px, also UV-mapped 1:1 in the model */
constexpr int32_t LCD_H = 32;
constexpr uint32_t LCD_BG     = 0xFF080400;   /* near-black backlight */
constexpr uint32_t LCD_ON     = 0xFFFFB000;   /* amber dot */
constexpr uint32_t LCD_DIM    = 0xFF3A2600;   /* unlit dot ghost */
constexpr uint32_t LCD_GREEN  = 0xFF00E676;   /* run number accent */

struct KcxLcdState {
    /* Persistent per-train state, mirrors the JS `state` object. */
    CycleTracker destination_cycle{"TSUEN WAN|KWUN TONG", 120};
    char run_number[16] = "";
    bool chime_played = false;        /* door chime once per stop */
};

struct KcxLcdScript : VehicleScript<KcxLcdState> {
    static constexpr auto ID = "demo:kcx_lcd";

    GraphicsTexture lcd{};
    LcdCanvas canvas{lcd, LCD_ON, LCD_DIM};
    int32_t lcd_model = -1;

    /* ---- JS: function create(ctx, state, train) ---- */
    void create(VehicleContext& ctx, KcxLcdState& state, const Train& train) override {
        /* new GraphicsTexture(128, 32) + ModelManager.upload(mesh) */
        lcd.create(ctx.input(), LCD_W, LCD_H);

        /* Acquire the quad model that UV-maps the LCD texture.
           (JS: new RawMeshBuilder(4, "interior", white.png) + upload) */
        if (ctx.host() && ctx.host()->acquire_model) {
            lcd_model = ctx.host()->acquire_model(
                ctx.host()->user, "demo:models/lcd_quad.json");
        }

        /* 车次号: run number derived from departure index —
           same trick JS scripts use (vehicleWrapper.getDepartureIndex()). */
        std::snprintf(state.run_number, sizeof(state.run_number),
                      "T%04d", static_cast<int>(train.departure_index() % 10000));
    }

    /* ---- JS: function render(ctx, state, train) ---- */
    void render(VehicleContext& ctx, KcxLcdState& state, const Train& train) override {
        /* 1) Paint the LCD bitmap (pure C++ hot loop — the part that
              would run through java.awt in the JS pipeline). */
        paint_lcd(state, train);

        /* 2) Ship the dirty rect to the host (JS: texture.upload()). */
        lcd.upload(ctx.frame());

        /* 3) Draw the LCD quad on every rendered car, both sides
              (JS: ctx.drawCarModel(model, car, matrices)). */
        Matrices matrices{ctx.frame()};
        for (int32_t car = 0; car < train.car_count(); car++) {
            if (!train.car(car).rendered()) continue;

            /* left side */
            matrices.push_pose();
            matrices.translate(-0.26F, 1.05F, 0.0F);
            matrices.rotate_y_degrees(90.0F);
            ctx.draw_car_model(lcd_model, car, &matrices);
            matrices.pop_pose();

            /* right side (mirrored content) */
            matrices.push_pose();
            matrices.translate(0.26F, 1.05F, 0.0F);
            matrices.rotate_y_degrees(-90.0F);
            ctx.draw_car_model(lcd_model, car, &matrices);
            matrices.pop_pose();
        }

        /* 4) Door chime when doors start opening
              (JS: ctx.playAnnSound("mtr:door_chime", 1, 1)). */
        if (train.door_opening() && !state.chime_played) {
            ctx.play_ann_sound("demo:door_opening_chime", 1.0F, 1.0F);
            state.chime_played = true;
        }
        if (!train.door_opening()) {
            state.chime_played = false;
        }
    }

    void dispose(VehicleContext&, KcxLcdState&, const Train&) override {
        lcd.close(nullptr);
    }

private:
    void paint_lcd(KcxLcdState& state, const Train& train) {
        lcd.fill(LCD_BG);

        /* --- Row 1: 车次号 + destination (cycle every 120 ticks) --- */
        canvas.set_colors(LCD_GREEN, LCD_DIM);
        canvas.draw_text(2, 1, state.run_number);                 /* T0123 */
        canvas.set_colors(LCD_ON, LCD_DIM);
        const std::string dest = state.destination_cycle.value_str(
            train.game_time_millis() / 50);                       /* game tick */
        canvas.draw_text(46, 1, dest.c_str());

        /* --- Row 2: NEXT: next station --- */
        const StopList& stops = train.this_route_stops();
        int32_t next_idx = train.next_stop_index();
        if (next_idx < stops.size()) {
            const Stop next = stops.at(next_idx);
            char line[80];
            std::snprintf(line, sizeof(line), "NEXT: %s",
                          next.name().str().c_str());
            canvas.draw_text(2, 12, line);
        } else if (stops.size() > 0) {
            canvas.draw_text(2, 12, "TERMINUS - PLEASE EXIT");
        }

        /* --- Row 3: door arrows (blink 600ms) + speed --- */
        const bool blink_on = LcdCanvas::blink(train.game_time_millis(), 600);
        const Car car0 = train.car(0);
        if (train.door_value() > 0.0) {
            if (car0.left_door_open() && blink_on)  draw_arrow(2, 24, true);
            if (car0.right_door_open() && blink_on) draw_arrow(120, 24, false);
        }
        char tail[24];
        std::snprintf(tail, sizeof(tail), "%d KMH",
                      static_cast<int>(train.speed_kmh() + 0.5));
        const int32_t tw = LcdCanvas::text_width(tail);
        canvas.draw_text(LCD_W - 4 - tw, 24, tail);

        lcd.dirty_all();
    }

    /* 7x7 dot chevrons pointing at the opening doors. */
    void draw_arrow(int32_t x, int32_t y, bool left) {
        for (int32_t row = 0; row < 7; row++) {
            const int32_t d = row < 3 ? (3 - row) : (row - 3);
            const int32_t col = left ? (1 + d) : (5 - d);
            canvas.dot(x + col, y + row, true);
            canvas.dot(x + col + 1, y + row, true); /* 2px thick */
        }
    }
};

MTR_REGISTER_VEHICLE_SCRIPT(KcxLcdScript)

} /* anonymous namespace */
