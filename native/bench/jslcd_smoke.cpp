/**
 * jslcd_smoke.cpp — end-to-end driver for the jslcd ports.
 *
 * Builds synthetic JcmVehicleSnapshot blobs (8-car train, siding name
 * "10010/01-02-...-08", 10-stop linear route + 12-stop 环线), drives
 * libjslcd_vehicle.so / libjslcd_train_num.so through the real ABI
 * (dlopen + mtrCreate/mtrRender/mtrDispose with host-provided state
 * blocks), reconstructs the uploaded textures from the frame records
 * and writes PPMs, then measures:
 *
 *   steady   — frames where nothing changed (repaint-on-change skip)
 *   repainted — frames forced to change (blink toggle, the JS redraws
 *              every frame like this)
 *
 * Build: cmake --build . --target jslcd_smoke && ./jslcd_smoke
 */
#include <mtr/mtr_native.h>

#include <chrono>
#include <cstdio>
#include <cstdint>
#include <cstring>
#include <cstdlib>
#include <string>
#include <vector>
#include <map>
#include <dlfcn.h>

using namespace std::chrono;

/* builder scratch (single-threaded driver) */
static int32_t g_dummy_route_offset = 0;
static int32_t g_dummy_siding_offset = 0;
static int32_t g_dummy_pool_start = 0;
static int32_t g_dummy_pool_end = 0;
static std::vector<std::pair<int32_t, int32_t>> g_dummy_stop_names;
static std::vector<std::pair<int32_t, int32_t>> g_dummy_stop_dests;

/* ------------------------------------------------------------------ */
/* host service stubs                                                  */
/* ------------------------------------------------------------------ */

static int32_t g_model_handle = 100;
static int32_t g_texture_handle = 1;

static int32_t hs_acquire_model(void*, const char* path) {
    std::printf("  [host] acquire_model(%s) -> %d\n", path, g_model_handle);
    return g_model_handle++;
}
static void hs_release_model(void*, int32_t) {}
static int32_t hs_create_texture(void*, int32_t w, int32_t h) {
    std::printf("  [host] create_texture(%d x %d) -> %d\n", w, h, g_texture_handle);
    return g_texture_handle++;
}
static void hs_release_texture(void*, int32_t) {}

/* Em-box fill stands in for the host TTF rasterizer (CJK glyphs). */
static int32_t hs_rasterize_text(void*, const char*, int32_t, int32_t x, int32_t y,
                                 int32_t max_w, uint8_t r, uint8_t g, uint8_t b,
                                 uint8_t* out, int32_t out_w, int32_t out_h) {
    if (max_w <= 0) return -1;
    const int32_t gw = max_w - 1, gh = static_cast<int32_t>(max_w * 0.92);
    for (int32_t py = y; py < y + gh; py++) {
        if (py < 0 || py >= out_h) continue;
        for (int32_t px = x; px < x + gw; px++) {
            if (px < 0 || px >= out_w) continue;
            const size_t idx = (static_cast<size_t>(py) * out_w + px) * 4;
            /* little-endian ARGB buffer: byte0=B, byte1=G, byte2=R.
               Direct opaque write so smoke checks can assert exact
               colors (the real host blends TTF anti-aliasing here). */
            out[idx + 0] = b;
            out[idx + 1] = g;
            out[idx + 2] = r;
            out[idx + 3] = 255;
        }
    }
    return 1;
}

static void hs_log(void*, int32_t level, const char* utf8, int32_t len) {
    std::printf("  [host log %d] %.*s\n", level, len, utf8);
}

static JcmHostServices make_host() {
    JcmHostServices hs{};
    hs.user = nullptr;
    hs.acquire_model = hs_acquire_model;
    hs.release_model = hs_release_model;
    hs.create_texture = hs_create_texture;
    hs.release_texture = hs_release_texture;
    hs.rasterize_text = hs_rasterize_text;
    hs.log = hs_log;
    return hs;
}

/* ------------------------------------------------------------------ */
/* snapshot builder                                                    */
/* ------------------------------------------------------------------ */

struct StopSpec {
    const char* name;         /* "太原站|Taiyuan Railway Station" */
    double distance;
    int64_t station_id;
    const char* dest;         /* destination name (last stop) */
    std::vector<std::pair<const char*, uint32_t>> transfers;
    /* v3: station exits (JS: station.getExits()) */
    std::vector<std::pair<const char*, std::vector<const char*>>> exits;
};

struct ExitPoolEntry {
    std::string name;
    std::vector<std::string> destinations;
};

struct CarSpec { float length = 20.0F; };

