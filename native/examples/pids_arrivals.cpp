/**
 * pids_arrivals.cpp — 到站信息屏 (native port of jsblock pids_1a.js)
 *
 * Side-by-side with the JS original
 * (fabric/src/main/resources/assets/jsblock/scripts/builtin/pids_1a.js):
 * every JS statement has a same-shaped C++ line, including the
 * PIDSUtil helper port in mtr/util.hpp (pids_util.js).
 *
 * Referenced from a JCM PIDS JSON preset:
 *   { "id": "demo_arrivals", "scriptFiles": ["..."], ... }
 * with "language": "cpp" and a native library reference:
 *   { "id": "demo_arrivals", "language": "cpp",
 *     "nativeLibrary": "natives/libpids_arrivals.so" }
 */
#include <mtr/script.hpp>
#include <mtr/util.hpp>
#include <cstdio>
#include <cstring>
#include <string>

using namespace mtr;

namespace {

constexpr int TOP_PADDING = 5;
constexpr int SIDE_PADDING = 3;
constexpr int MAX_ARRIVALS = 3;
constexpr int COLOR_ORANGE = 0xFFFC9700;

struct ArrivalsState {
    CycleTracker cycle{"", 40};      /* TextUtil.cycleString equivalent */
    int64_t switch_flip_at = 0;
    bool show_cars = false;
};

struct ArrivalsPidsScript : PidsScript<ArrivalsState> {
    static constexpr auto ID = "demo:arrivals";

    void create(PidsContext& ctx, ArrivalsState& state, const Pids& pids) override {
        (void)ctx; (void)state; (void)pids;
    }

    void render(PidsContext& ctx, ArrivalsState& state, const Pids& pids) override {
        /* JS: let arrivalIdx = 0; */
        int32_t arrival_idx = 0;
        /* JS: let perRowHeight = (pids.height - TOP_PADDING) / MAX_ARRIVALS; */
        const double per_row_height =
            static_cast<double>(pids.height() - TOP_PADDING) / MAX_ARRIVALS;

        for (int32_t i = 0; i < MAX_ARRIVALS; i++) {
            /* JS: let rowY = TOP_PADDING + (i*perRowHeight); */
            const double row_y = TOP_PADDING + i * per_row_height;
            /* JS: let customMsg = pids.getCustomMessage(i); */
            const StrView custom_msg = pids.get_custom_message(i);
            /* JS: let arrival = pids.arrivals().get(arrivalIdx); */
            const Arrivals arrivals = pids.arrivals();
            const bool has_arrival = arrival_idx < arrivals.size();
            const Arrival arrival = has_arrival ? arrivals.at(arrival_idx) : Arrival(nullptr, nullptr);

            if (custom_msg.valid() && !custom_msg.empty()) {
                /* JS: Text.create("Custom Text").text(TextUtil.cycleString(customMsg))... */
                Text::create("Custom Text")
                    .text(custom_msg.str().c_str())
                    .scale(1.725)
                    .size((pids.width() / 1.725) - ((SIDE_PADDING / 1.725) * 2), 9)
                    .stretch_xy()
                    .font_mc()
                    .color(COLOR_ORANGE)
                    .pos(SIDE_PADDING, row_y)
                    .draw(ctx);
            } else if (has_arrival && !pids.is_row_hidden(i)) {
                /* JS: let routeNumber = TextUtil.cycleString(arrival.routeNumber()); */
                const std::string route_number = arrival.route_number().str();
                /* JS: let destinationStr = TextUtil.cycleString(arrival.destination()).trim(); */
                std::string destination_str = arrival.destination().str();

                /* JS: circularState() CLOCKWISE / ANTI_CLOCKWISE prefixes */
                if (arrival.circular_state() == 1) {
                    destination_str = "Clockwise via " + destination_str;
                } else if (arrival.circular_state() == 2) {
                    destination_str = "Anticlockwise via " + destination_str;
                }

                const std::string final_destination_display =
                    (route_number + " " + destination_str);

                /* JS: Text.create("Destination Text").text(finalDestinationDisplay)... */
                Text::create("Destination Text")
                    .text(final_destination_display.c_str())
                    .scale(1.725)
                    .size((pids.width() / 1.725) - 30 - ((SIDE_PADDING / 1.725) * 3), 9)
                    .stretch_xy()
                    .font_mc()
                    .color(COLOR_ORANGE)
                    .pos(SIDE_PADDING, row_y)
                    .draw(ctx);

                /* JS: PIDSUtil.getETAText(arrival.arrivalTime()) */
                const std::string eta_text = pids_util::eta_text(
                    arrival.arrival_time(), pids.game_time_millis());
                /* JS: PIDSUtil.getCarText(arrival.carCount()) */
                const std::string car_text = pids_util::car_text(arrival.car_count());

                std::string eta_or_car = eta_text;
                /* JS: let needShowCarText = pids.arrivals().mixedCarLength(); */
                const bool need_show_car_text = arrivals.mixed_car_length();
                if (need_show_car_text) {
                    eta_or_car += "|" + car_text;
                }
                const int64_t switch_duration = need_show_car_text ? 120 : 60;

                /* JS: TextUtil.cycleString(etaOrCarText, switchDuration) */
                const std::string cycled = cycle_string(
                    eta_or_car, switch_duration, pids.game_time_millis() / 50);

                /* JS: Text.create("ETA Text").text(...).rightAlign()... */
                Text::create("ETA Text")
                    .text(cycled.c_str())
                    .scale(1.725)
                    .size(30, 9)
                    .stretch_xy()
                    .right_align()
                    .font_mc()
                    .color(COLOR_ORANGE)
                    .pos(pids.width() - SIDE_PADDING, row_y)
                    .draw(ctx);

                arrival_idx++;
            }
        }

        /* Extra native touch (beyond the JS original): clock row. */
        const std::string clock = pids_util::format_time(pids.in_game_time(), true);
        Text::create("Clock")
            .text(clock.c_str())
            .scale(1.4)
            .size(20, 9)
            .right_align()
            .font_mc()
            .color(0xFF00E676)
            .pos(pids.width() - SIDE_PADDING, 1)
            .draw(ctx);
    }

private:
    /* TextUtil.cycleString(str, duration) — cycle "|" segments. */
    static std::string cycle_string(const std::string& s, int64_t period_ticks,
                                    int64_t game_tick) {
        if (s.find('|') == std::string::npos) return s;
        size_t start = 0;
        std::vector<std::string> parts;
        while (true) {
            const size_t pos = s.find('|', start);
            if (pos == std::string::npos) {
                parts.push_back(s.substr(start));
                break;
            }
            parts.push_back(s.substr(start, pos - start));
            start = pos + 1;
        }
        const size_t idx = static_cast<size_t>(
            (game_tick / period_ticks) % static_cast<int64_t>(parts.size()));
        return parts[idx];
    }
};

MTR_REGISTER_PIDS_SCRIPT(ArrivalsPidsScript)

} /* anonymous namespace */
