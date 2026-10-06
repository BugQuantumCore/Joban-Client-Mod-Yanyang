/**
 * jslcd_common.hpp — shared port of the community "JS LCD" vehicle pack.
 *
 * Source scripts (JS → here, 1:1):
 *   config.js          → colors / timing / layout constants / fonts
 *   util.js            → text split, siding 车号 parsing, door & speed
 *                        helpers, current-station index
 *   train_num_util.js  → parseSidingName / getCarDisplayNum
 *   data.js            → StationInfo + getStationsFromStops +
 *                        transfer route parse / sort / 拼音首字母
 *   circular.js        → resolveCircularState (环线判定, lock semantics)
 *   mtr_util.js        → train status machine
 *
 * The JS versions cross into Rhino/NativeJavaObject for every getter;
 * the C++ versions read the flat JcmVehicleSnapshot once per frame.
 */
#pragma once

#include <mtr/script.hpp>
#include <mtr/gfx2d.hpp>
#include <string>
#include <vector>
#include <cstring>
#include <cstdio>
#include <cstdlib>
#include <cmath>

namespace jslcd {

/* ==================== config.js ==================== */

/* Physical LCD quad (model space, from the JS pack):
   p0→p1 = width, p1→p2 = height. */
constexpr double LCD_POS_L[4][3] = {
    {0.8627472, 2.4004532, -10.5625},
    {1.1772487, 2.1962136, -10.5625},
    {1.1772487, 2.1962136, -9.25},
    {0.8627472, 2.4004532, -9.25}
};
constexpr double LCD_POS_R[4][3] = {
    {-0.8627472, 2.4004532, -9.4375},
    {-1.1772487, 2.1962136, -9.4375},
    {-1.1772487, 2.1962136, -10.75},
    {-0.8627472, 2.4004532, -10.75}
};

/* computeLcdAspectFromSlot — physical aspect from the quad above.
 *
 * ★ Ported-bug note: the JS helper returns |v0→v1| / |v1→v2|, which for
 *   LCD_POS_L/R (vertex order TL→BL→BR→TR: v0→v1 is the SHORT/vertical
 *   edge, v1→v2 the LONG/horizontal edge) yields the RECIPROCAL of the
 *   intended aspect (0.286 instead of the "≈ 3.5" its own comment
 *   claims), blowing SCR_H up to 11564 px. The native port takes
 *   long_edge / short_edge — 3.5 for these quads — matching config.js's
 *   design constants (LCD_ASPECT 3.5, SCR 3304×944, TEX 2800×800) that
 *   every layout constant in the pack is tuned against. */
inline double compute_lcd_aspect() {
    const double e0 = std::sqrt(
        (LCD_POS_L[1][0] - LCD_POS_L[0][0]) * (LCD_POS_L[1][0] - LCD_POS_L[0][0]) +
        (LCD_POS_L[1][1] - LCD_POS_L[0][1]) * (LCD_POS_L[1][1] - LCD_POS_L[0][1]) +
        (LCD_POS_L[1][2] - LCD_POS_L[0][2]) * (LCD_POS_L[1][2] - LCD_POS_L[0][2]));
    const double e1 = std::sqrt(
        (LCD_POS_L[2][0] - LCD_POS_L[1][0]) * (LCD_POS_L[2][0] - LCD_POS_L[1][0]) +
        (LCD_POS_L[2][1] - LCD_POS_L[1][1]) * (LCD_POS_L[2][1] - LCD_POS_L[1][1]) +
        (LCD_POS_L[2][2] - LCD_POS_L[1][2]) * (LCD_POS_L[2][2] - LCD_POS_L[1][2]));
    if (e0 <= 0 || e1 <= 0) return 3.5;
    return e0 > e1 ? e0 / e1 : e1 / e0;
}

/* Logical drawing size (TEX space) — layout math lives here. */
constexpr int TEX_W = 2800;
constexpr int TEX_H = 800;
constexpr double LAYOUT_K = TEX_H / 480.0;   /* ≈ 1.6667 */

/* ★ Intentional C++ optimization, documented deviation from the JS:
   the JS rasterizes at SCR (3304×944 per screen, per car, both sides —
   12.5 MB each). The native port keeps the identical logical layout
   but rasterizes at half device density (same quad, same UVs):
   4× less memory and upload bandwidth, visually indistinguishable on
   an in-game LCD. Set LCD_RASTER_SCALE to 1.0 for byte-parity with
   the JS texture size. */
#ifndef LCD_RASTER_SCALE
#define LCD_RASTER_SCALE 0.5
#endif

constexpr int SCR_W = 3304;
inline int scr_h_for_aspect(double aspect) { return static_cast<int>(std::lround(SCR_W / aspect)); }

/* Raster device size (texture allocation size). */
constexpr int RASTER_W = static_cast<int>(SCR_W * LCD_RASTER_SCALE + 0.5);
inline int raster_h_for_aspect(double aspect) {
    return static_cast<int>(std::lround(SCR_W / aspect * LCD_RASTER_SCALE));
}
inline double scale_x_for(double /*aspect*/) {
    return (SCR_W * LCD_RASTER_SCALE) / static_cast<double>(TEX_W);
}
inline double scale_y_for(double aspect) {
    return raster_h_for_aspect(aspect) / static_cast<double>(TEX_H);
}

/* ---- colors (config.js) ---- */
constexpr uint32_t LINE_COLOR   = mtr::clr(0, 155, 192);
constexpr uint32_t GRAY_COLOR   = mtr::clr(118, 130, 137);
constexpr uint32_t RED_COLOR    = mtr::clr(237, 28, 36);
constexpr uint32_t GREEN_COLOR  = mtr::clr(0, 200, 80);
constexpr uint32_t WHITE_COLOR  = 0xFFFFFFFFu;
constexpr uint32_t BLACK_COLOR  = 0xFF000000u;
constexpr uint32_t DOT_GRAY     = mtr::clr(180, 180, 180);
constexpr uint32_t DEEP_GRAY_DOT= mtr::clr(90, 100, 107);
constexpr uint32_t BLINK_RED    = mtr::clra(237, 28, 36, 255);
constexpr uint32_t BLINK_GREEN  = mtr::clra(0, 200, 80, 255);

/* ---- timing (config.js, ms) ---- */
constexpr int64_t CYCLE_FULL_MS    = 10000;
constexpr int64_t CYCLE_PARTIAL_MS = 10000;
constexpr int64_t BLINK_INTERVAL_MS = 1000;
constexpr int64_t STOP_DEBOUNCE_MS = 3000;

/* ---- vertical layout constants (config.js, LAYOUT_K-scaled) ---- */
constexpr int HEADER_H      = static_cast<int>(std::lround(130 * LAYOUT_K));  /* 217 */
constexpr int RING_TOP_Y    = static_cast<int>(std::lround(240 * LAYOUT_K));  /* 400 */
constexpr int RING_BOTTOM_Y = static_cast<int>(std::lround(390 * LAYOUT_K));  /* 650 */
constexpr int RING_LEFT_X   = 200;
constexpr int RING_RIGHT_X  = TEX_W - 200;
constexpr int RING_RADIUS   = (RING_BOTTOM_Y - RING_TOP_Y) / 2;               /* 125 */
constexpr int RING_STROKE   = static_cast<int>(std::lround(24 * LAYOUT_K));   /* 40 */
constexpr int RING_PADDING  = static_cast<int>(std::lround(60 * LAYOUT_K));   /* 100 */

/* K(x) — port of `Math.round(v * LAYOUT_K)` used all over the JS. */
inline int K(double v) { return static_cast<int>(std::lround(v * LAYOUT_K)); }

/* ==================== util.js (text) ==================== */

inline std::string get_non_extra_parts(const std::string& name) {
    const size_t idx = name.find("||");
    return idx == std::string::npos ? name : name.substr(0, idx);
}

inline bool is_interchange_route_kept(const std::string& name) {
    static const char* drop[] = {"巴士", "晋祠专线", "观光", "空中捷运", "快速直达专线", "公交"};
    for (const char* d : drop) if (name.find(d) != std::string::npos) return false;
    return true;
}

/* splitCjkNonCjk — TextUtil.getCjkParts/getNonCjkParts equivalent:
   split by script (CJK vs latin). The "|" separator is dropped on both
   sides (it only appears as the cn|en delimiter in MTR strings). */
inline void append_utf8(std::string& out, uint32_t cp) {
    char buf[5]; int32_t n = 0;
    if (cp < 0x80) { buf[n++] = static_cast<char>(cp); }
    else if (cp < 0x800) {
        buf[n++] = static_cast<char>(0xC0 | (cp >> 6));
        buf[n++] = static_cast<char>(0x80 | (cp & 0x3F));
    } else {
        buf[n++] = static_cast<char>(0xE0 | (cp >> 12));
        buf[n++] = static_cast<char>(0x80 | ((cp >> 6) & 0x3F));
        buf[n++] = static_cast<char>(0x80 | (cp & 0x3F));
    }
    out.append(buf, n);
}

inline void split_cjk_non_cjk(const std::string& full, std::string& cn, std::string& en) {
    cn.clear(); en.clear();
    if (full.empty()) return;

    mtr::Utf8Cursor c(full.c_str(), static_cast<int32_t>(full.size()));
    while (!c.done()) {
        const uint32_t cp = c.next();
        if (cp == '|') continue;   /* separator, not content */
        if (mtr::is_cjk_cp(cp)) append_utf8(cn, cp);
        else append_utf8(en, cp);
    }
    while (!en.empty() && en.front() == ' ') en.erase(en.begin());

    /* JS fallback: no CJK found and a "|" separator exists → split there. */
    if (cn.empty()) {
        const size_t bar = full.find('|');
        if (bar != std::string::npos) {
            cn = full.substr(0, bar);
            en = full.substr(bar + 1);
            while (!en.empty() && en.front() == ' ') en.erase(en.begin());
            while (!cn.empty() && cn.back() == ' ') cn.pop_back();
        } else {
            cn = full;   /* no CJK, no separator: whole string is cn */
            en.clear();
        }
    }
}

/* ==================== train_num_util.js ==================== */

struct SidingInfo {
    std::string vehicleNum;
    std::vector<std::string> carNums;
    std::string formation;
    std::string extra;
};

inline std::vector<std::string> split_str(const std::string& s, char sep) {
    std::vector<std::string> out;
    size_t start = 0;
    for (size_t i = 0; i <= s.size(); i++) {
        if (i == s.size() || s[i] == sep) {
            std::string part = s.substr(start, i - start);
            /* trim spaces */
            const size_t a = part.find_first_not_of(' ');
            const size_t b = part.find_last_not_of(' ');
            out.push_back(a == std::string::npos ? "" : part.substr(a, b - a + 1));
            start = i + 1;
        }
    }
    return out;
}

inline SidingInfo parse_siding_name(const std::string& sidingName) {
    SidingInfo result;
    if (sidingName.empty()) return result;

    const size_t slash = sidingName.find('/');
    if (slash != std::string::npos) {
        result.vehicleNum = sidingName.substr(0, slash);
        const size_t a = result.vehicleNum.find_first_not_of(' ');
        const size_t b = result.vehicleNum.find_last_not_of(' ');
        result.vehicleNum = a == std::string::npos ? "" : result.vehicleNum.substr(a, b - a + 1);
        result.carNums = split_str(sidingName.substr(slash + 1), '-');
        return result;
    }

    /* "车号 编组 车厢" e.g. "10010 Tc1 A1-1" */
    std::vector<std::string> parts = split_str(sidingName, ' ');
    parts.erase(std::remove_if(parts.begin(), parts.end(),
        [](const std::string& p) { return p.empty(); }), parts.end());
    if (parts.size() >= 3) {
        result.vehicleNum = parts[0];
        result.formation = parts[1];
        result.extra = parts[2];
        for (char ch : result.extra) {
            if (ch >= '0' && ch <= '9') result.carNums.push_back(std::string(1, ch));
        }
        return result;
    }

    result.carNums = split_str(sidingName, '-');
    return result;
}

/* getCarDisplayNum — "10010 01" style per-car label for the LCD header. */
inline std::string get_car_display_num(const std::string& sidingName, int carIndex) {
    const SidingInfo parsed = parse_siding_name(sidingName);
    if (parsed.carNums.empty() || carIndex < 0 || carIndex >= static_cast<int>(parsed.carNums.size())) {
        return parsed.vehicleNum;
    }
    const std::string carName = parsed.carNums[carIndex];
    std::string suffix = carName;
    if (!parsed.vehicleNum.empty() && carName.rfind(parsed.vehicleNum, 0) == 0) {
        suffix = carName.substr(parsed.vehicleNum.size());
    }
    if (!parsed.vehicleNum.empty()) return parsed.vehicleNum + " " + suffix;
    return suffix;
}

/* getVehicleDisplayNum — "10010 Tc1" for the head/tail plates. */
inline std::string get_vehicle_display_num(const std::string& sidingName) {
    const SidingInfo parsed = parse_siding_name(sidingName);
    if (!parsed.formation.empty()) return parsed.vehicleNum + " " + parsed.formation;
    if (!parsed.vehicleNum.empty() && !parsed.carNums.empty()) {
        const std::string first = parsed.carNums[0];
        if (first.rfind(parsed.vehicleNum, 0) == 0) {
            const std::string suffix = first.substr(parsed.vehicleNum.size());
            if (!suffix.empty()) return parsed.vehicleNum + " " + suffix;
        }
    }
    return parsed.vehicleNum.empty() ? sidingName : parsed.vehicleNum;
}

/* ==================== data.js ==================== */

struct Transfer {
    std::string name;
    uint32_t color = 0xFF646464u;
};

struct StationInfo {
    std::string id;
    std::string nameCn, nameEn;
    std::vector<Transfer> transfers;
    /* exits: the v2 ABI does not carry station exits; the JS draws the
       exit panel only when the station exposes them, so the port keeps
       an empty list (JS: `if (!exits || exits.length === 0) return;`). */
};

inline std::string get_station_id_str(const mtr::Stop& stop) {
    if (stop.station_id() != 0) return "s" + std::to_string(stop.station_id());
    if (stop.platform_id() != 0) return "p" + std::to_string(stop.platform_id());
    return "";
}

/* getStationsFromStops — data.js, reading the flat snapshot. */
inline std::vector<StationInfo> get_stations_from_stops(const mtr::StopList& stops,
                                                        const std::string& thisRouteName) {
    std::vector<StationInfo> stations;
    const int32_t n = stops.size();
    stations.reserve(n > 0 ? static_cast<size_t>(n) : 0);
    for (int32_t i = 0; i < n; i++) {
        const mtr::Stop stop = stops.at(i);
        StationInfo st;
        std::string fullName(stop.name().str());
        split_cjk_non_cjk(fullName, st.nameCn, st.nameEn);
        if (st.nameCn.empty() && !fullName.empty()) st.nameCn = fullName;
        st.id = get_station_id_str(stop);

        const int32_t ic = stop.interchange_count();
        for (int32_t j = 0; j < ic; j++) {
            const auto ir = stop.interchange(j);
            const std::string irName(ir.name.str());
            if (irName.empty()) continue;
            if (!is_interchange_route_kept(irName)) continue;
            if (get_non_extra_parts(irName) == thisRouteName) continue;
            Transfer t;
            t.name = irName;
            t.color = static_cast<uint32_t>(ir.color) | 0xFF000000u;
            st.transfers.push_back(t);
        }
        stations.push_back(std::move(st));
    }

    /* circular-line dedupe: drop the repeated terminal station */
    if (stations.size() > 1) {
        const StationInfo& first = stations.front();
        const StationInfo& last = stations.back();
        const bool sameId = !first.id.empty() && first.id == last.id;
        const bool sameName = first.nameCn == last.nameCn && first.nameEn == last.nameEn
            && !first.nameCn.empty();
        if (sameId || sameName) stations.pop_back();
    }
    return stations;
}

/* ---- transfer route parse / sort (data.js) ---- */

enum TransferType { TRANSFER_NUMBER = 0, TRANSFER_LETTER_NUMBER = 1, TRANSFER_CHINESE = 2 };

struct ParsedTransferRoute {
    std::string id, cn, en, letter;
    long num = 0;
    int type = TRANSFER_CHINESE;
    uint32_t color = 0xFF646464u;
};

inline ParsedTransferRoute parse_transfer_route(const std::string& name) {
    ParsedTransferRoute out;
    const std::string clean = get_non_extra_parts(name);
    const size_t bar = clean.find('|');
    if (bar != std::string::npos) {
        out.cn = clean.substr(0, bar);
        out.en = clean.substr(bar + 1);
        /* trims */
        const size_t a = out.cn.find_first_not_of(' ');
        const size_t b = out.cn.find_last_not_of(' ');
        out.cn = a == std::string::npos ? "" : out.cn.substr(a, b - a + 1);
        const size_t a2 = out.en.find_first_not_of(' ');
        const size_t b2 = out.en.find_last_not_of(' ');
        out.en = a2 == std::string::npos ? "" : out.en.substr(a2, b2 - a2 + 1);
    } else {
        split_cjk_non_cjk(clean, out.cn, out.en);
    }
    if (out.cn.empty() && !clean.empty()) out.cn = clean;

    /* ^([A-Za-z]+)?(\d+) on the cn part */
    size_t i = 0;
    while (i < out.cn.size() && std::isalpha(static_cast<unsigned char>(out.cn[i]))) i++;
    const std::string letter = out.cn.substr(0, i);
    size_t j = i;
    while (j < out.cn.size() && std::isdigit(static_cast<unsigned char>(out.cn[j]))) j++;
    if (j > i) {
        const std::string digits = out.cn.substr(i, j - i);
        out.num = std::atol(digits.c_str());
        out.id = letter + digits;
        out.letter = letter;
        out.type = letter.empty() ? TRANSFER_NUMBER : TRANSFER_LETTER_NUMBER;
    } else {
        out.id = out.cn;
        out.letter.clear();
        out.num = 0;
        out.type = TRANSFER_CHINESE;
    }
    return out;
}

/* getChinesePinyinInitial — data.js table. */
inline char pinyin_initial(uint32_t cp) {
    struct Ent { uint32_t cp; char py; };
    static const Ent map[] = {
        {0x4E00,'Y'},{0x4E8C,'E'},{0x4E09,'S'},{0x56DB,'S'},{0x4E94,'W'},
        {0x516D,'L'},{0x4E03,'Q'},{0x516B,'B'},{0x4E5D,'J'},{0x5341,'S'},
        {0x4E1C,'D'},{0x897F,'X'},{0x5357,'N'},{0x5317,'B'},{0x4E2D,'Z'},
        {0x4E0A,'S'},{0x4E0B,'X'},{0x5927,'D'},{0x5C0F,'X'},{0x65B0,'X'},
        {0x8001,'L'},{0x9AD8,'G'},{0x4F4E,'D'},{0x5FEB,'K'},{0x901F,'S'},
        {0x6162,'M'},{0x673A,'J'},{0x573A,'C'},{0x8F66,'C'},{0x7AD9,'Z'},
        {0x516C,'G'},{0x4EA4,'J'},{0x5DF4,'B'},{0x58EB,'S'},{0x89C2,'G'},
        {0x5149,'G'},{0x7A7A,'K'},{0x6377,'J'},{0x8FD0,'Y'},{0x5730,'D'},
        {0x94C1,'T'},{0x53F7,'H'},{0x7EBF,'X'},{0x652F,'Z'},{0x5EF6,'Y'},
        {0x957F,'C'},{0x73AF,'H'},{0x5185,'N'},{0x5916,'W'},{0x9996,'S'},
        {0x90FD,'D'},{0x56FD,'G'},{0x9645,'J'},{0x56ED,'Y'},{0x5C71,'S'},
        {0x6CB3,'H'},{0x6E56,'H'},{0x6C5F,'J'},{0x6D77,'H'},{0x6E7E,'W'},
        {0x6865,'Q'},{0x8DEF,'L'},{0x8857,'J'},{0x95E8,'M'},{0x53E3,'K'}
    };
    for (const Ent& e : map) if (e.cp == cp) return e.py;
    return '\x7F';
}

inline int compare_transfer_route(const ParsedTransferRoute& a, const ParsedTransferRoute& b) {
    if (a.type != b.type) return a.type < b.type ? -1 : 1;
    if (a.type == TRANSFER_NUMBER) return a.num < b.num ? -1 : (a.num > b.num ? 1 : 0);
    if (a.type == TRANSFER_LETTER_NUMBER) {
        if (a.letter != b.letter) return a.letter < b.letter ? -1 : 1;
        return a.num < b.num ? -1 : (a.num > b.num ? 1 : 0);
    }
    const uint32_t cpa = !a.cn.empty() ? mtr::Utf8Cursor(a.cn.c_str(), -1).next() : 0;
    const uint32_t cpb = !b.cn.empty() ? mtr::Utf8Cursor(b.cn.c_str(), -1).next() : 0;
    const char pa = pinyin_initial(cpa), pb = pinyin_initial(cpb);
    if (pa != pb) return pa < pb ? -1 : 1;
    return a.cn < b.cn ? -1 : (a.cn > b.cn ? 1 : 0);
}

/* normalizeLineId — draw_common.js ("10号线"→"10", "S1线"→"S1", 中文去后缀). */
inline std::string normalize_line_id(const std::string& name) {
    if (name.empty()) return "";
    std::string clean = get_non_extra_parts(name);
    const size_t bar = clean.find('|');
    if (bar != std::string::npos) clean = clean.substr(0, bar);
    /* trim */
    const size_t a = clean.find_first_not_of(' ');
    const size_t b = clean.find_last_not_of(' ');
    clean = a == std::string::npos ? "" : clean.substr(a, b - a + 1);

    /* ^([A-Za-z]*\d+) */
    size_t i = 0;
    while (i < clean.size() && std::isalpha(static_cast<unsigned char>(clean[i]))) i++;
    size_t j = i;
    while (j < clean.size() && std::isdigit(static_cast<unsigned char>(clean[j]))) j++;
    if (j > i) return clean.substr(0, j);

    /* 中文线路：去掉末尾 捷运 / 号线 / 线 */
    auto strip_suffix = [](std::string& s, const char* suf) {
        const size_t n = std::strlen(suf);
        if (s.size() >= n && s.compare(s.size() - n, n, suf) == 0) s.erase(s.size() - n);
    };
    std::string cn = clean;
    strip_suffix(cn, "捷运");
    strip_suffix(cn, "号线");
    strip_suffix(cn, "线");
    const size_t a2 = cn.find_first_not_of(' ');
    const size_t b2 = cn.find_last_not_of(' ');
    cn = a2 == std::string::npos ? "" : cn.substr(a2, b2 - a2 + 1);
    return cn.empty() ? clean : cn;
}

/* ==================== circular.js ==================== */

/**
 * resolveCircularState — port of circular.js.
 *  (i)  v2 snapshot route circular_state (host marshalled from
 *       SimplifiedRoute.getCircularState(), the same source the JS reads).
 *       CLOCKWISE/ANTICLOCKWISE ⇒ 环线, and the result is LOCKED for the
 *       instance lifetime (JS `_isCircularLocked`).
 *  (ii) without a positive (i): heuristic over the full stop list —
 *       if the train's stop list revisits the current route's platforms
 *       (a loop), treat as circular without locking. (The JS walks
 *       getStops().get(i).route.getCircularState(); the v2 ABI carries
 *       the current route's state only, so the loop-shape of the stop
 *       list stands in for the per-route probe.)
 */
struct CircularResult {
    bool isCircular = false;
    int circularState = 0;   /* 1 clockwise, 2 anticlockwise */
};

inline CircularResult resolve_circular_state(const mtr::Train& train,
                                             bool& lockedFlag, int& storedState) {
    CircularResult result;
    if (lockedFlag) {
        result.isCircular = true;
        result.circularState = storedState;
        return result;
    }
    storedState = 0;
    const uint8_t cs = train.circular_state();
    if (cs == 1 || cs == 2) {
        result.isCircular = true;
        result.circularState = cs;
        lockedFlag = true;          /* 方式 (i)：命中后锁定 */
        storedState = cs;
        return result;
    }
    /* 方式 (ii)：stop list loops back onto this route ⇒ 环线（不锁定） */
    const mtr::StopList all = train.stops();
    const int64_t thisRoute = train.this_route_id();
    int seen = 0;
    for (int32_t i = 0; i < all.size(); i++) {
        if (all.at(i).route_id() == thisRoute) {
            if (++seen >= 2) {
                result.isCircular = true;
                result.circularState = 2;   /* direction unknown ⇒ 外环文案 */
                storedState = 2;
                return result;
            }
        }
    }
    return result;
}

/* ==================== mtr_util.js ==================== */

enum TrainStatus {
    STATUS_NO_ROUTE = 0,
    STATUS_WAITING_FOR_DEPARTURE,
    STATUS_LEAVING_DEPOT,
    STATUS_ON_ROUTE,
    STATUS_ARRIVED,
    STATUS_CHANGING_ROUTE,
    STATUS_RETURNING_TO_DEPOT
};

inline TrainStatus get_train_status(const mtr::Train& train) {
    const mtr::StopList all = train.stops();
    const mtr::StopList thisRoute = train.this_route_stops();
    if (all.size() == 0) return STATUS_NO_ROUTE;
    if (!train.on_route()) return STATUS_WAITING_FOR_DEPARTURE;
    const int32_t nextIdx = train.next_stop_index();
    if (nextIdx >= all.size()) return STATUS_RETURNING_TO_DEPOT;
    if (train.rail_progress() == all.at(nextIdx).distance()) return STATUS_ARRIVED;
    if (nextIdx == 0) return STATUS_LEAVING_DEPOT;
    if (thisRoute.size() > 0 && nextIdx >= thisRoute.size()) return STATUS_CHANGING_ROUTE;
    return STATUS_ON_ROUTE;
}

/* ==================== util.js (vehicle state) ==================== */

/* getTravelLeftSide — 模型侧 ↔ 行驶方向左侧（isReversed 翻转）。 */
constexpr const char* TRAVEL_LEFT_MODEL_SIDE_WHEN_FORWARD = "L";

inline void get_travel_left_side(const mtr::Train& train, char& side, bool& reversed) {
    reversed = train.reversed();
    const bool forwardLeftIsR = (TRAVEL_LEFT_MODEL_SIDE_WHEN_FORWARD[0] == 'R');
    const bool forwardLeftIsL = !forwardLeftIsR;
    const bool nowLeftIsR = reversed ? forwardLeftIsL : forwardLeftIsR;
    side = nowLeftIsR ? 'R' : 'L';
}

inline double vehicle_speed_ms(const mtr::Train& train) {
    return std::fabs(train.speed_ms());
}

inline double vehicle_door_value(const mtr::Train& train) {
    return std::max(0.0, std::min(1.0, train.door_value()));
}

} /* namespace jslcd */
