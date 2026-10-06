/**
 * smoke_test.cpp — dlopen smoke tests for the PIDS and eyecandy
 * example modules: drives create -> render -> dispose with synthetic
 * snapshots and verifies draw-call records come back.
 *
 * Build: g++ -O2 -std=c++17 -I../include smoke_test.cpp -ldl -o smoke_test
 */
#include <mtr/script.hpp>
#include <dlfcn.h>
#include <cstdio>
#include <cstring>
#include <cstdlib>

using namespace mtr;

typedef uint32_t (*pfn_u32)(void);
typedef const char* (*pfn_cstr)(void);
typedef size_t (*pfn_size)(void);
typedef int32_t (*pfn_life1)(const JcmFrameInput*);
typedef int32_t (*pfn_life2)(const JcmFrameInput*, JcmFrameOutput*);

struct Module {
    void* lib;
    pfn_u32 abi;
    pfn_cstr type;
    pfn_cstr id;
    pfn_size state_size;
    pfn_life1 create;
    pfn_life2 render;
    pfn_life1 dispose;

    bool open(const char* path) {
        lib = dlopen(path, RTLD_NOW);
        if (!lib) { std::printf("dlopen(%s) failed: %s\n", path, dlerror()); return false; }
        abi = (pfn_u32)dlsym(lib, "mtrNativeAbiVersion");
        type = (pfn_cstr)dlsym(lib, "mtrScriptType");
        id = (pfn_cstr)dlsym(lib, "mtrScriptId");
        state_size = (pfn_size)dlsym(lib, "mtrStateSize");
        create = (pfn_life1)dlsym(lib, "mtrCreate");
        render = (pfn_life2)dlsym(lib, "mtrRender");
        dispose = (pfn_life1)dlsym(lib, "mtrDispose");
        return abi && type && id && state_size && create && render && dispose;
    }
};

