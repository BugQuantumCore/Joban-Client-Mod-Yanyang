/**
 * wr2a03_bench.cpp — performance harness for the WR2-A03 native scripts.
 *
 * Answers the two questions that matter before shipping a resource pack:
 *   1. How long does one frame of mtrRender take (and how much of that is
 *      the script's own rasteriser vs. the pixel upload)?
 *   2. How much memory does one vehicle's scripts actually hold?
 *
 * It drives the real libraries through the real ABI (same snapshot builder as
 * wr2a03_smoke) and reports, per module:
 *
 *   first/max/avg frame time   — real work (repaint) frames
 *   steady-state frame time    — frames where the content signature did not
 *                                change, i.e. the repaint-on-change skip
 *   upload volume per frame    — bytes pushed through the frame pixel arena
 *   host-side texture memory   — w*h*4 per GraphicsTexture the script created
 *
 * Usage: wr2a03_bench <dir-with-dlls> [frames] [cars]
 */
#include <mtr/mtr_native.h>

#include <algorithm>
#include <chrono>
#include <cmath>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <map>
#include <string>
#include <vector>

#if defined(_WIN32)
#  include <windows.h>
#  include <psapi.h>
#else
#  include <dlfcn.h>
#  include <unistd.h>
#endif

using clock_type = std::chrono::steady_clock;

/* ------------------------------------------------------------------ */
/* host stubs (same shape as wr2a03_smoke, counting uploads)           */
/* ------------------------------------------------------------------ */