static std::vector<uint8_t> build_snapshot(
        int car_count, const CarSpec* cars, const std::vector<StopSpec>& stops,
        int64_t game_time_millis, double door_value, double speed_ms,
        int32_t next_stop_index, const char* route_name, uint32_t route_color,
        uint8_t circular_state, const char* siding_name, bool reversed,
        bool on_route) {
    std::vector<uint8_t> buf;
    auto pad_to = [&](size_t align) {
        while (buf.size() % align != 0) buf.push_back(0);
    };

    /* header placeholder */
    buf.resize(sizeof(JcmVehicleSnapshot));
    pad_to(alignof(JcmCar));

    /* cars */
    const int32_t car_offset = static_cast<int32_t>(buf.size());
    for (int i = 0; i < car_count; i++) {
        JcmCar c{};
        c.length = cars[i < 8 ? i : 0].length;
        c.width = 2.5F;
        c.left_door_open = door_value > 0.5 ? 1 : 0;
        c.right_door_open = door_value > 0.5 ? 1 : 0;
        c.rendered = 1;
        c.vehicle_type_offset = 0;
        c.vehicle_type_len = 0;
        buf.insert(buf.end(), reinterpret_cast<const uint8_t*>(&c),
                   reinterpret_cast<const uint8_t*>(&c) + sizeof(c));
    }
    pad_to(alignof(JcmStop));

    /* interchanges first (needed for offsets) */
    const int32_t ic_offset = [&] {
        pad_to(alignof(JcmInterchange));
        const int32_t off = static_cast<int32_t>(buf.size());
        for (const StopSpec& st : stops) {
            for (const auto& tr : st.transfers) {
                JcmInterchange ic{};
                ic.color = static_cast<int32_t>(tr.second);
                ic.route_name_offset = 0;   /* fixed below */
                ic.route_name_len = static_cast<int32_t>(std::strlen(tr.first));
                buf.insert(buf.end(), reinterpret_cast<const uint8_t*>(&ic),
                           reinterpret_cast<const uint8_t*>(&ic) + sizeof(ic));
            }
        }
        return off;
    }();

    /* v3: exits pool — JcmExit[] placeholders first (name/destination
       offsets patched after the string pool is laid out) */
    int32_t exit_pool_off = 0;
    std::vector<int32_t> per_stop_exit_pool(stops.size(), 0);
    {
        bool any = false;
        for (const StopSpec& st : stops) if (!st.exits.empty()) { any = true; break; }
        if (any) {
            pad_to(alignof(JcmExit));
            exit_pool_off = static_cast<int32_t>(buf.size());
            for (size_t i = 0; i < stops.size(); i++) {
                if (stops[i].exits.empty()) continue;
                per_stop_exit_pool[i] = static_cast<int32_t>(buf.size());
                for (size_t e = 0; e < stops[i].exits.size(); e++) {
                    JcmExit ex{};
                    ex.name_len = static_cast<int32_t>(std::strlen(stops[i].exits[e].first));
                    ex.destination_count = static_cast<int32_t>(stops[i].exits[e].second.size());
                    buf.insert(buf.end(), reinterpret_cast<const uint8_t*>(&ex),
                               reinterpret_cast<const uint8_t*>(&ex) + sizeof(ex));
                }
            }
        }
    }

    /* string pool: all strings sequential, remember offsets */
    std::vector<std::pair<int32_t, int32_t>> ic_name_refs;
    std::vector<std::pair<int32_t, int32_t>> exit_name_refs;   /* v3 */
    std::vector<std::pair<int32_t, int32_t>> exit_dest_refs;   /* v3 */
    size_t exit_name_cursor = 0;                               /* v3 patch cursor */
    int32_t str_pool_start = 0;
    {
        pad_to(1);
        /* first: stop names + destinations */
        std::vector<std::pair<int32_t, int32_t>> stop_names, stop_dests;
        for (const StopSpec& st : stops) {
            const int32_t off = static_cast<int32_t>(buf.size());
            buf.insert(buf.end(), st.name, st.name + std::strlen(st.name));
            stop_names.emplace_back(off, static_cast<int32_t>(std::strlen(st.name)));
            const char* d = st.dest ? st.dest : "";
            const int32_t doff = static_cast<int32_t>(buf.size());
            buf.insert(buf.end(), d, d + std::strlen(d));
            stop_dests.emplace_back(doff, static_cast<int32_t>(std::strlen(d)));
        }
        for (const StopSpec& st : stops) {
            for (const auto& tr : st.transfers) {
                const int32_t off = static_cast<int32_t>(buf.size());
                buf.insert(buf.end(), tr.first, tr.first + std::strlen(tr.first));
                ic_name_refs.emplace_back(off, static_cast<int32_t>(std::strlen(tr.first)));
            }
        }
        /* v3: exit names + destination strings */
        for (const StopSpec& st : stops) {
            for (const auto& ex : st.exits) {
                const int32_t off = static_cast<int32_t>(buf.size());
                buf.insert(buf.end(), ex.first, ex.first + std::strlen(ex.first));
                exit_name_refs.emplace_back(off, static_cast<int32_t>(std::strlen(ex.first)));
                for (const char* d : ex.second) {
                    const int32_t doff = static_cast<int32_t>(buf.size());
                    buf.insert(buf.end(), d, d + std::strlen(d));
                    exit_dest_refs.emplace_back(doff, static_cast<int32_t>(std::strlen(d)));
                }
            }
        }
        const int32_t rn_off = static_cast<int32_t>(buf.size());
        buf.insert(buf.end(), route_name, route_name + std::strlen(route_name));
        const int32_t sn_off = static_cast<int32_t>(buf.size());
        buf.insert(buf.end(), siding_name, siding_name + std::strlen(siding_name));
        str_pool_start = stop_names.empty() ? 0 : stop_names[0].first;
        g_dummy_route_offset = rn_off;
        g_dummy_siding_offset = sn_off;
        g_dummy_pool_start = str_pool_start;
        g_dummy_pool_end = static_cast<int32_t>(buf.size());
        g_dummy_stop_names = stop_names;
        g_dummy_stop_dests = stop_dests;
    }

    pad_to(alignof(JcmStop));
    const int32_t stop_offset = static_cast<int32_t>(buf.size());
    {
        /* patch interchange name offsets in place */
        JcmInterchange* ic_base = reinterpret_cast<JcmInterchange*>(buf.data() + ic_offset);
        size_t k = 0;
        for (const StopSpec& st : stops) {
            for (size_t t = 0; t < st.transfers.size(); t++, k++) {
                ic_base[k].route_name_offset = ic_name_refs[k].first;
            }
        }
        int32_t ic_cursor = ic_offset;
        for (size_t i = 0; i < stops.size(); i++) {
            JcmStop s{};
            s.route_id = 42;
            s.station_id = stops[i].station_id;
            s.platform_id = stops[i].station_id * 10;
            s.distance = stops[i].distance;
            s.dwell_time_millis = 15000;
            s.name_offset = g_dummy_stop_names[i].first;
            s.name_len = g_dummy_stop_names[i].second;
            s.destination_offset = g_dummy_stop_dests[i].first;
            s.destination_len = g_dummy_stop_dests[i].second;
            s.custom_destination_offset = -1;
            s.custom_destination_len = 0;
            s.interchange_count = static_cast<int32_t>(stops[i].transfers.size());
            s.interchange_offset = stops[i].transfers.empty() ? 0 : ic_cursor;
            s.exit_count = static_cast<int32_t>(stops[i].exits.size());
            s.exit_offset = stops[i].exits.empty() ? 0 : per_stop_exit_pool[i];
            s.is_route_switchover = 0;
            if (!stops[i].transfers.empty()) {
                ic_cursor += static_cast<int32_t>(stops[i].transfers.size() * sizeof(JcmInterchange));
            }
            buf.insert(buf.end(), reinterpret_cast<const uint8_t*>(&s),
                       reinterpret_cast<const uint8_t*>(&s) + sizeof(s));
        }
    }

    /* v3: destination str-ref pool (after stops; offsets are blob-relative)
       + patch JcmExit name/destination offsets in place */
    if (exit_pool_off != 0 && !exit_dest_refs.empty()) {
        pad_to(alignof(JcmStrRef));
        const int32_t dest_refs_off = static_cast<int32_t>(buf.size());
        size_t dr = 0;
        for (size_t i = 0; i < stops.size(); i++) {
            if (stops[i].exits.empty()) continue;
            JcmExit* base = reinterpret_cast<JcmExit*>(buf.data() + per_stop_exit_pool[i]);
            size_t nr = 0;
            for (const auto& ex : stops[i].exits) {
                base[nr].name_offset = exit_name_refs[exit_name_cursor + nr].first;
                base[nr].destination_offset = dest_refs_off
                    + static_cast<int32_t>(dr * sizeof(JcmStrRef));
                dr += ex.second.size();
                nr++;
            }
            exit_name_cursor += stops[i].exits.size();
        }
        for (const auto& ref : exit_dest_refs) {
            JcmStrRef r{};
            r.offset = ref.first;
            r.len = ref.second;
            buf.insert(buf.end(), reinterpret_cast<const uint8_t*>(&r),
                       reinterpret_cast<const uint8_t*>(&r) + sizeof(r));
        }
    }

    /* header */
    JcmVehicleSnapshot* hdr = reinterpret_cast<JcmVehicleSnapshot*>(buf.data());
    hdr->vehicle_id = 123456789;
    hdr->siding_id = 7;
    hdr->this_route_id = 42;
    hdr->departure_index = 38;
    hdr->car_count = car_count;
    hdr->transport_mode = 0;
    hdr->speed_kmh = speed_ms * 3.6;
    hdr->speed_ms = speed_ms;
    hdr->rail_progress = 3400.0;
    hdr->door_value = door_value;
    hdr->notch_level = 3;
    hdr->reversed = reversed ? 1 : 0;
    hdr->on_route = on_route ? 1 : 0;
    hdr->door_opening = door_value > 0.05 && door_value < 0.95 ? 1 : 0;
    hdr->currently_manual = 0;
    hdr->manual_allowed = 1;
    hdr->client_player_riding = 1;
    hdr->any_car_rendered = 1;
    hdr->total_dwell_time_millis = 15000;
    hdr->elapsed_dwell_time_millis = 0;
    hdr->game_time_millis = game_time_millis;
    hdr->in_game_time = 6000;
    hdr->car_offset = car_offset;
    hdr->stop_count = static_cast<int32_t>(stops.size());
    hdr->stop_offset = stop_offset;
    hdr->this_route_stop_count = static_cast<int32_t>(stops.size());
    hdr->this_route_stop_offset = stop_offset;
    hdr->next_route_stop_count = 0;
    hdr->next_route_stop_offset = 0;
    hdr->next_stop_index = next_stop_index;
    hdr->route_name_offset = g_dummy_route_offset;
    hdr->route_name_len = static_cast<int32_t>(std::strlen(route_name));
    hdr->route_color = static_cast<int32_t>(route_color);
    hdr->circular_state = circular_state;
    hdr->siding_name_offset = g_dummy_siding_offset;
    hdr->siding_name_len = static_cast<int32_t>(std::strlen(siding_name));
    hdr->string_pool_offset = g_dummy_pool_start;
    hdr->string_pool_len = g_dummy_pool_end - g_dummy_pool_start;
    return buf;
}

