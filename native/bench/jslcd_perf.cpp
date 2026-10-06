/* quick perf probe: where does a jslcd repaint frame spend its time? */
#include <mtr/mtr_native.h>
#include <chrono>
#include <cstdio>
#include <cstring>
#include <cstdlib>
#include <vector>
#include <dlfcn.h>

using namespace std::chrono;

static int32_t p_model = 100, p_tex = 1;
static int32_t hs_am(void*, const char*) { return p_model++; }
static void hs_rm(void*, int32_t) {}
static int32_t hs_ct(void*, int32_t, int32_t) { return p_tex++; }
static void hs_rt(void*, int32_t) {}
static int32_t hs_ras(void*, const char*, int32_t, int32_t x, int32_t y, int32_t max_w,
                      uint8_t r, uint8_t g, uint8_t b, uint8_t* out, int32_t w, int32_t h) {
    if (max_w <= 0) return -1;
    const int32_t gw = max_w - 1, gh = (int32_t)(max_w * 0.92);
    for (int32_t py = y; py < y + gh; py++) {
        if (py < 0 || py >= h) continue;
        for (int32_t px = x; px < x + gw; px++) {
            if (px < 0 || px >= w) continue;
            const size_t i = ((size_t)py * w + px) * 4;
            /* little-endian ARGB buffer: byte0=B, byte1=G, byte2=R */
            out[i] = (uint8_t)((190 * b + 65 * out[i]) / 255);
            out[i+1] = (uint8_t)((190 * g + 65 * out[i+1]) / 255);
            out[i+2] = (uint8_t)((190 * r + 65 * out[i+2]) / 255);
            out[i+3] = 255;
        }
    }
    return 1;
}
static void hs_log(void*, int32_t, const char*, int32_t) {}

struct Stop { const char* n; double d; int64_t sid; const char* dest;
              std::vector<std::pair<const char*, uint32_t>> tr; };

static std::vector<uint8_t> build(int cars, const std::vector<Stop>& stops,
                                  int64_t t, double door, int32_t nsi) {
    std::vector<uint8_t> b(sizeof(JcmVehicleSnapshot));
    auto pad = [&](size_t a) { while (b.size() % a) b.push_back(0); };
    pad(alignof(JcmCar));
    const int32_t car_off = b.size();
    for (int i = 0; i < cars; i++) {
        JcmCar c{}; c.length = 20; c.width = 2.5F; c.rendered = 1;
        b.insert(b.end(), (const uint8_t*)&c, (const uint8_t*)&c + sizeof(c));
    }
    pad(alignof(JcmInterchange));
    const int32_t ic_off = b.size();
    std::vector<std::pair<int32_t,int32_t>> icnames;
    for (auto& s : stops) for (auto& t2 : s.tr) {
        JcmInterchange ic{}; ic.color = (int32_t)t2.second;
        b.insert(b.end(), (const uint8_t*)&ic, (const uint8_t*)&ic + sizeof(ic));
    }
    std::vector<std::pair<int32_t,int32_t>> names, dests;
    for (auto& s : stops) {
        int32_t o = b.size();
        b.insert(b.end(), s.n, s.n + strlen(s.n));
        names.push_back({o, (int32_t)strlen(s.n)});
        const char* d = s.dest ? s.dest : "";
        int32_t o2 = b.size();
        b.insert(b.end(), d, d + strlen(d));
        dests.push_back({o2, (int32_t)strlen(d)});
    }
    for (auto& s : stops) for (auto& t2 : s.tr) {
        int32_t o = b.size();
        b.insert(b.end(), t2.first, t2.first + strlen(t2.first));
        icnames.push_back({o, (int32_t)strlen(t2.first)});
    }
    const char* rn = "10号线|Line 10";
    int32_t rn_off = b.size();
    b.insert(b.end(), rn, rn + strlen(rn));
    const char* sn = "10010/01-02-03-04-05-06-07-08";
    int32_t sn_off = b.size();
    b.insert(b.end(), sn, sn + strlen(sn));
    pad(alignof(JcmStop));
    const int32_t stop_off = b.size();
    {
        JcmInterchange* icb = (JcmInterchange*)(b.data() + ic_off);
        size_t k = 0;
        int32_t cur = ic_off;
        for (size_t i = 0; i < stops.size(); i++) {
            JcmStop s{};
            s.route_id = 42; s.station_id = stops[i].sid; s.platform_id = stops[i].sid * 10;
            s.distance = stops[i].d; s.dwell_time_millis = 15000;
            s.name_offset = names[i].first; s.name_len = names[i].second;
            s.destination_offset = dests[i].first; s.destination_len = dests[i].second;
            s.custom_destination_offset = -1;
            s.interchange_count = (int32_t)stops[i].tr.size();
            s.interchange_offset = stops[i].tr.empty() ? 0 : cur;
            for (size_t t3 = 0; t3 < stops[i].tr.size(); t3++, k++)
                icb[k].route_name_offset = icnames[k].first;
            cur += (int32_t)(stops[i].tr.size() * sizeof(JcmInterchange));
            b.insert(b.end(), (const uint8_t*)&s, (const uint8_t*)&s + sizeof(s));
        }
    }
    JcmVehicleSnapshot* h = (JcmVehicleSnapshot*)b.data();
    h->vehicle_id = 1; h->this_route_id = 42; h->car_count = cars;
    h->speed_ms = 17; h->speed_kmh = 61; h->rail_progress = 3400;
    h->door_value = door; h->notch_level = 3; h->on_route = 1;
    h->any_car_rendered = 1; h->client_player_riding = 1;
    h->game_time_millis = t; h->in_game_time = 6000;
    h->car_offset = car_off; h->stop_count = stops.size(); h->stop_offset = stop_off;
    h->this_route_stop_count = stops.size(); h->this_route_stop_offset = stop_off;
    h->next_stop_index = nsi;
    h->route_name_offset = rn_off; h->route_name_len = strlen(rn);
    h->route_color = (int32_t)0xFF009BC0; h->circular_state = 0;
    h->siding_name_offset = sn_off; h->siding_name_len = strlen(sn);
    return b;
}