namespace {

int32_t g_next_texture = 1;
int32_t g_next_model = 100;
long    g_texture_count = 0;
long    g_quad_count = 0;
long    g_upload_bytes = 0;
long    g_texture_bytes = 0;

int32_t hs_acquire_model(void*, const char*) { return g_next_model++; }
void    hs_release_model(void*, int32_t) {}

int32_t hs_create_texture(void*, int32_t w, int32_t h) {
    g_texture_count++;
    g_texture_bytes += static_cast<long>(w) * h * 4;   /* RGBA8, as Java allocates */
    return g_next_texture++;
}
void hs_release_texture(void*, int32_t) {}

int32_t hs_acquire_quad_model(void*, const float* verts, const float* uv,
                              int32_t vertex_count, int32_t render_stage,
                              int32_t texture_handle) {
    if (!verts || !uv || vertex_count != 4 || texture_handle < 0) return -1;
    (void)render_stage;
    g_quad_count++;
    return g_next_model++;
}

int32_t hs_rasterize_text(void*, const char*, int32_t, int32_t x, int32_t y,
                          int32_t max_w, uint8_t r, uint8_t g, uint8_t b,
                          uint8_t* out, int32_t out_w, int32_t out_h) {
    if (max_w <= 0 || !out) return -1;
    const int32_t gw = max_w - 1, gh = static_cast<int32_t>(max_w * 0.92);
    for (int32_t py = y; py < y + gh; py++) {
        if (py < 0 || py >= out_h) continue;
        for (int32_t px = x; px < x + gw; px++) {
            if (px < 0 || px >= out_w) continue;
            const size_t i = (static_cast<size_t>(py) * out_w + px) * 4;
            out[i + 0] = b; out[i + 1] = g; out[i + 2] = r; out[i + 3] = 255;
        }
    }
    return 1;
}

void hs_log(void*, int32_t, const char*, int32_t) {}

JcmHostServices make_host() {
    JcmHostServices hs{};
    hs.acquire_model = hs_acquire_model;
    hs.release_model = hs_release_model;
    hs.create_texture = hs_create_texture;
    hs.release_texture = hs_release_texture;
    hs.rasterize_text = hs_rasterize_text;
    hs.log = hs_log;
    hs.acquire_quad_model = hs_acquire_quad_model;
    return hs;
}

/* ------------------------------------------------------------------ */
/* snapshot builder (mirrors wr2a03_smoke)                             */
/* ------------------------------------------------------------------ */

struct StopSpec {
    const char* name;
    double distance;
    int64_t station_id;
    const char* dest;
    uint8_t routeCircularState = 0;
};

struct Snapshot { std::vector<uint8_t> buf; };

Snapshot build_snapshot(int carCount, const std::vector<StopSpec>& stops,
                        int64_t gameTimeMillis, double doorValue, double speedMs,
                        int32_t nextStopIndex, const char* routeName,
                        uint32_t routeColor, uint8_t circularState,
                        const char* sidingName) {
    Snapshot out;
    std::vector<uint8_t>& buf = out.buf;
    auto pad_to = [&](size_t a) { while (buf.size() % a) buf.push_back(0); };

    buf.resize(sizeof(JcmVehicleSnapshot));
    pad_to(alignof(JcmCar));
    const int32_t carOffset = static_cast<int32_t>(buf.size());
    for (int i = 0; i < carCount; i++) {
        JcmCar c{};
        c.length = 25.0F; c.width = 3.0F; c.rendered = 1;
        c.left_door_open = doorValue > 0.5 ? 1 : 0;
        c.right_door_open = doorValue > 0.5 ? 1 : 0;
        buf.insert(buf.end(), reinterpret_cast<const uint8_t*>(&c),
                   reinterpret_cast<const uint8_t*>(&c) + sizeof(c));
    }
    pad_to(alignof(JcmStop));
    const int32_t stopOffset = static_cast<int32_t>(buf.size());
    std::vector<std::pair<int32_t, int32_t>> names, dests;
    /* reserve the stop array, then append the pool and patch */
    buf.resize(buf.size() + stops.size() * sizeof(JcmStop));

    int32_t routeNameOff = 0, sidingOff = 0, poolStart = static_cast<int32_t>(buf.size());
    poolStart = static_cast<int32_t>(buf.size());
    for (size_t i = 0; i < stops.size(); i++) {
        const int32_t off = static_cast<int32_t>(buf.size());
        buf.insert(buf.end(), stops[i].name, stops[i].name + std::strlen(stops[i].name));
        names.emplace_back(off, static_cast<int32_t>(std::strlen(stops[i].name)));
        const char* d = stops[i].dest ? stops[i].dest : "";
        const int32_t doff = static_cast<int32_t>(buf.size());
        buf.insert(buf.end(), d, d + std::strlen(d));
        dests.emplace_back(doff, static_cast<int32_t>(std::strlen(d)));
    }
    routeNameOff = static_cast<int32_t>(buf.size());
    buf.insert(buf.end(), routeName, routeName + std::strlen(routeName));
    sidingOff = static_cast<int32_t>(buf.size());
    buf.insert(buf.end(), sidingName, sidingName + std::strlen(sidingName));

    for (size_t i = 0; i < stops.size(); i++) {
        JcmStop s{};
        s.route_id = 42;
        s.station_id = stops[i].station_id;
        s.platform_id = stops[i].station_id * 10;
        s.distance = stops[i].distance;
        s.dwell_time_millis = 15000;
        s.name_offset = names[i].first;
        s.name_len = names[i].second;
        s.destination_offset = dests[i].first;
        s.destination_len = dests[i].second;
        s.custom_destination_offset = -1;
        s.route_circular_state = stops[i].routeCircularState;
        std::memcpy(buf.data() + stopOffset + i * sizeof(JcmStop), &s, sizeof(s));
    }

    JcmVehicleSnapshot* hdr = reinterpret_cast<JcmVehicleSnapshot*>(buf.data());
    hdr->vehicle_id = 123456789;
    hdr->this_route_id = 42;
    hdr->departure_index = 38;
    hdr->car_count = carCount;
    hdr->speed_kmh = speedMs * 3.6;
    hdr->speed_ms = speedMs;
    hdr->rail_progress = 3400.0;
    hdr->door_value = doorValue;
    hdr->notch_level = 3;
    hdr->on_route = 1;
    hdr->any_car_rendered = 1;
    hdr->total_dwell_time_millis = 15000;
    hdr->game_time_millis = gameTimeMillis;
    hdr->car_offset = carOffset;
    hdr->stop_count = static_cast<int32_t>(stops.size());
    hdr->stop_offset = stopOffset;
    hdr->this_route_stop_count = static_cast<int32_t>(stops.size());
    hdr->this_route_stop_offset = stopOffset;
    hdr->next_stop_index = nextStopIndex;
    hdr->route_name_offset = routeNameOff;
    hdr->route_name_len = static_cast<int32_t>(std::strlen(routeName));
    hdr->route_color = static_cast<int32_t>(routeColor);
    hdr->circular_state = circularState;
    hdr->siding_name_offset = sidingOff;
    hdr->siding_name_len = static_cast<int32_t>(std::strlen(sidingName));
    hdr->string_pool_offset = poolStart;
    hdr->string_pool_len = static_cast<int32_t>(buf.size()) - poolStart;
    return out;
}

/* ------------------------------------------------------------------ */
/* module loader + frame accounting                                    */
/* ------------------------------------------------------------------ */

void* lib_open(const std::string& p) {
#if defined(_WIN32)
    return static_cast<void*>(LoadLibraryA(p.c_str()));
#else
    return dlopen(p.c_str(), RTLD_NOW | RTLD_LOCAL);
#endif
}
void* lib_sym(void* l, const char* n) {
#if defined(_WIN32)
    return reinterpret_cast<void*>(GetProcAddress(static_cast<HMODULE>(l), n));
#else
    return dlsym(l, n);
#endif
}
void lib_close(void* l) {
    if (!l) return;
#if defined(_WIN32)
    FreeLibrary(static_cast<HMODULE>(l));
#else
    dlclose(l);
#endif
}

struct Module {
    void* handle = nullptr;
    const char* (*id)(void) = nullptr;
    size_t (*state_size)(void) = nullptr;
    /* ABI 6, optional. The host must call this before the first create() —
       see the note on mtrStateSize in mtr_native.h. */
    void (*init)(const JcmFrameInput*) = nullptr;
    int32_t (*create)(const JcmFrameInput*) = nullptr;
    int32_t (*render)(const JcmFrameInput*, JcmFrameOutput*) = nullptr;
    int32_t (*dispose)(const JcmFrameInput*) = nullptr;