/* ------------------------------------------------------------------ */
/* module driver                                                       */
/* ------------------------------------------------------------------ */

struct Module {
    void* handle = nullptr;
    uint32_t (*abi)(void) = nullptr;
    const char* (*type)(void) = nullptr;
    const char* (*id)(void) = nullptr;
    size_t (*state_size)(void) = nullptr;
    int32_t (*create)(const JcmFrameInput*) = nullptr;
    int32_t (*render)(const JcmFrameInput*, JcmFrameOutput*) = nullptr;
    int32_t (*dispose)(const JcmFrameInput*) = nullptr;
    void* state = nullptr;

    bool open(const char* path) {
        handle = dlopen(path, RTLD_NOW | RTLD_LOCAL);
        if (!handle) { std::printf("dlopen(%s) failed: %s\n", path, dlerror()); return false; }
        abi = reinterpret_cast<uint32_t(*)(void)>(dlsym(handle, "mtrNativeAbiVersion"));
        type = reinterpret_cast<const char*(*)(void)>(dlsym(handle, "mtrScriptType"));
        id = reinterpret_cast<const char*(*)(void)>(dlsym(handle, "mtrScriptId"));
        state_size = reinterpret_cast<size_t(*)(void)>(dlsym(handle, "mtrStateSize"));
        create = reinterpret_cast<int32_t(*)(const JcmFrameInput*)>(dlsym(handle, "mtrCreate"));
        render = reinterpret_cast<int32_t(*)(const JcmFrameInput*, JcmFrameOutput*)>(dlsym(handle, "mtrRender"));
        dispose = reinterpret_cast<int32_t(*)(const JcmFrameInput*)>(dlsym(handle, "mtrDispose"));
        return abi && type && id && state_size && create && render && dispose;
    }
};

