/**
 * lcd_bench.cpp — real micro-benchmark for the native pipeline.
 *
 * Drives the exact same code path the MTR host would drive:
 *   mtrCreate -> (mtrRender x N) with a realistic vehicle snapshot
 * and measures per-frame cost, including LCD rasterization (the
 * 128x32 dot-matrix paint), draw-call recording, and frame output.
 *
 * The JS twin (lcd_bench.js) implements the same algorithm with the
 * same 5x7 font under the JS engine, so the numbers are directly
 * comparable (see the showcase page).
 *
 * Build:  g++ -O2 -std=c++17 -I../include bench_lcd.cpp \
 *             ../examples/vehicle_lcd.cpp -o lcd_bench
 */
#include <mtr/script.hpp>
#include <mtr/gfx.hpp>
#include <chrono>
#include <cstdio>
#include <cstring>
#include <cstdlib>

using namespace mtr;

/* Build a realistic MTR vehicle snapshot:
   8 cars, 12 stops with names, doors opening, moving at 80 km/h. */
static void build_snapshot(JcmVehicleSnapshot& snap, JcmCar cars[8],
                           JcmStop stops[12], char* pool, int32_t pool_cap) {
    std::memset(&snap, 0, sizeof(snap));
    std::memset(cars, 0, sizeof(JcmCar) * 8);
    std::memset(stops, 0, sizeof(JcmStop) * 12);

    int32_t pool_len = 0;
    auto pool_intern = [&](const char* s) {
        const int32_t len = static_cast<int32_t>(std::strlen(s));
        std::memcpy(pool + pool_len, s, static_cast<size_t>(len));
        /* Offsets are relative to the SNAPSHOT BLOB base (ABI rule). */
        const int32_t off = static_cast<int32_t>(
            (pool + pool_len) - reinterpret_cast<char*>(&snap));
        pool_len += len;
        return off;
    };

    static const char* kStopNames[12] = {
        "CENTRAL", "ADMIRALTY", "WAN CHAI", "CAUSEWAY BAY", "TIU KENG LENG",
        "YAU TONG", "LAM TIN", "KWUN TONG", "NGAU TAU KOK", "KOWLOON BAY",
        "CHOI HUNG", "PO LAM"
    };

    snap.vehicle_id = 0x114514;
    snap.siding_id = 7;
    snap.this_route_id = 42;
    snap.departure_index = 389;
    snap.car_count = 8;
    snap.transport_mode = 0;
    snap.speed_kmh = 80.0;
    snap.speed_ms = 22.2;
    snap.rail_progress = 5120.0;
    snap.door_value = 0.85;
    snap.notch_level = 4;
    snap.door_opening = 1;
    snap.on_route = 1;
    snap.any_car_rendered = 1;
    snap.game_time_millis = 1730000000000LL;
    snap.in_game_time = 6000;
    snap.next_stop_index = 5;

    for (int i = 0; i < 8; i++) {
        cars[i].length = 13.0f;
        cars[i].width = 2.5f;
        cars[i].rendered = 1;
        cars[i].left_door_open = (i % 2 == 0) ? 1 : 0;
        cars[i].right_door_open = (i % 2 == 1) ? 1 : 0;
    }

    for (int i = 0; i < 12; i++) {
        stops[i].route_id = 42;
        stops[i].station_id = 100 + i;
        stops[i].platform_id = 200 + i;
        stops[i].distance = 400.0 * (i + 1);
        stops[i].dwell_time_millis = 30000;
        const int32_t name_off = pool_intern(kStopNames[i]);
        stops[i].name_offset = name_off;
        stops[i].name_len = static_cast<int32_t>(std::strlen(kStopNames[i]));
        const int32_t dest_off = pool_intern("PO LAM");
        stops[i].destination_offset = dest_off;
        stops[i].destination_len = 6;
        stops[i].custom_destination_offset = -1;
        stops[i].custom_destination_len = 0;
    }

    snap.car_offset = static_cast<int32_t>(
        reinterpret_cast<const uint8_t*>(cars) - reinterpret_cast<const uint8_t*>(&snap));
    snap.stop_count = 12;
    snap.stop_offset = static_cast<int32_t>(
        reinterpret_cast<const uint8_t*>(stops) - reinterpret_cast<const uint8_t*>(&snap));
    snap.this_route_stop_count = 12;
    snap.this_route_stop_offset = snap.stop_offset;
    snap.next_route_stop_count = 0;
    snap.next_route_stop_offset = snap.stop_offset;
    snap.string_pool_offset = static_cast<int32_t>(pool - reinterpret_cast<char*>(&snap));
    snap.string_pool_len = pool_len;
    (void)pool_cap;
}