int main() {
    JcmHostServices hs{};
    hs.acquire_model = hs_am; hs.release_model = hs_rm;
    hs.create_texture = hs_ct; hs.release_texture = hs_rt;
    hs.rasterize_text = hs_ras; hs.log = hs_log;

    void* so = dlopen("./libjslcd_vehicle.so", RTLD_NOW | RTLD_LOCAL);
    if (!so) { printf("dlopen fail: %s\n", dlerror()); return 1; }
    auto render = (int32_t(*)(const JcmFrameInput*, JcmFrameOutput*))dlsym(so, "mtrRender");
    auto create = (int32_t(*)(const JcmFrameInput*))dlsym(so, "mtrCreate");
    auto sz = (size_t(*)(void))dlsym(so, "mtrStateSize");

    std::vector<Stop> stops = {
        {"太原站|Taiyuan Railway Station", 0, 101, "", {{"2号线|Line 2", 0xFF00A651}}},
        {"迎泽大街|Yingze Avenue", 900, 102, "", {}},
        {"青年路口|Qingnian Lukou", 1800, 103, "", {}},
        {"大南门|Dananmen", 2700, 104, "", {{"1号线|Line 1", 0xFFE60012}}},
        {"体育馆|Tiyuguan", 3600, 105, "", {}},
        {"长风街|Changfeng Street", 4500, 106, "", {{"3号线|Line 3", 0xFF005BAC}, {"S1线|Line S1", 0xFFF15A22}}},
        {"学府街|Xuefu Street", 5400, 107, "", {}},
        {"南中环|Nanzhonghuan", 6300, 108, "", {}},
        {"晋阳街|Jinyang Street", 7200, 109, "", {}},
        {"西桥|Xiqiao", 8100, 110, "西桥|Xiqiao", {}},
    };

    for (int cars : {1, 8}) {
        auto snap = build(cars, stops, 1000000, 0.0, 4);
        void* st = calloc(1, sz());
        JcmFrameInput in{};
        in.abi_version = MTR_NATIVE_ABI_VERSION;
        in.resource_kind = MTR_RESOURCE_VEHICLE;
        in.snapshot = snap.data();
        in.state = st; in.state_size = sz(); in.host = &hs;
        create(&in);
        JcmFrameOutput out{};
        render(&in, &out);
        const int N = 300;
        auto t0 = steady_clock::now();
        for (int f = 0; f < N; f++) {
            ((JcmVehicleSnapshot*)snap.data())->game_time_millis = 1000000 + (int64_t)f * 1000;
            render(&in, &out);
        }
        auto t1 = steady_clock::now();
        printf("cars=%d  repaint+upload: %.2f us/frame\n", cars,
               duration<double, std::micro>(t1 - t0).count() / N);
        free(st);
    }
    return 0;
}