/* texture reconstruction from frame uploads */
struct ReconTex {
    int32_t w = 0, h = 0;
    std::vector<uint8_t> px;   /* RGBA */
};

static void apply_frame(const JcmFrameOutput& out, std::map<int32_t, ReconTex>& texes,
                        int64_t& model_records, int64_t& upload_records,
                        int64_t& upload_bytes) {
    const uint8_t* p = static_cast<const uint8_t*>(out.records);
    for (int32_t i = 0; i < out.record_count; i++) {
        const JcmRecordHeader* h = reinterpret_cast<const JcmRecordHeader*>(p);
        if (h->kind == JCM_DRAW_MODEL) {
            model_records++;
        } else if (h->kind == JCM_DRAW_TEXTURE_UPLOAD) {
            const JcmDrawTextureUpload* u =
                reinterpret_cast<const JcmDrawTextureUpload*>(p);
            upload_records++;
            upload_bytes += u->pixel_data_len;
            if (u->texture_handle >= 0 && u->width > 0 && u->height > 0) {
                ReconTex& t = texes[u->texture_handle];
                if ((int)t.px.size() != u->width * u->height * 4) {
                    t.w = u->width; t.h = u->height;
                    t.px.assign(static_cast<size_t>(u->width) * u->height * 4, 0);
                }
                for (int32_t y = 0; y < u->dirty_h; y++) {
                    const int64_t src_off = u->pixel_data_offset
                        + static_cast<int64_t>(y) * u->dirty_w * 4;
                    std::memcpy(&t.px[static_cast<size_t>(u->dirty_y + y) * u->width * 4
                                  + static_cast<size_t>(u->dirty_x) * 4],
                                out.pixel_arena + src_off,
                                static_cast<size_t>(u->dirty_w) * 4);
                }
            }
        }
        p += h->record_size;
    }
}