/* Hand-rolled instead of atoi: glibc 2.38 rewrote the strto* family to
   __isoc23_*, which stamps GLIBC_2.38 on the binary and makes it unloadable on
   glibc < 2.38 (Debian 12, Ubuntu 22.04). */
static int parse_arg_int(const char* s, int fallback) {
    if (!s) return fallback;
    long v = 0;
    bool any = false;
    for (; *s == ' '; s++) {}
    for (; *s >= '0' && *s <= '9'; s++) { v = v * 10 + (*s - '0'); any = true; }
    return any ? static_cast<int>(v) : fallback;
}

int main(int argc, char** argv) {
    const int frames = argc > 1 ? parse_arg_int(argv[1], 20000) : 20000;

    /* Snapshot blob: snapshot header + cars + stops + string pool. */
    static JcmVehicleSnapshot snap;
    static JcmCar cars[8];
    static JcmStop stops[12];
    static char pool[512];
    build_snapshot(snap, cars, stops, pool, sizeof(pool));

    JcmFrameInput in{};
    in.abi_version = MTR_NATIVE_ABI_VERSION;
    in.resource_kind = MTR_RESOURCE_VEHICLE;
    in.snapshot = &snap;
    in.state = nullptr;       /* benchmark uses module-local state */
    in.state_size = 0;
    in.host = nullptr;        /* no host services in benchmark */

    JcmFrameOutput out{};

    /* ---- lifecycle ---- */
    if (mtrCreate(&in) != 0) {
        std::fprintf(stderr, "mtrCreate failed\n");
        return 1;
    }

    /* Warmup. */
    for (int i = 0; i < 200; i++) {
        snap.game_time_millis += 50;
        mtrRender(&in, &out);
    }

    /* ---- timed loop ---- */
    const auto t0 = std::chrono::steady_clock::now();
    for (int i = 0; i < frames; i++) {
        snap.game_time_millis += 50;          /* 20 fps script rate */
        snap.speed_kmh = 80.0 + (i % 7);      /* vary content a bit */
        mtrRender(&in, &out);
    }
    const auto t1 = std::chrono::steady_clock::now();
    const double ns = std::chrono::duration<double, std::nano>(t1 - t0).count();
    const double us_per_frame = ns / frames / 1000.0;

    std::printf("frames=%d total=%.3f ms\n", frames, ns / 1e6);
    std::printf("per-frame=%.2f us (%.0f ns)\n", us_per_frame, ns / frames);
    std::printf("draw-calls-last-frame=%d\n", out.record_count);

    /* ---- sanity: render one frame and verify records ---- */
    snap.door_value = 1.0;
    mtrRender(&in, &out);
    int uploads = 0;
    const uint8_t* pos = static_cast<const uint8_t*>(out.records);
    for (int r = 0; r < out.record_count; r++) {
        const auto* h = reinterpret_cast<const JcmRecordHeader*>(pos);
        if (h->kind == JCM_DRAW_MODEL) uploads++;
        pos += h->record_size;
    }
    std::printf("model-draw-calls=%d (16 expected: 8 cars x 2 sides)\n", uploads);
    return uploads == 16 ? 0 : 2;
}