int main() {
    /* ---------------- PIDS module ---------------- */
    {
        Module m;
        if (!m.open("./libpids_arrivals.so")) return 1;
        std::printf("pids module: abi=%u type=%s id=%s state=%zu bytes\n",
                    m.abi(), m.type(), m.id(), m.state_size());

        /* Snapshot blob: header + arrivals + custom message refs + string pool */
        static uint8_t blob[4096];
        std::memset(blob, 0, sizeof(blob));
        auto* snap = reinterpret_cast<JcmPidsSnapshot*>(blob);
        snap->block_pos[0] = 10; snap->block_pos[1] = 64; snap->block_pos[2] = -20;
        snap->game_time_millis = 1730000000000LL;
        snap->in_game_time = 18000;
        snap->width = 128;
        snap->height = 64;
        snap->rows = 3;
        snap->arrival_count = 2;

        auto* arrivals = reinterpret_cast<JcmArrival*>(blob + 512);
        snap->arrival_offset = 512;
        char* pool = reinterpret_cast<char*>(blob + 2048);
        snap->string_pool_offset = 2048;

        int32_t pool_len = 0;
        auto intern = [&](const char* s) {
            const int32_t len = static_cast<int32_t>(std::strlen(s));
            std::memcpy(pool + pool_len, s, static_cast<size_t>(len));
            /* Offsets are relative to the SNAPSHOT BLOB base (ABI rule). */
            const int32_t off = static_cast<int32_t>(
                (pool + pool_len) - reinterpret_cast<char*>(snap));
            pool_len += len;
            return off;
        };

        const int32_t dest1 = intern("TSUEN WAN");
        const int32_t dest2 = intern("CENTRAL");
        const int32_t rn1 = intern("R14");
        const int32_t rn2 = intern("");
        const int32_t pn1 = intern("2");
        const int32_t pn2 = intern("1");
        snap->string_pool_len = pool_len;

        arrivals[0].arrival_epoch_millis = snap->game_time_millis + 45 * 1000;
        arrivals[0].departure_epoch_millis = snap->game_time_millis + 90 * 1000;
        arrivals[0].route_id = 14;
        arrivals[0].platform_id = 200;
        arrivals[0].route_color = 0xFFBB2C00;
        arrivals[0].car_count = 8;
        arrivals[0].realtime = 1;
        arrivals[0].circular_state = 0;
        arrivals[0].destination_offset = dest1;
        arrivals[0].destination_len = 9;
        arrivals[0].route_number_offset = rn1;
        arrivals[0].route_number_len = 3;
        arrivals[0].platform_name_offset = pn1;
        arrivals[0].platform_name_len = 1;

        arrivals[1].arrival_epoch_millis = snap->game_time_millis + 220 * 1000;
        arrivals[1].departure_epoch_millis = snap->game_time_millis + 300 * 1000;
        arrivals[1].route_id = 3;
        arrivals[1].platform_id = 201;
        arrivals[1].route_color = 0xFF00733B;
        arrivals[1].car_count = 7;          /* mixed car length! */
        arrivals[1].realtime = 0;
        arrivals[1].circular_state = 1;     /* clockwise */
        arrivals[1].destination_offset = dest2;
        arrivals[1].destination_len = 7;
        arrivals[1].route_number_offset = rn2;
        arrivals[1].route_number_len = 0;
        arrivals[1].platform_name_offset = pn2;
        arrivals[1].platform_name_len = 1;

        /* custom message on row 2 */
        auto* refs = reinterpret_cast<int32_t*>(blob + 1024);
        snap->custom_message_count = 3;
        snap->custom_message_offset = 1024;
        const int32_t msg = intern("MIND THE GAP|請小心月台空隙");
        refs[0] = -1; refs[1] = 0;              /* null, empty */
        refs[2] = msg; refs[3] = 25;
        snap->row_hidden_bits = 0;

        JcmFrameInput in{};
        in.abi_version = MTR_NATIVE_ABI_VERSION;
        in.resource_kind = MTR_RESOURCE_PIDS;
        in.snapshot = snap;
        in.host = nullptr;
        uint8_t state[256] = {0};
        in.state = state;
        in.state_size = m.state_size() <= sizeof(state) ? m.state_size() : 0;

        JcmFrameOutput out{};
        if (m.create(&in) != 0) { std::printf("pids create FAILED\n"); return 1; }
        if (m.render(&in, &out) != 0) { std::printf("pids render FAILED\n"); return 1; }
        m.dispose(&in);

        std::printf("pids frame: %d records, strings=%lld bytes\n",
                    out.record_count, (long long)out.string_arena_len);
        if (out.record_count < 5) { std::printf("too few records!\n"); return 1; }

        /* Verify text content made it into the string arena. */
        const char* arena = out.string_arena;
        bool found_arrival = false, found_msg = false, found_clock = false;
        std::string all(arena, static_cast<size_t>(out.string_arena_len));
        if (all.find("TSUEN WAN") != std::string::npos) found_arrival = true;
        if (all.find("MIND THE GAP") != std::string::npos) found_msg = true;
        if (all.find(":") != std::string::npos) found_clock = true;
        std::printf("arena checks: arrival=%d custom=%d clock=%d\n",
                    found_arrival, found_msg, found_clock);
        if (!found_arrival || !found_msg) { std::printf("PIDS SMOKE FAILED\n"); return 1; }
        std::printf("PIDS SMOKE OK\n\n");
    }

    /* ---------------- eyecandy module ---------------- */
    {
        Module m;
        if (!m.open("./libeyecandy_signal.so")) return 1;
        std::printf("eyecandy module: abi=%u type=%s id=%s state=%zu bytes\n",
                    m.abi(), m.type(), m.id(), m.state_size());

        static JcmEyecandySnapshot snap;
        std::memset(&snap, 0, sizeof(snap));
        snap.block_pos[0] = 5; snap.block_pos[1] = 70; snap.block_pos[2] = 8;
        snap.game_time_millis = 1730000000000LL;
        snap.in_game_time = 9000;
        snap.translate[0] = 0.5F; snap.translate[1] = 0.0F; snap.translate[2] = 0.5F;
        snap.rotate[1] = 90.0F;
        snap.full_brightness = 1;
        snap.facing = 2;
        snap.redstone_level = 11;
        snap.block_use_events = 1;

        static char model_id[32] = "demo:redstone_signal";
        snap.model_id_offset = static_cast<int32_t>(
            reinterpret_cast<const char*>(model_id) - reinterpret_cast<const char*>(&snap));
        snap.model_id_len = 20;

        JcmFrameInput in{};
        in.abi_version = MTR_NATIVE_ABI_VERSION;
        in.resource_kind = MTR_RESOURCE_EYECANDY;
        in.snapshot = &snap;
        in.host = nullptr;
        uint8_t state[256] = {0};
        in.state = state;
        in.state_size = m.state_size() <= sizeof(state) ? m.state_size() : 0;

        JcmFrameOutput out{};
        if (m.create(&in) != 0) { std::printf("eyecandy create FAILED\n"); return 1; }
        if (m.render(&in, &out) != 0) { std::printf("eyecandy render FAILED\n"); return 1; }
        m.dispose(&in);

        std::printf("eyecandy frame: %d records (upload + model + 2 sounds + shape)\n",
                    out.record_count);
        int uploads = 0, models = 0, sounds = 0, shapes = 0;
        const uint8_t* pos = static_cast<const uint8_t*>(out.records);
        for (int r = 0; r < out.record_count; r++) {
            const auto* h = reinterpret_cast<const JcmRecordHeader*>(pos);
            switch (h->kind) {
                case JCM_DRAW_TEXTURE_UPLOAD: uploads++; break;
                case JCM_DRAW_MODEL: models++; break;
                case JCM_DRAW_SOUND: sounds++; break;
                case JCM_DRAW_OUTLINE_SHAPE: shapes++; break;
                default: break;
            }
            pos += h->record_size;
        }
        std::printf("kinds: uploads=%d models=%d sounds=%d shapes=%d\n",
                    uploads, models, sounds, shapes);
        if (uploads != 1 || models != 1 || sounds != 2 || shapes != 1) {
            std::printf("EYECANDY SMOKE FAILED\n"); return 1;
        }
        std::printf("EYECANDY SMOKE OK\n");
    }

    return 0;
}