static void write_ppm(const char* path, const ReconTex& t) {
    FILE* f = std::fopen(path, "wb");
    if (!f) return;
    std::fprintf(f, "P6\n%d %d\n255\n", t.w, t.h);
    for (size_t i = 0; i < t.px.size(); i += 4) {
        /* RGBA storage is little-endian ARGB: [B, G, R, A] */
        const uint8_t rgb[3] = {t.px[i + 2], t.px[i + 1], t.px[i]};
        std::fwrite(rgb, 1, 3, f);
    }
    std::fclose(f);
    std::printf("  wrote %s (%dx%d)\n", path, t.w, t.h);
}

static uint32_t sample_px(const ReconTex& t, int x, int y) {
    const size_t idx = (static_cast<size_t>(y) * t.w + x) * 4;
    /* little-endian ARGB: px[i+2]=R, px[i+1]=G, px[i]=B */
    return (static_cast<uint32_t>(t.px[idx + 2]) << 16)
         | (static_cast<uint32_t>(t.px[idx + 1]) << 8)
         | static_cast<uint32_t>(t.px[idx]);
}

static bool close_to(uint32_t a, uint32_t b, int tol) {
    const int dr = std::abs(static_cast<int>((a >> 16) & 0xFF) - static_cast<int>((b >> 16) & 0xFF));
    const int dg = std::abs(static_cast<int>((a >> 8) & 0xFF) - static_cast<int>((b >> 8) & 0xFF));
    const int db = std::abs(static_cast<int>(a & 0xFF) - static_cast<int>(b & 0xFF));
    return dr <= tol && dg <= tol && db <= tol;
}

/* opaque near-black ink (plates have a transparent background). */
static int count_ink(const ReconTex& t) {
    int n = 0;
    for (size_t i = 0; i < t.px.size(); i += 4) {
        if (t.px[i + 3] >= 200 && t.px[i] < 70 && t.px[i + 1] < 70 && t.px[i + 2] < 70) n++;
    }
    return n;
}

static int count_color(const ReconTex& t, uint32_t want, int tol,
                       int x0, int y0, int x1, int y1) {
    int n = 0;
    for (int y = y0; y < y1 && y < t.h; y++) {
        for (int x = x0; x < x1 && x < t.w; x++) {
            if (close_to(sample_px(t, x, y), want, tol)) n++;
        }
    }
    return n;
}

/* ------------------------------------------------------------------ */
/* scenes                                                              */
/* ------------------------------------------------------------------ */

static const CarSpec kCars[8] = {{20}, {19}, {19}, {19}, {19}, {19}, {19}, {20}};

static std::vector<StopSpec> linear_stops() {
    const uint32_t c1 = 0xFFE60012;   /* 1号线 red */
    const uint32_t c2 = 0xFF00A651;   /* 2号线 green */
    const uint32_t c3 = 0xFF005BAC;   /* 3号线 */
    const uint32_t cs = 0xFFF15A22;   /* S1 */
    return {
        {"太原站|Taiyuan Railway Station", 0,    101, "",     {{"2号线|Line 2", c2}},
         {{"A", {"火车南站|South Railway Station"}},
          {"B", {"长途汽车站|Coach Terminal", "迎泽公园|Yingze Park"}}}},
        {"迎泽大街|Yingze Avenue",          900,  102, "",     {}},
        {"青年路口|Qingnian Lukou",         1800, 103, "",     {}},
        {"大南门|Dananmen",                 2700, 104, "",     {{"1号线|Line 1", c1}},
         {{"A", {"柳巷商业区|Liuxiang District"}}}},
        {"体育馆|Tiyuguan",                 3600, 105, "",     {},
         {{"A", {"体育中心|Sports Center", "滨河体育場|Binhe Stadium"}},
          {"B", {"游泳馆|Natatorium"}},
          {"C", {"公交枢纽|Transit Hub"}}}},
        {"长风街|Changfeng Street",         4500, 106, "",     {{"3号线|Line 3", c3}, {"S1线|Line S1", cs}}},
        {"学府街|Xuefu Street",             5400, 107, "",     {}},
        {"南中环|Nanzhonghuan",             6300, 108, "",     {}},
        {"晋阳街|Jinyang Street",           7200, 109, "",     {}},
        {"西桥|Xiqiao",                     8100, 110, "西桥|Xiqiao", {}},
    };
}

static std::vector<StopSpec> circular_stops() {
    std::vector<StopSpec> s;
    static const char* names[] = {
        "西北延|Xibei Yan", "涧河路|Jianhe Road", "动物园|Zoo",
        "胜利桥东|Shengliqiaodong", "府西街|Fuxi Street", "迎泽公园|Yingze Park",
        "南内环|Nanneihuan", "长风街|Changfeng Street", "体育中心|Sports Center",
        "晋阳湖|Jinyang Lake", "兴华街|Xinghua Street", "玉门河|Yumenhe",
    };
    for (int i = 0; i < 12; i++) {
        StopSpec st{};
        st.name = names[i];
        st.distance = i * 800.0;
        st.station_id = 200 + i;
        st.dest = "";
        if (i == 4) st.transfers.push_back({"2号线|Line 2", 0xFF00A651});
        if (i == 7) st.transfers.push_back({"1号线|Line 1", 0xFFE60012});
        s.push_back(st);
    }
    /* 环线: 末站回到首站 (route platform loop) */
    s.push_back({"西北延|Xibei Yan", 12 * 800.0, 200, "", {}});
    return s;
}