    bool open(const std::string& p) {
        handle = lib_open(p);
        if (!handle) return false;
        id = reinterpret_cast<const char* (*)(void)>(lib_sym(handle, "mtrScriptId"));
        state_size = reinterpret_cast<size_t (*)(void)>(lib_sym(handle, "mtrStateSize"));
        init = reinterpret_cast<void (*)(const JcmFrameInput*)>(lib_sym(handle, "mtrInit"));
        create = reinterpret_cast<int32_t (*)(const JcmFrameInput*)>(lib_sym(handle, "mtrCreate"));
        render = reinterpret_cast<int32_t (*)(const JcmFrameInput*, JcmFrameOutput*)>(
            lib_sym(handle, "mtrRender"));
        dispose = reinterpret_cast<int32_t (*)(const JcmFrameInput*)>(
            lib_sym(handle, "mtrDispose"));
        return id && state_size && create && render && dispose;
    }
    ~Module() { lib_close(handle); }
};

struct FrameCount {
    long models = 0;
    long uploads = 0;
    long uploadBytes = 0;
    long maxUploadRect = 0;
};

void account(const JcmFrameOutput& out, FrameCount& fc, bool countBytes) {
    const uint8_t* p = static_cast<const uint8_t*>(out.records);
    for (int32_t i = 0; i < out.record_count; i++) {
        const JcmRecordHeader* h = reinterpret_cast<const JcmRecordHeader*>(p);
        if (h->kind == JCM_DRAW_MODEL) {
            fc.models++;
        } else if (h->kind == JCM_DRAW_TEXTURE_UPLOAD) {
            const JcmDrawTextureUpload* u =
                reinterpret_cast<const JcmDrawTextureUpload*>(p);
            fc.uploads++;
            if (countBytes) fc.uploadBytes += u->pixel_data_len;
            const long rect = static_cast<long>(u->dirty_w) * u->dirty_h * 4;
            if (rect > fc.maxUploadRect) fc.maxUploadRect = rect;
        }
        p += h->record_size;
    }
}

long peak_rss_kb() {
#if defined(_WIN32)
    PROCESS_MEMORY_COUNTERS pmc{};
    if (GetProcessMemoryInfo(GetCurrentProcess(), &pmc, sizeof(pmc))) {
        return static_cast<long>(pmc.PeakWorkingSetSize / 1024);
    }
    return -1;
#elif defined(__linux__)
    /* VmHWM in /proc/self/status is in kB already */
    std::FILE* f = std::fopen("/proc/self/status", "r");
    if (!f) return -1;
    char line[256];
    long peak = -1;
    while (std::fgets(line, sizeof(line), f)) {
        long kb = 0;
        if (std::sscanf(line, "VmHWM: %ld kB", &kb) == 1) { peak = kb; break; }
    }
    std::fclose(f);
    return peak;
#else
    return -1;
#endif
}

long current_rss_kb() {
#if defined(_WIN32)
    PROCESS_MEMORY_COUNTERS pmc{};
    if (GetProcessMemoryInfo(GetCurrentProcess(), &pmc, sizeof(pmc))) {
        return static_cast<long>(pmc.WorkingSetSize / 1024);
    }
    return -1;
#elif defined(__linux__)
    /* /proc/self/statm field 2 is resident pages */
    std::FILE* f = std::fopen("/proc/self/statm", "r");
    if (!f) return -1;
    long total = 0, resident = 0;
    const int n = std::fscanf(f, "%ld %ld", &total, &resident);
    std::fclose(f);
    if (n != 2) return -1;
    return resident * (sysconf(_SC_PAGESIZE) / 1024);
#else
    return -1;
#endif
}

/* ------------------------------------------------------------------ */

std::vector<StopSpec> linear_stops() {
    return {
        {"火车站|Railway Station", 0, 101, ""},
        {"人民广场|People Square", 900, 102, ""},
        {"青年路|Qingnian Road", 1800, 103, ""},
        {"大南门|Dananmen", 2700, 104, ""},
        {"体育馆|Stadium", 3600, 105, ""},
        {"长风街|Changfeng Street", 4500, 106, ""},
        {"学府街|Xuefu Street", 5400, 107, ""},
        {"南中环|Nanzhonghuan", 6300, 108, ""},
        {"晋阳街|Jinyang Street", 7200, 109, ""},
        {"西桥|Xiqiao", 8100, 110, "西桥|Xiqiao"},
    };
}

std::vector<StopSpec> circular_stops() {
    static const char* names[] = {
        "西北延|Xibei Yan", "涧河路|Jianhe Road", "动物园|Zoo",
        "胜利桥东|Shengliqiaodong", "府西街|Fuxi Street", "迎泽公园|Yingze Park",
        "南内环|Nanneihuan", "长风街|Changfeng Street", "体育中心|Sports Center",
        "晋阳湖|Jinyang Lake", "兴华街|Xinghua Street", "玉门河|Yumenhe",
    };
    std::vector<StopSpec> s;
    for (int i = 0; i < 12; i++) {
        StopSpec st{};
        st.name = names[i];
        st.distance = i * 800.0;
        st.station_id = 200 + i;
        st.dest = "";
        st.routeCircularState = 1;
        s.push_back(st);
    }
    s.push_back({"西北延|Xibei Yan", 12 * 800.0, 200, "", 1});
    return s;
}

struct Result {
    double firstMs = 0, maxMs = 0;
    double repaintAvgMs = 0, skipAvgMs = 0;      /* split by "did it re-rasterise" */
    long repaintFrames = 0, skipFrames = 0, drawOnlyFrames = 0;
    long totalUploads = 0, totalUploadBytes = 0, maxUploadRect = 0;
    long modelsPerFrame = 0;
    long peakFrameBytes = 0;
};

Result bench(Module& m, const Snapshot& snap, const JcmHostServices& host,
             int frames, const char* label) {
    Result r;
    void* state = std::calloc(1, m.state_size());
    JcmFrameInput in{};
    in.abi_version = MTR_NATIVE_ABI_VERSION;
    in.resource_kind = MTR_RESOURCE_VEHICLE;
    in.state = state;
    in.state_size = m.state_size();
    in.host = &host;

    /* Same protocol the JNI bridge follows: construct the state, then create,
       then render. Skipping create() would leave the State unconstructed for a
       non-trivial type (see mtr_native.h) — the module constructs it lazily in
       that case, but the host is supposed to drive the real order. */
    if (m.init) m.init(&in);
    in.snapshot = snap.buf.data();
    if (m.create(&in) != 0) {
        std::printf("  create failed\n");
        m.dispose(&in);
        std::free(state);
        return r;
    }

    JcmFrameOutput out{};
    double repaintSum = 0, skipSum = 0;
    long repaintCount = 0, skipCount = 0;

    for (int f = 0; f < frames; f++) {
        Snapshot local = snap;
        JcmVehicleSnapshot* hdr = reinterpret_cast<JcmVehicleSnapshot*>(local.buf.data());
        hdr->game_time_millis += static_cast<int64_t>(f) * 50;   /* advance the clock */
        in.snapshot = local.buf.data();

        FrameCount fc{};
        const auto t0 = clock_type::now();
        if (m.render(&in, &out) != 0) { std::printf("  render failed at frame %d\n", f); break; }
        const double ms = std::chrono::duration<double, std::milli>(clock_type::now() - t0).count();
        account(out, fc, false);

        if (f == 0) r.firstMs = ms;
        if (ms > r.maxMs) r.maxMs = ms;
        r.totalUploads += fc.uploads;
        r.totalUploadBytes += fc.uploadBytes;
        if (fc.maxUploadRect > r.maxUploadRect) r.maxUploadRect = fc.maxUploadRect;
        r.modelsPerFrame = fc.models;

        long frameBytes = 0;
        {
            const uint8_t* p = static_cast<const uint8_t*>(out.records);
            for (int32_t i = 0; i < out.record_count; i++) {
                const JcmRecordHeader* h = reinterpret_cast<const JcmRecordHeader*>(p);
                if (h->kind == JCM_DRAW_TEXTURE_UPLOAD) {
                    frameBytes += reinterpret_cast<const JcmDrawTextureUpload*>(p)->pixel_data_len;
                }
                p += h->record_size;
            }
        }
        if (frameBytes > r.peakFrameBytes) r.peakFrameBytes = frameBytes;

        if (f > 1) {   /* skip the two warm-up frames */
            if (fc.uploads > 0) { repaintSum += ms; repaintCount++; }
            else if (fc.models > 0) { skipSum += ms; skipCount++; }
        }
    }
    r.repaintFrames = repaintCount;
    r.skipFrames = skipCount;
    r.repaintAvgMs = repaintCount ? repaintSum / repaintCount : 0;
    r.skipAvgMs = skipCount ? skipSum / skipCount : 0;
    m.dispose(&in);
    std::free(state);

    std::printf("  %s\n", label);
    std::printf("    first frame           %8.2f ms   (textures created + first paint)\n", r.firstMs);
    std::printf("    repaint frame (avg)   %8.3f ms   over %ld frames\n", r.repaintAvgMs, r.repaintFrames);
    std::printf("    steady frame (avg)    %8.3f ms   over %ld frames (upload skipped)\n", r.skipAvgMs, r.skipFrames);
    std::printf("    worst frame           %8.2f ms\n", r.maxMs);
    std::printf("    uploads %ld total (%.1f/frame), %ld model draws/frame\n",
                r.totalUploads, frames ? static_cast<double>(r.totalUploads) / frames : 0.0,
                r.modelsPerFrame);
    std::printf("    dirty rect max %.0f KB ; pixel bytes in the heaviest frame %.1f MB\n",
                r.maxUploadRect / 1024.0, r.peakFrameBytes / 1048576.0);
    return r;
}

/* Hand-rolled instead of atoi: glibc 2.38 rewrote the strto* family to
   __isoc23_*, which stamps GLIBC_2.38 on the binary and makes it unloadable on
   glibc < 2.38 (Debian 12, Ubuntu 22.04). */
int parse_arg_int(const char* s, int fallback) {
    if (!s) return fallback;
    long v = 0;
    bool any = false;
    for (; *s == ' '; s++) {}
    for (; *s >= '0' && *s <= '9'; s++) { v = v * 10 + (*s - '0'); any = true; }
    return any ? static_cast<int>(v) : fallback;
}

int run(int argc, char** argv) {
    const std::string dir = argc > 1 ? argv[1] : ".";
    const int frames = argc > 2 ? parse_arg_int(argv[2], 400) : 400;
#if defined(_WIN32)
    const std::string lcd = dir + "\\wr2a03_lcd.dll";
    const std::string num = dir + "\\wr2a03_train_num.dll";
#elif defined(__APPLE__)
    const std::string lcd = dir + "/libwr2a03_lcd.dylib";
    const std::string num = dir + "/libwr2a03_train_num.dylib";
#else
    const std::string lcd = dir + "/libwr2a03_lcd.so";
    const std::string num = dir + "/libwr2a03_train_num.so";
#endif

    std::printf("== wr2a03 native performance ==\n");
    std::printf("frames per scenario: %d\n\n", frames);

    JcmHostServices host = make_host();
    const long rssBefore = current_rss_kb();

    /* ---------------- LCD ---------------- */
    {
        Module m;
        if (!m.open(lcd)) { std::printf("cannot load %s\n", lcd.c_str()); return 1; }
        std::printf("module %s (state %zu B)\n", m.id(), m.state_size());
        g_texture_count = g_quad_count = g_texture_bytes = 0;

        auto stops = linear_stops();
        Snapshot snap = build_snapshot(6, stops, 1000000, 0.0, 17.0, 4,
                                       "10号线|Line 10", 0xFF009BC0, 0, "10010/01-02-03-04-05-06");
        Result r = bench(m, snap, host, frames, "LCD linear 6-car");

        std::printf("  textures created %ld (%.1f MB host-side RGBA8) | quads %ld\n",
                    g_texture_count, g_texture_bytes / 1048576.0, g_quad_count);
        std::printf("  upload volume %ld bytes over %d frames (%.1f KB/frame avg)\n",
                    g_upload_bytes, frames, g_upload_bytes / 1024.0 / frames);

        /* circular */
        auto loop = circular_stops();
        Snapshot circ = build_snapshot(6, loop, 2000000, 0.0, 12.0, 5,
                                       "环线|Loop Line", 0xFF009BC0, 1, "10010/01-02-03-04-05-06");
        Result r2 = bench(m, circ, host, frames, "LCD circular 6-car");
        (void)r2;
    }
    const long rssAfterLcd = current_rss_kb();

    /* ---------------- train numbers ---------------- */
    {
        Module m;
        if (!m.open(num)) { std::printf("\ncannot load %s\n", num.c_str()); return 1; }
        std::printf("\nmodule %s (state %zu B)\n", m.id(), m.state_size());
        g_texture_count = g_quad_count = g_texture_bytes = 0;

        auto stops = linear_stops();
        Snapshot snap = build_snapshot(6, stops, 1000000, 0.0, 17.0, 4,
                                       "10号线|Line 10", 0xFF009BC0, 0, "10010/01-02-03-04-05-06");
        Result r = bench(m, snap, host, frames, "car numbers 6-car");

        std::printf("  textures created %ld (%.1f MB host-side RGBA8) | quads %ld\n",
                    g_texture_count, g_texture_bytes / 1048576.0, g_quad_count);
        std::printf("  upload volume %ld bytes over %d frames (%.1f KB/frame avg)\n",
                    g_upload_bytes, frames, g_upload_bytes / 1024.0 / frames);
        (void)r;
    }

    std::printf("\nRSS: before %.1f MB -> after LCD %.1f MB -> end %.1f MB (peak %.1f MB)\n",
                rssBefore / 1024.0, rssAfterLcd / 1024.0,
                current_rss_kb() / 1024.0, peak_rss_kb() / 1024.0);
    return 0;
}

} /* anonymous namespace */

int main(int argc, char** argv) {
    return run(argc, argv);
}