/* ------------------------------------------------------------------ */

int main() {
    std::printf("== jslcd smoke ==\n\n");
    JcmHostServices host = make_host();
    int failures = 0;
    auto check = [&](bool ok, const char* what) {
        std::printf("  [%s] %s\n", ok ? "PASS" : "FAIL", what);
        if (!ok) failures++;
    };

    /* ============ module 1: jslcd_vehicle (LCD) ============ */
    {
        Module m;
        if (!m.open("./libjslcd_vehicle.so")) { std::printf("cannot open LCD module\n"); return 1; }
        std::printf("jslcd_vehicle: id=%s type=%s abi=%u state=%zu bytes\n\n",
                    m.id(), m.type(), m.abi(), m.state_size());
        check(m.abi() == MTR_NATIVE_ABI_VERSION, "ABI version matches host");
        check(std::strcmp(m.type(), "vehicle") == 0, "script type is vehicle");
        m.state = std::calloc(1, m.state_size());

        const auto stops = linear_stops();
        auto snap = build_snapshot(8, kCars, stops,
                                   1000000, 0.0, 17.0, 4,
                                   "10号线|Line 10", 0xFF009BC0, 0,
                                   "10010/01-02-03-04-05-06-07-08", false, true);

        JcmFrameInput in{};
        in.abi_version = MTR_NATIVE_ABI_VERSION;
        in.resource_kind = MTR_RESOURCE_VEHICLE;
        in.snapshot = snap.data();
        in.state = m.state;
        in.state_size = m.state_size();
        in.host = &host;

        check(m.create(&in) == 0, "mtrCreate");

        std::map<int32_t, ReconTex> texes;
        JcmFrameOutput out{};
        int64_t models = 0, uploads = 0, upbytes = 0;
        check(m.render(&in, &out) == 0, "mtrRender frame 1 (linear, full map)");
        apply_frame(out, texes, models, uploads, upbytes);
        check(models == 16, "16 model records (8 cars x 2 screens)");
        /* repaint budget: 2 cars/frame → full train over 4 frames */
        for (int f = 0; f < 9; f++) {
            m.render(&in, &out);
            apply_frame(out, texes, models, uploads, upbytes);
        }
        check(uploads >= 16, "first 10 frames upload all 16 textures (staggered)");
        std::printf("  uploads=%lld bytes=%lldMB\n",
                    (long long)uploads, (long long)(upbytes / 1024 / 1024));

        /* dump car 0 left */
        if (!texes.empty()) {
            write_ppm("/tmp/jslcd_linear_car0_left.ppm", texes.begin()->second);
            const ReconTex& t = texes.begin()->second;
            std::printf("  texture size %dx%d\n", t.w, t.h);
            /* header band: route color at top */
            const uint32_t hdr = sample_px(t, t.w / 2, 10);
            check(close_to(hdr, 0x009BC0, 40), "header band is route color (cyan)");
            /* body white-ish */
            const uint32_t body = sample_px(t, t.w / 2, t.h - 30);
            check(close_to(body, 0xFFFFFF, 25), "body background is white");
            /* station dots: count red+green in map band */
            const int dots = count_color(t, 0xED1C24, 60, 0, t.h * 55 / 100, t.w, t.h * 72 / 100)
                           + count_color(t, 0x00C850, 60, 0, t.h * 55 / 100, t.w, t.h * 72 / 100);
            check(dots > 200, "station dots present (red/green)");
            /* 车号 glass card: white 车号 glyphs on the route-color card
               in the header right zone (header text is white too, but it
               lives in the middle; the right 20% is card-only). */
            const int glyphs = count_color(t, 0xFFFFFF, 30, t.w * 80 / 100, 4, t.w, t.h * 28 / 100);
            check(glyphs > 100, "车号 glass card glyphs present (white digits)");

            /* v3: exit panel — next stop 太原站 (index 0+1? no: door closed,
               full map shows current = 太原站 which carries exits A/B).
               Panel lives at x = TEX_W-420 .. TEX_W (right 15%), below
               the transfer badges (y >= 178/480 of body height). Exit
               letters are painted route-cyan (0x009BC0); destinations
               black + gray. Assert cyan glyphs in that window. */
            const int exitLetters = count_color(t, 0x009BC0, 60,
                                                t.w * 84 / 100, t.h * 45 / 100,
                                                t.w, t.h * 100 / 100);
            check(exitLetters > 40, "v3 exit panel: cyan exit letters present (出站口 A/B)");
            /* destination text is black on white in the same window */
            const int exitBlack = count_color(t, 0x000000, 40,
                                              t.w * 84 / 100, t.h * 45 / 100,
                                              t.w, t.h);
            check(exitBlack > 60, "v3 exit panel: destination text present (black CJK)");
        }

        /* steady state: 200 frames, nothing changes → zero uploads */
        models = uploads = upbytes = 0;
        for (int f = 0; f < 200; f++) {
            m.render(&in, &out);
            apply_frame(out, texes, models, uploads, upbytes);
        }
        check(uploads == 0, "steady state: 200 unchanged frames → 0 uploads (repaint-on-change)");
        check(models == 16 * 200, "steady state still emits model records every frame");

        /* blink toggle → staggered repaint: 8 stale cars, budget 2/frame
           → 4 frames × 2 cars × 2 sides = 16 uploads, then quiescent */
        {
            auto snap2 = build_snapshot(8, kCars, stops,
                                        1000000 + 1000, 0.0, 17.0, 4,
                                        "10号线|Line 10", 0xFF009BC0, 0,
                                        "10010/01-02-03-04-05-06-07-08", false, true);
            JcmFrameInput in2 = in;
            in2.snapshot = snap2.data();
            int64_t mo = 0, up_total = 0, ub = 0;
            for (int f = 0; f < 10; f++) {
                int64_t up1 = 0;
                m.render(&in2, &out);
                apply_frame(out, texes, mo, up1, ub);
                up_total += up1;
            }
            check(up_total == 16, "blink repaint completes in 4 frames (16 staggered uploads)");
        }

        /* door open scene → big station name page */
        {
            auto snap3 = build_snapshot(8, kCars, stops,
                                        1000000 + 60000, 0.85, 0.0, 4,
                                        "10号线|Line 10", 0xFF009BC0, 0,
                                        "10010/01-02-03-04-05-06-07-08", false, true);
            JcmFrameInput in3 = in;
            in3.snapshot = snap3.data();
            int64_t mo = 0, up = 0, ub = 0;
            m.render(&in3, &out);
            std::map<int32_t, ReconTex> texes3;
            apply_frame(out, texes3, mo, up, ub);
            /* second render to flush (first may be white-clear) */
            m.render(&in3, &out);
            apply_frame(out, texes3, mo, up, ub);
            if (!texes3.empty()) {
                const ReconTex& t = texes3.begin()->second;
                write_ppm("/tmp/jslcd_dooropen_car0_left.ppm", t);
                /* 大站名: route color text in the middle region */
                const int big = count_color(t, 0x009BC0, 50, 0, t.h * 32 / 100, t.w, t.h * 72 / 100);
                check(big > 500, "door-open page shows big station name in route color");
            }
        }

        /* circular scene */
        {
            const auto cstops = circular_stops();
            auto snap4 = build_snapshot(8, kCars, cstops,
                                        2000000, 0.0, 15.0, 5,
                                        "2号线|Line 2", 0xFF00A651, 2,
                                        "10010/01-02-03-04-05-06-07-08", false, true);
            JcmFrameInput in4 = in;
            in4.snapshot = snap4.data();
            /* new instance state to avoid route cache staleness */
            void* state2 = std::calloc(1, m.state_size());
            in4.state = state2;
            in4.state_size = m.state_size();
            m.create(&in4);
            JcmFrameOutput out4{};
            check(m.render(&in4, &out4) == 0, "mtrRender circular (外环, full map)");
            std::map<int32_t, ReconTex> texes4;
            int64_t mo = 0, up = 0, ub = 0;
            apply_frame(out4, texes4, mo, up, ub);
            if (!texes4.empty()) {
                const ReconTex& t = texes4.begin()->second;
                write_ppm("/tmp/jslcd_circular_car0_left.ppm", t);
                /* ring: route color pixels along ring band */
                const int ring = count_color(t, 0x00A651, 40, 0, t.h * 40 / 100, t.w, t.h * 65 / 100);
                check(ring > 2000, "circular ring painted in route color (green)");
                const uint32_t hdr = sample_px(t, t.w / 2, 10);
                check(close_to(hdr, 0x00A651, 40), "circular header band route color");
            }
            std::free(state2);
        }

        /* ---- timing: steady (skip) vs forced-repaint (JS equivalent) ---- */
        {
            const int N = 2000;
            auto snapS = build_snapshot(8, kCars, stops, 3000000, 0.0, 17.0, 4,
                                        "10号线|Line 10", 0xFF009BC0, 0,
                                        "10010/01-02-03-04-05-06-07-08", false, true);
            JcmFrameInput inS = in; inS.snapshot = snapS.data();
            /* fresh state so first paint happens outside the timed loop */
            void* stS = std::calloc(1, m.state_size());
            inS.state = stS; inS.state_size = m.state_size();
            m.create(&inS);
            m.render(&inS, &out);
            const auto t0 = steady_clock::now();
            for (int f = 0; f < N; f++) m.render(&inS, &out);
            const auto t1 = steady_clock::now();
            const double steady_us = duration<double, std::micro>(t1 - t0).count() / N;
            std::free(stS);

            /* forced: blink toggles every frame (advance 1000 ms/frame —
               this is exactly what the JS does implicitly: repaint all,
               every frame, because it has no change detection) */
            void* stF = std::calloc(1, m.state_size());
            JcmFrameInput inF = in;
            inF.state = stF; inF.state_size = m.state_size();
            m.create(&inF);
            int64_t mo = 0, up = 0, ub = 0;
            double forced_us = 0;
            {
                std::vector<uint8_t> snapF;
                JcmFrameOutput outF{};
                const auto s0 = steady_clock::now();
                for (int f = 0; f < N; f++) {
                    snapF = build_snapshot(8, kCars, stops, 3000000 + (int64_t)f * 1000,
                                           0.0, 17.0, 4, "10号线|Line 10", 0xFF009BC0, 0,
                                           "10010/01-02-03-04-05-06-07-08", false, true);
                    inF.snapshot = snapF.data();
                    m.render(&inF, &outF);
                }
                const auto s1 = steady_clock::now();
                forced_us = duration<double, std::micro>(s1 - s0).count() / N;
            }
            std::free(stF);

            std::printf("\n  timing (8 cars, 10 stations):\n");
            std::printf("    steady-state  : %8.2f us/frame (models only, no repaint)\n", steady_us);
            std::printf("    full repaint  : %8.2f us/frame (blink every frame, JS-equivalent)\n", forced_us);
            check(steady_us < 100.0, "steady-state frame cost < 100 us");
            check(forced_us < 20000.0, "repaint-budget frame cost < 20 ms (2 cars/frame)");
        }

        check(m.dispose(&in) == 0, "mtrDispose");
        std::free(m.state);
    }

    std::printf("\n");

    /* ============ module 2: jslcd_train_num (车号) ============ */
    {
        Module m;
        if (!m.open("./libjslcd_train_num.so")) { std::printf("cannot open train_num module\n"); return 1; }
        std::printf("jslcd_train_num: id=%s type=%s abi=%u state=%zu bytes\n\n",
                    m.id(), m.type(), m.abi(), m.state_size());
        check(std::strcmp(m.id(), "jslcd:train_num") == 0, "script id");
        m.state = std::calloc(1, m.state_size());

        const auto stops = linear_stops();
        auto snap = build_snapshot(8, kCars, stops,
                                   1000000, 0.0, 17.0, 4,
                                   "10号线|Line 10", 0xFF009BC0, 0,
                                   "10010/01-02-03-04-05-06-07-08", false, true);

        JcmFrameInput in{};
        in.abi_version = MTR_NATIVE_ABI_VERSION;
        in.resource_kind = MTR_RESOURCE_VEHICLE;
        in.snapshot = snap.data();
        in.state = m.state;
        in.state_size = m.state_size();
        in.host = &host;

        check(m.create(&in) == 0, "mtrCreate (transparent clear)");

        std::map<int32_t, ReconTex> texes;
        JcmFrameOutput out{};
        int64_t models = 0, uploads = 0, upbytes = 0;
        check(m.render(&in, &out) == 0, "mtrRender frame 1");
        apply_frame(out, texes, models, uploads, upbytes);
        check(models == 18, "18 model records (8 cars x 2 + head + tail)");
        check(uploads >= 18, "first frame paints all 18 plates");

        /* steady state: no siding change → nothing uploaded */
        models = uploads = upbytes = 0;
        for (int f = 0; f < 100; f++) {
            m.render(&in, &out);
            apply_frame(out, texes, models, uploads, upbytes);
        }
        check(uploads == 0, "steady state: siding unchanged → 0 uploads");

        /* siding change → repaint all plates with new 车号 */
        {
            auto snap2 = build_snapshot(8, kCars, stops,
                                        1000000 + 5000, 0.0, 17.0, 4,
                                        "10号线|Line 10", 0xFF009BC0, 0,
                                        "10011/11-12-13-14-15-16-17-18", false, true);
            JcmFrameInput in2 = in;
            in2.snapshot = snap2.data();
            int64_t mo = 0, up = 0, ub = 0;
            m.render(&in2, &out);
            apply_frame(out, texes, mo, up, ub);
            check(up >= 18, "siding name change repaints all plates");
        }

        /* dump one car plate + head plate: sequential host handles —
           16 car plates, then head, then tail. */
        if (texes.size() >= 17) {
            auto it = texes.begin();
            write_ppm("/tmp/jslcd_num_car0_left.ppm", it->second);
            auto fwd_it = std::next(it, 16);
            write_ppm("/tmp/jslcd_num_fwd.ppm", fwd_it->second);
            /* black digits on transparent bg: count opaque ink pixels */
            const int ink = count_ink(it->second);
            check(ink > 200, "car plate has black 车厢号 glyphs (opaque ink)");
            const int ink_fwd = count_ink(fwd_it->second);
            check(ink_fwd > 100, "head plate has black 车号 glyphs");
        }

        check(m.dispose(&in) == 0, "mtrDispose");
        std::free(m.state);
    }

    std::printf("\n== %s (%d failures) ==\n", failures ? "SMOKE FAILED" : "SMOKE PASSED", failures);
    return failures ? 1 : 0;
}
