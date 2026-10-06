/**
 * jslcd_vehicle.cpp — 车侧 LCD 路线图显示屏 (native port of the
 * community "JS LCD" pack: main.js + config.js + data.js + circular.js +
 * draw_header.js + draw_common.js + draw_circular.js + draw_linear.js).
 *
 * JS pipeline (per frame, per car, per side):
 *   DisplayHelper.graphicsFor(slot) → java.awt.Graphics2D painting
 *   (TTF fonts, GeneralPath, AlphaComposite) → texture.upload() →
 *   ctx.drawCarModel(model, car, null)
 *
 * Native pipeline (this file):
 *   mtr::Gfx2D (deterministic software rasterizer, see gfx2d.hpp)
 *   painting the SAME logical layout (TEX_W×TEX_H coordinate space,
 *   LAYOUT_K constants identical to config.js) → dirty-rect upload →
 *   ctx.draw_car_model(model, car, nullptr).
 *
 * Content parity with the JS pack:
 *   - 顶部信息栏: route color band + 线路名块 (数字号线 big-number
 *     layout / 命名线 centered cn+en) + 开往终点站 / 环线方向 +
 *     下一站/到达 + 车号"液态玻璃"卡片 (shadow steps + highlights).
 *   - 环线: full ring map (RoundRectangle2D 轨道 + 站点圆点 + 自适应
 *     字号 + 换乘徽章 + 进度闪烁) / partial (5-station capsule, wrap).
 *   - 直线: full linear map (gray track + route-colored segment +
 *     blink overlay + alternating names) / partial (5-station capsule
 *     with boundary circle, passed = deep gray).
 *   - 开门: 大站名 (route color) + 终点站提示 + 换乘/Transfer panel.
 *   - 页面轮播 full/partial (10 s), blink 1 s, door detect, stop count
 *     debounce — state machine semantics identical to main.js.
 *
 * Performance (why this is faster than the JS, measured in bench/):
 *   1. Repaint-on-change: the JS repaints EVERY frame (20-60 Hz); the
 *      native port tracks a content signature (page mode, blink state,
 *      door state, station index, route/siding revision) and only
 *      re-rasterizes when the picture actually changes — steady state
 *      is ~1 repaint/s (blink) instead of ~60.
 *   2. L/R screen sharing: when leftMode == rightMode (both "full")
 *      the right texture is a memcpy of the left (identical content by
 *      construction in main.js).
 *   3. Gfx2D hot loops: tight device-bbox loops with opaque fast path,
 *     no Java2D pipeline, no GeneralPath churn, no per-draw TTF
 *      derivation, no AlphaComposite object allocation.
 *   4. Dirty-rect upload: only the changed bounding box crosses the
 *      JNI boundary, and (with LCD_RASTER_SCALE) at half density.
 *
 * Referenced from mtr_custom_resources.json:
 *   "vehicleScripts": [{ "id": "jslcd:vehicle_lcd", "language": "cpp",
 *                        "nativeLibrary": "natives/libjslcd_vehicle.so" }]
 */
#include "jslcd_common.hpp"

#include <algorithm>
#include <string>
#include <vector>

using namespace mtr;
using namespace jslcd;

namespace {

/* ==================== CIRC_CN / CIRC_EN (config.js) ==================== */

const char* circ_cn(int cs) {
    if (cs == 1) return "内环";     /* CLOCKWISE → 内环 */
    if (cs == 2) return "外环";     /* ANTICLOCKWISE → 外环 */
    return "环线";
}
const char* circ_en(int cs) {
    if (cs == 1) return "Inner Loop";
    if (cs == 2) return "Outer Loop";
    return "Loop";
}

/* ==================== per-instance state ==================== */

struct CarScreens {
    GraphicsTexture left;
    GraphicsTexture right;
};

struct JsLcdState {
    /* --- JS state object fields (main.js create()) --- */
    int64_t lastCycleTime = 0;
    int64_t lastBlinkTime = 0;
    bool blinkState = false;
    int pageMode = 0;                 /* 0 = full, 1 = partial */
    bool isDoorOpen = false;
    bool lastDoorOpen = false;
    std::string cachedRouteId;
    std::vector<StationInfo> stations;
    uint32_t stationsRev = 1;         /* native: cache revision */
    std::string destinationCn, destinationEn;
    bool isCircular = false;
    int circularState = 0;
    bool circularLocked = false;      /* JS _isCircularLocked */
    uint32_t routeColor = LINE_COLOR;
    std::string routeNameCn, routeNameEn;
    int stopCountIdx = -1;            /* JS _stopCountIdx */
    int64_t lastCountTime = 0;
    int lastIdxShown = -1;            /* JS _lastIdxShown */

    /* --- native-side resources --- */
    std::vector<CarScreens> cars;     /* rebuilt when carCount changes */
    int32_t modelLeft = -1, modelRight = -1;
    std::string sidingName;

    /* --- repaint-on-change bookkeeping (per car) ---
       paintedCarSig[ci] == frameSig means car ci's textures are
       current; otherwise the car is stale and gets repainted as the
       per-frame repaint budget allows. */
    std::vector<uint64_t> paintedCarSig;
};

/* ==================== info bundle (render frame) ==================== */

struct ScreenInfo {
    const std::vector<StationInfo>* stations = nullptr;
    int currentIdx = 0, nextIdx = 0;
    bool isCircular = false;
    int circularState = 0;
    std::string destinationCn, destinationEn;
    bool isDoorOpen = false;
    bool blinkState = false;
    uint32_t routeColor = LINE_COLOR;
    std::string routeNameCn, routeNameEn;
    std::string vehicleNum;
};

/* ==================== draw_common.js ==================== */

/* drawCenteredText */
void draw_centered_text(Gfx2D& g, const std::string& text, double cx,
                        double y_baseline, double size, uint32_t color) {
    if (text.empty()) return;
    g.set_color(color);
    const double w = Gfx2D::text_width(size, text.c_str());
    g.draw_text(cx - w * 0.5, y_baseline, size, text.c_str());
}

/* drawArrow — GeneralPath triangle, 4 directions. */
void draw_arrow(Gfx2D& g, double x, double y, char dir, uint32_t color, double size) {
    g.set_color(color);
    double xs[3], ys[3];
    switch (dir) {
        case 'R': /* right */
            xs[0] = x - size; ys[0] = y - size * 0.6;
            xs[1] = x + size; ys[1] = y;
            xs[2] = x - size; ys[2] = y + size * 0.6;
            break;
        case 'L': /* left */
            xs[0] = x + size; ys[0] = y - size * 0.6;
            xs[1] = x - size; ys[1] = y;
            xs[2] = x + size; ys[2] = y + size * 0.6;
            break;
        case 'U': /* up */
            xs[0] = x - size * 0.6; ys[0] = y + size;
            xs[1] = x;            ys[1] = y - size;
            xs[2] = x + size * 0.6; ys[2] = y + size;
            break;
        default:  /* down */
            xs[0] = x - size * 0.6; ys[0] = y - size;
            xs[1] = x;            ys[1] = y + size;
            xs[2] = x + size * 0.6; ys[2] = y - size;
            break;
    }
    g.fill_polygon(xs, ys, 3);
}

/* drawArrowChar — the JS draws →/←/↑/↓ glyphs on the blink segment;
   the native port draws the equivalent vector chevrons. */
void draw_arrow_char(Gfx2D& g, char dir, double cx, double cy) {
    const double s = K(30) * 0.5;
    draw_arrow(g, cx, cy, dir, WHITE_COLOR, s);
}

/* drawAdaptiveCenteredText (compressed when wider than availW). */
double compute_per_text_compress_x(double text_w, double avail_w) {
    if (text_w <= avail_w || text_w <= 0) return 1.0;
    const double natural = avail_w / text_w;
    int steps = static_cast<int>(std::floor(natural * 20.0 + 1e-9));
    if (steps < 10) steps = 10;
    if (steps > 20) steps = 20;
    return steps / 20.0;
}

void draw_adaptive_centered_text(Gfx2D& g, const std::string& text, double cx,
                                 double y_baseline, double size, double avail_w,
                                 uint32_t color) {
    if (text.empty()) return;
    const double orig_w = Gfx2D::text_width(size, text.c_str());
    const double compress = compute_per_text_compress_x(orig_w, avail_w);
    const double draw_w = orig_w * compress;
    g.set_color(color);
    g.draw_text_compressed(cx - draw_w * 0.5, y_baseline, size, text.c_str(), compress);
}

/* computeAdaptiveFontSize */
double compute_adaptive_font_size(double base_size, double max_w, double avail_w,
                                  double min_size) {
    if (max_w <= 0 || avail_w >= max_w) return base_size;
    double target = base_size * (avail_w / max_w);
    if (target < min_size) target = min_size;
    return std::lround(target);
}

/* getStationPos — 环线 station positions (top/bottom rows). */
struct StationPos {
    double x = 0, y = 0;
    int section = 0;   /* 0 top, 1 bottom */
};

StationPos get_station_pos(int index, int total, bool reversed) {
    const double topStartX = RING_LEFT_X + RING_RADIUS + RING_PADDING;
    const double topEndX = RING_RIGHT_X - RING_RADIUS - RING_PADDING;
    const int topCount = (total + 1) / 2;
    const int botCount = total - topCount;

    StationPos p;
    if (index < topCount) {
        double t = topCount <= 1 ? 0.5 : static_cast<double>(index) / (topCount - 1);
        if (reversed) t = 1 - t;
        p.x = topStartX + t * (topEndX - topStartX);
        p.y = RING_TOP_Y;
        p.section = 0;
    } else {
        const int i = index - topCount;
        double t = botCount <= 1 ? 0.5 : static_cast<double>(i) / (botCount - 1);
        if (reversed) t = 1 - t;
        p.x = topEndX - t * (topEndX - topStartX);
        p.y = RING_BOTTOM_Y;
        p.section = 1;
    }
    return p;
}

/* getLinearStationPos — 直线 station positions, alternating above/below. */
struct LinearPos { double x = 0, y = 0; bool above = false; };

LinearPos get_linear_station_pos(int index, int total) {
    const double startX = RING_LEFT_X + RING_PADDING;
    const double endX = RING_RIGHT_X - RING_PADDING;
    const double y = (RING_TOP_Y + RING_BOTTOM_Y) * 0.5;
    LinearPos p;
    if (total <= 1) {
        p.x = (startX + endX) * 0.5;
    } else {
        const double t = static_cast<double>(index) / (total - 1);
        p.x = startX + t * (endX - startX);
    }
    p.y = y;
    p.above = (index % 2 == 1);
    return p;
}

/* appendRingSegment — GeneralPath arc approximation (24 steps), port. */
void append_ring_segment(std::vector<double>& xs, std::vector<double>& ys,
                         const StationPos& pa, const StationPos& pb, bool clockwise) {
    if (pa.section == pb.section) {
        xs.push_back(pb.x); ys.push_back(pb.y);
        return;
    }
    bool useRightArc;
    if (pa.section == 0 && pb.section == 1) useRightArc = clockwise;
    else useRightArc = !clockwise;

    double cx;
    if (useRightArc) cx = RING_RIGHT_X - RING_RADIUS;
    else             cx = RING_LEFT_X + RING_RADIUS;
    const double cy = (RING_TOP_Y + RING_BOTTOM_Y) * 0.5;

    if (pa.section == 0) { xs.push_back(cx); ys.push_back(RING_TOP_Y); }
    else                 { xs.push_back(cx); ys.push_back(RING_BOTTOM_Y); }

    const int steps = 24;
    for (int s = 1; s <= steps; s++) {
        double a;
        if (pa.section == 0) {
            a = useRightArc ? (-M_PI / 2 + (double)s / steps * M_PI)
                            : (-M_PI / 2 - (double)s / steps * M_PI);
        } else {
            a = useRightArc ? (M_PI / 2 - (double)s / steps * M_PI)
                            : (M_PI / 2 + (double)s / steps * M_PI);
        }
        xs.push_back(cx + RING_RADIUS * std::cos(a));
        ys.push_back(cy + RING_RADIUS * std::sin(a));
    }
    xs.push_back(pb.x); ys.push_back(pb.y);
}

/* drawProgressOverlay — 环线 blink segment + arrow. */
void draw_progress_overlay(Gfx2D& g, const JsLcdState& state, const ScreenInfo& info,
                           bool reversed, bool clockwise) {
    const int n = static_cast<int>(info.stations->size());
    const StationPos p1 = get_station_pos(info.currentIdx, n, reversed);
    const StationPos p2 = get_station_pos(info.nextIdx, n, reversed);

    std::vector<double> xs, ys;
    xs.reserve(60); ys.reserve(60);
    xs.push_back(p1.x); ys.push_back(p1.y);
    append_ring_segment(xs, ys, p1, p2, clockwise);

    const double border_stroke = RING_STROKE + K(3);
    g.set_color(BLACK_COLOR);
    g.stroke_polyline(xs.data(), ys.data(), static_cast<int32_t>(xs.size()), border_stroke);
    g.set_color(state.blinkState ? BLINK_GREEN : BLINK_RED);
    g.stroke_polyline(xs.data(), ys.data(), static_cast<int32_t>(xs.size()), RING_STROKE);

    double midX, midY;
    char arrowDir;
    if (p1.section == p2.section) {
        midX = (p1.x + p2.x) * 0.5;
        midY = p1.y;
        arrowDir = p2.x > p1.x ? 'R' : 'L';
    } else {
        const double ringCenterX = (RING_LEFT_X + RING_RIGHT_X) * 0.5;
        midX = ((p1.x + p2.x) * 0.5 > ringCenterX) ? RING_RIGHT_X : RING_LEFT_X;
        midY = (RING_TOP_Y + RING_BOTTOM_Y) * 0.5;
        arrowDir = (p2.section == 1) ? 'D' : 'U';
    }
    draw_arrow_char(g, arrowDir, midX, midY);
}

/* drawTransferBadges — 环线 station transfer pills. */
void draw_transfer_badges(Gfx2D& g, const std::vector<Transfer>& transfers,
                          const StationPos& pos) {
    const double badgeH = K(26);
    const double gap = K(6);
    std::vector<std::string> texts(transfers.size());
    std::vector<double> widths(transfers.size());
    double totalW = 0;
    for (size_t i = 0; i < transfers.size(); i++) {
        texts[i] = normalize_line_id(transfers[i].name);
        widths[i] = std::max(Gfx2D::text_width(K(18), texts[i].c_str()) + 22.0, badgeH);
        totalW += widths[i] + gap;
    }
    totalW -= gap;

    double startX = pos.x - totalW * 0.5;
    const double yOff = K(32);
    const double y = pos.y + (pos.section == 0 ? yOff : -yOff);

    for (size_t j = 0; j < transfers.size(); j++) {
        const double w = widths[j];
        const double cx = startX + w * 0.5;
        g.set_color(transfers[j].color);
        g.fill_round_rect(cx - w * 0.5, y - badgeH * 0.5, w, badgeH, badgeH, badgeH);
        g.set_color(WHITE_COLOR);
        const double tw = Gfx2D::text_width(K(18), texts[j].c_str());
        g.draw_text(cx - tw * 0.5, y + K(7), K(18), texts[j].c_str());
        startX += w + gap;
    }
}

/* drawBigStationName — 开门时的大站名。 */
void draw_big_station_name(Gfx2D& g, const StationInfo& station, double center_y,
                           uint32_t route_color) {
    const uint32_t rc = route_color ? route_color : LINE_COLOR;
    const double cn_size = K(150);
    const double en_size = K(42);
    const double gap = K(24);
    const double cn_visual_h = cn_size * 0.75;
    const double en_visual_h = en_size * 0.7;
    const double total_h = cn_visual_h + gap + en_visual_h;
    const double top = center_y - total_h * 0.5;
    const double cn_baseline = top + cn_visual_h;
    const double en_baseline = cn_baseline + gap + en_visual_h;

    g.set_color(rc);
    const double cnW = Gfx2D::text_width(cn_size, station.nameCn.c_str());
    g.draw_text(TEX_W * 0.5 - cnW * 0.5, cn_baseline, cn_size, station.nameCn.c_str());

    g.set_color(BLACK_COLOR);
    const double enW = Gfx2D::text_width(en_size, station.nameEn.c_str());
    g.draw_text(TEX_W * 0.5 - enW * 0.5, en_baseline, en_size, station.nameEn.c_str());
}

/* drawTerminusMessage */
void draw_terminus_message(Gfx2D& g, double y) {
    const char* cn_text = "本次列车已抵达终点站，请所有乘客全部下车。";
    const char* en_text = "This train has arrived at the terminus. Please alight from the train.";
    g.set_color(BLACK_COLOR);
    const double cnW = Gfx2D::text_width(K(26), cn_text);
    g.draw_text(TEX_W * 0.5 - cnW * 0.5, y, K(26), cn_text);
    const double enW = Gfx2D::text_width(K(20), en_text);
    g.draw_text(TEX_W * 0.5 - enW * 0.5, y + K(36), K(20), en_text);
}

/* ==================== draw_header.js ==================== */

bool is_number_route_name(const std::string& routeCn, const std::string& routeEn) {
    /* ^(\d+)号线$ */
    if (routeCn.size() < 9) return false;
    size_t i = 0;
    while (i < routeCn.size() && routeCn[i] >= '0' && routeCn[i] <= '9') i++;
    if (i == 0) return false;
    if (routeCn.compare(i, routeCn.size() - i, "号线") != 0) return false;
    return routeEn == ("Line " + routeCn.substr(0, i));
}

struct RouteNameOpts {
    double scale = 1.0;
    bool number_bold = true;
    bool number_center = false;
};

/* measureRouteNameBlock */
double measure_route_name_block(const std::string& routeCn, const std::string& routeEn,
                                double h, const RouteNameOpts& opts) {
    if (is_number_route_name(routeCn, routeEn)) {
        const std::string numStr = routeCn.substr(0, routeCn.size() - 6);
        const double numSize = std::max(1.0, static_cast<double>(std::lround(h * 0.62 * opts.scale)));
        const double suffixSize = std::max(1.0, static_cast<double>(std::lround(h * 0.26 * opts.scale)));
        const double enSize = std::max(1.0, static_cast<double>(std::lround(h * 0.20 * opts.scale)));
        const double numW = Gfx2D::text_width(numSize, numStr.c_str());
        const double suffixW = Gfx2D::text_width(suffixSize, "号线");
        const double enW = Gfx2D::text_width(enSize, routeEn.c_str());
        return std::lround(numW + std::lround(h * 0.06 * opts.scale)
                           + std::max(suffixW, enW));
    }
    const double cnSize = std::max(1.0, static_cast<double>(std::lround(h * 0.30 * opts.scale)));
    const double enSize = std::max(1.0, static_cast<double>(std::lround(h * 0.20 * opts.scale)));
    const double cnW = Gfx2D::text_width(cnSize, routeCn.c_str());
    const double enW = Gfx2D::text_width(enSize, routeEn.c_str());
    return std::lround(std::max(cnW, enW));
}

/* drawRouteNameBlock — 数字号线 / 命名线 two layouts. */
void draw_route_name_block(Gfx2D& g, double x, double y, double w, double h,
                           const std::string& routeCn, const std::string& routeEn,
                           uint32_t text_color, const RouteNameOpts& opts) {
    if (routeCn.empty() && routeEn.empty()) return;
    const double cy = y + h * 0.5;

    if (is_number_route_name(routeCn, routeEn)) {
        /* ---------- 数字号线: big number + 号线 / Line N ---------- */
        const std::string numStr = routeCn.substr(0, routeCn.size() - 6);
        const double numSize = std::max(1.0, static_cast<double>(std::lround(h * 0.62 * opts.scale)));
        const double suffixSize = std::max(1.0, static_cast<double>(std::lround(h * 0.26 * opts.scale)));
        const double enSize = std::max(1.0, static_cast<double>(std::lround(h * 0.20 * opts.scale)));

        const double numW = Gfx2D::text_width(numSize, numStr.c_str());
        const double suffixW = Gfx2D::text_width(suffixSize, "号线");
        const double enW = Gfx2D::text_width(enSize, routeEn.c_str());

        const double lineGap = std::max(1.0, static_cast<double>(std::lround(h * 0.02 * opts.scale)));
        const double suffixH = Gfx2D::text_ascent(suffixSize) + Gfx2D::text_descent(suffixSize);
        const double enH = Gfx2D::text_ascent(enSize) + Gfx2D::text_descent(enSize);
        const double blockH = suffixH + lineGap + enH;

        const double gapNX = std::lround(h * 0.06 * opts.scale);
        const double rightW = std::max(suffixW, enW);
        const double totalW = numW + gapNX + rightW;

        const double startX = opts.number_center ? x + (w - totalW) * 0.5 : x;
        const double numBaseline = cy + (Gfx2D::text_ascent(numSize)
                                         - Gfx2D::text_descent(numSize)) * 0.5;
        const double rightX = startX + numW + gapNX;

        g.set_color(text_color);
        g.draw_text(startX, numBaseline, numSize, numStr.c_str());

        const double blockTop = cy - blockH * 0.5;
        const double suffixBaseline = blockTop + Gfx2D::text_ascent(suffixSize);
        const double enBaseline = blockTop + suffixH + lineGap + Gfx2D::text_ascent(enSize);
        g.draw_text(rightX, suffixBaseline, suffixSize, "号线");
        g.draw_text(rightX, enBaseline, enSize, routeEn.c_str());
    } else {
        /* ---------- 命名线: cn + en centered ---------- */
        const double baseCn = std::max(1.0, static_cast<double>(std::lround(h * 0.30 * opts.scale)));
        const double baseEn = std::max(1.0, static_cast<double>(std::lround(h * 0.20 * opts.scale)));
        const double centerX = x + w * 0.5;

        const double cnW = Gfx2D::text_width(baseCn, routeCn.c_str());
        const double cnH = Gfx2D::text_ascent(baseCn) + Gfx2D::text_descent(baseCn);
        const double enW = routeEn.empty() ? 0 : Gfx2D::text_width(baseEn, routeEn.c_str());
        const double enH = routeEn.empty() ? 0
            : Gfx2D::text_ascent(baseEn) + Gfx2D::text_descent(baseEn);
        const double gap2 = routeEn.empty() ? 0 : std::max(1.0, static_cast<double>(std::lround(h * 0.05 * opts.scale)));
        const double totalH = cnH + gap2 + enH;
        const double top2 = cy - totalH * 0.5;

        g.set_color(text_color);
        g.draw_text(centerX - cnW * 0.5, top2 + Gfx2D::text_ascent(baseCn),
                    baseCn, routeCn.c_str());
        if (!routeEn.empty()) {
            g.draw_text(centerX - enW * 0.5, top2 + cnH + gap2 + Gfx2D::text_ascent(baseEn),
                        baseEn, routeEn.c_str());
        }
    }
}

/* drawDestinationBlock — 开往 X / To X | 环线方向. */
void draw_destination_block(Gfx2D& g, const ScreenInfo& info, double startX,
                            double limitX, uint32_t text_color) {
    std::string destCn, destEn;
    if (info.isCircular) {
        destCn = circ_cn(info.circularState);
        destEn = circ_en(info.circularState);
    } else {
        const auto& stations = *info.stations;
        destCn = !info.destinationCn.empty() ? info.destinationCn
                 : (!stations.empty() ? stations.back().nameCn : "");
        destEn = !info.destinationEn.empty() ? info.destinationEn
                 : (!stations.empty() ? stations.back().nameEn : "");
    }
    std::string cnText = info.isCircular ? destCn : (destCn.empty() ? "" : "开往 " + destCn);
    std::string enText = info.isCircular ? destEn : (destEn.empty() ? "" : "To " + destEn);
    if (cnText.empty() && enText.empty()) return;

    const double availW = limitX - startX;
    if (availW < K(60)) return;

    double cnSize = K(32), enSize = K(22);
    const double cnW = Gfx2D::text_width(cnSize, cnText.c_str());
    const double enW = Gfx2D::text_width(enSize, enText.c_str());
    const double maxW = std::max(cnW, enW);
    if (maxW > availW && maxW > 0) {
        const double k = availW / maxW;
        cnSize = std::max(1.0, std::floor(cnSize * k));
        enSize = std::max(1.0, std::floor(enSize * k));
    }

    const double cnBase = K(62);
    const double enBase = K(102);
    g.set_color(text_color);
    if (!cnText.empty()) g.draw_text(startX, cnBase, cnSize, cnText.c_str());
    if (!enText.empty()) g.draw_text(startX, enBase, enSize, enText.c_str());
}

/* drawVehicleNumBox — 车号"液态玻璃"卡片. */
void draw_vehicle_num_box(Gfx2D& g, const ScreenInfo& info, uint32_t route_color) {
    const std::string& numText = info.vehicleNum;
    if (numText.empty()) return;   /* JS: 车号为空 → 跳过整张卡片 */

    const double size = K(36);     /* FONT_VEHICLE_NUM = 36 * LAYOUT_K */
    const double textW = Gfx2D::text_width(size, numText.c_str());
    const double ascent = Gfx2D::text_ascent(size);
    const double descent = Gfx2D::text_descent(size);
    const double textH = ascent + descent;

    const double boxH = K(80);
    const double pad = std::max(0.0, static_cast<double>(std::lround((boxH - textH) * 0.5)));
    const double boxW = textW + pad * 2;
    const double boxY = (HEADER_H - boxH) * 0.5;
    const double boxX = (TEX_W - boxY) - boxW;
    const double baseY = boxY + (boxH + ascent - descent) * 0.5;
    const double textCx = boxX + boxW * 0.5;
    const double radius = 14;

    /* 弱阴影 (6 steps) */
    for (int s = 6; s >= 1; s--) {
        const int alpha = static_cast<int>(std::lround(28 * (1 - (s - 1) / 6.0)));
        g.set_color(mtr::clra(0, 0, 0, alpha));
        g.fill_round_rect(boxX - s, boxY - s + 3, boxW + s * 2, boxH + s * 2,
                          radius + s, radius + s);
    }

    /* 卡片底色 */
    g.set_color(route_color);
    g.fill_round_rect(boxX, boxY, boxW, boxH, radius, radius);

    /* 磨砂高光 ×2 */
    g.set_color(mtr::clra(255, 255, 255, 60));
    g.fill_round_rect(boxX + 3, boxY + 3, boxW - 6, std::lround(boxH * 0.45),
                      radius, radius);
    g.set_color(mtr::clra(255, 255, 255, 90));
    g.fill_round_rect(boxX + 3, boxY + 3, boxW - 6, std::lround(boxH * 0.16),
                      radius, radius);

    /* 玻璃描边 */
    g.set_color(mtr::clra(255, 255, 255, 130));
    g.draw_round_rect(boxX, boxY, boxW, boxH, radius, radius, 2.0);

    /* 车号文字（对比色） */
    g.set_color(Gfx2D::contrast_text(route_color));
    g.draw_text(textCx - textW * 0.5, baseY, size, numText.c_str());
}

/* drawHeader — 顶部信息栏. */
void draw_header(Gfx2D& g, const ScreenInfo& info) {
    const uint32_t route_color = info.routeColor ? info.routeColor : LINE_COLOR;
    const uint32_t text_color = Gfx2D::contrast_text(route_color);
    g.set_color(route_color);
    g.fill_rect(0, 0, TEX_W, HEADER_H);

    const std::string& routeCn = info.routeNameCn;
    const std::string& routeEn = info.routeNameEn;

    double leftX = 24;
    /* routeLogo: the JS loads a PNG via Resources; the native port draws
       a route-color chip with the line id (JS falls back to "no logo" the
       same way — leftX stays 24 when the badge is unavailable). */
    {
        const std::string lineId = normalize_line_id(routeCn);
        if (!lineId.empty()) {
            const double lh = HEADER_H - K(24);
            g.draw_logo_badge(leftX, K(12), lh * 0.85, lh, info.routeColor,
                              lineId.c_str(), K(18));
            leftX += lh * 0.85 + K(36);
        }
    }

    double nameRightX = leftX;
    if (!routeCn.empty() || !routeEn.empty()) {
        RouteNameOpts nameOpts;   /* { scale: 1.0, numberBold: true } */
        const double contentW = measure_route_name_block(routeCn, routeEn, HEADER_H, nameOpts);
        draw_route_name_block(g, leftX, 0, contentW, HEADER_H, routeCn, routeEn,
                              text_color, nameOpts);
        nameRightX = leftX + contentW;
    }

    const double centerX = TEX_W * 0.5;
    const char* label = info.isDoorOpen ? "到达" : "下一站";
    const char* labelEn = info.isDoorOpen ? "Arrived" : "Next Station";
    const StationInfo& bigStation = info.isDoorOpen
        ? (*info.stations)[std::min(info.currentIdx, (int)info.stations->size() - 1)]
        : (*info.stations)[std::min(info.nextIdx, (int)info.stations->size() - 1)];

    double centerLimit = centerX;
    {
        const std::string cnFull = std::string(label) + " " + bigStation.nameCn;
        const std::string enFull = std::string(labelEn) + " " + bigStation.nameEn;
        const double halfCn = Gfx2D::text_width(K(40), cnFull.c_str()) * 0.5;
        const double halfEn = Gfx2D::text_width(K(28), enFull.c_str()) * 0.5;
        centerLimit = centerX - std::max(halfCn, halfEn) - K(30);
    }
    draw_destination_block(g, info, nameRightX + K(36), centerLimit, text_color);

    {
        const std::string cnFull = std::string(label) + " " + bigStation.nameCn;
        const std::string enFull = std::string(labelEn) + " " + bigStation.nameEn;
        draw_centered_text(g, cnFull, centerX, K(62), K(40), text_color);
        draw_centered_text(g, enFull, centerX, K(102), K(28), text_color);
    }
    draw_vehicle_num_box(g, info, route_color);
}

/* ==================== data.js: partial transfer panel ==================== */

void draw_partial_transfer_info(Gfx2D& g, int curIdx,
                                const std::vector<StationInfo>& stations) {
    if (curIdx < 0 || curIdx >= (int)stations.size()) return;
    const std::vector<Transfer>& transfers = stations[curIdx].transfers;
    if (transfers.empty()) return;

    std::vector<ParsedTransferRoute> items;
    for (const Transfer& t : transfers) {
        ParsedTransferRoute item = parse_transfer_route(t.name);
        item.color = t.color;
        items.push_back(item);
    }
    std::sort(items.begin(), items.end(), compare_transfer_route);

    const double titleX = 40;
    const double titleY = HEADER_H + K(48);

    g.set_color(BLACK_COLOR);
    g.draw_text(titleX, titleY, K(34), "换乘");
    g.draw_text(titleX + K(108), titleY, K(30), "Transfer");

    const int cols = 3;
    const double badgeGap = K(14);
    const double startX = 40;
    const double startY = titleY + K(32);
    const double availW = 480;
    const double availH = TEX_H - startY - 20;

    const int rows = (int)((items.size() + cols - 1) / cols);
    const double maxByW = std::floor((availW - (cols - 1) * badgeGap) / cols);
    const double maxByH = std::floor((availH - (rows - 1) * badgeGap) / rows);
    double badgeSize = std::min(std::min(maxByW, maxByH), (double)K(130));
    if (badgeSize < 1) badgeSize = 1;

    const double badgeW = badgeSize;
    const double badgeH = std::lround(badgeSize * 0.55);

    RouteNameOpts nameOpts;   /* { scale: 1.0, numberBold: false, numberCenter: true } */
    nameOpts.number_bold = false;
    nameOpts.number_center = true;
    const double padX = K(10);

    for (size_t i = 0; i < items.size(); i++) {
        const int col = (int)(i % cols);
        const int row = (int)(i / cols);
        const double bx = startX + col * (badgeW + badgeGap);
        const double by = startY + row * (badgeH + badgeGap);

        g.set_color(items[i].color);
        g.fill_round_rect(bx, by, badgeW, badgeH, badgeH, badgeH);

        draw_route_name_block(g, bx + padX, by, badgeW - padX * 2, badgeH,
                              items[i].cn, items[i].en,
                              Gfx2D::contrast_text(items[i].color), nameOpts);
    }
}

/* drawExitInfo — ported; inactive until the ABI carries station exits. */
void draw_exit_info(Gfx2D&, int, const std::vector<StationInfo>&, uint32_t) {
    /* v2 snapshot has no station exits (JS reads station.getExits()).
       The JS bails out identically when the list is empty — the panel
       is a documented extension point (add exits to JcmStation). */
}

/* ==================== draw_circular.js ==================== */

void draw_circular_full_map(Gfx2D& g, const JsLcdState& state, const ScreenInfo& info) {
    const auto& stations = *info.stations;
    const int n = (int)stations.size();

    const uint32_t route_color = info.routeColor ? info.routeColor : LINE_COLOR;
    const bool reversed = (info.circularState == 2);   /* ANTICLOCKWISE */
    const bool clockwise = !reversed;

    const int highlightIdx = info.isDoorOpen ? info.currentIdx : info.nextIdx;

    /* 灰色轨道 + 线路色轨道 (RoundRectangle2D stroke) */
    g.set_color(GRAY_COLOR);
    g.draw_round_rect(RING_LEFT_X, RING_TOP_Y,
                      RING_RIGHT_X - RING_LEFT_X, RING_BOTTOM_Y - RING_TOP_Y,
                      RING_RADIUS * 2, RING_RADIUS * 2, RING_STROKE);
    g.set_color(route_color);
    g.draw_round_rect(RING_LEFT_X, RING_TOP_Y,
                      RING_RIGHT_X - RING_LEFT_X, RING_BOTTOM_Y - RING_TOP_Y,
                      RING_RADIUS * 2, RING_RADIUS * 2, RING_STROKE);

    if (!info.isDoorOpen) draw_progress_overlay(g, state, info, reversed, clockwise);

    /* ---- 自适应字号 ---- */
    const double topStartX = RING_LEFT_X + RING_RADIUS + RING_PADDING;
    const double topEndX = RING_RIGHT_X - RING_RADIUS - RING_PADDING;
    const int topCount = (n + 1) / 2;
    const int botCount = n - topCount;
    const double rowW = topEndX - topStartX;

    const double topSpacing = topCount > 1 ? rowW / (topCount - 1) : rowW;
    const double botSpacing = botCount > 1 ? rowW / (botCount - 1) : rowW;
    const double minSpacing = std::min(topSpacing, botSpacing);
    const double availW = minSpacing * 0.98;

    const double baseCnSize = K(32);
    const double baseEnSize = K(26);

    double maxCnW = 0, maxEnW = 0;
    for (const StationInfo& st : stations) {
        maxCnW = std::max(maxCnW, Gfx2D::text_width(baseCnSize, st.nameCn.c_str()));
        maxEnW = std::max(maxEnW, Gfx2D::text_width(baseEnSize, st.nameEn.c_str()));
    }

    const double MIN_CN_SIZE = 36, MIN_EN_SIZE = 28;
    const double cnSize = compute_adaptive_font_size(baseCnSize, maxCnW, availW, MIN_CN_SIZE);
    const double enSize = compute_adaptive_font_size(baseEnSize, maxEnW, availW, MIN_EN_SIZE);

    /* ---- 绘制循环 ---- */
    const double dotR = K(14);
    const double topCnOff = K(42), topEnOff = K(20);
    const double botCnOff = K(50), botEnOff = K(72);

    for (int i = 0; i < n; i++) {
        const StationPos pos = get_station_pos(i, n, reversed);

        const uint32_t fillColor = (i == highlightIdx) ? RED_COLOR : GREEN_COLOR;
        const uint32_t nameColor = (i == highlightIdx) ? route_color : BLACK_COLOR;

        g.set_color(fillColor);
        g.fill_oval(pos.x - dotR, pos.y - dotR, dotR * 2, dotR * 2);
        g.set_color(BLACK_COLOR);
        g.draw_oval(pos.x - dotR, pos.y - dotR, dotR * 2, dotR * 2, 3.0);

        if (pos.section == 0) {
            draw_adaptive_centered_text(g, stations[i].nameCn, pos.x, pos.y - topCnOff,
                                        cnSize, availW, nameColor);
            draw_adaptive_centered_text(g, stations[i].nameEn, pos.x, pos.y - topEnOff,
                                        enSize, availW, nameColor);
        } else {
            draw_adaptive_centered_text(g, stations[i].nameCn, pos.x, pos.y + botCnOff,
                                        cnSize, availW, nameColor);
            draw_adaptive_centered_text(g, stations[i].nameEn, pos.x, pos.y + botEnOff,
                                        enSize, availW, nameColor);
        }

        if (!stations[i].transfers.empty()) {
            draw_transfer_badges(g, stations[i].transfers, pos);
        }
    }
}

void draw_circular_partial_map(Gfx2D& g, const JsLcdState& state, const ScreenInfo& info) {
    const auto& stations = *info.stations;
    const int n = (int)stations.size();
    const int curIdx = info.currentIdx;
    const int nxtIdx = info.nextIdx;

    const uint32_t route_color = info.routeColor ? info.routeColor : LINE_COLOR;

    if (info.isDoorOpen) {
        const double whiteCenterY = (HEADER_H + TEX_H) * 0.5;
        draw_big_station_name(g, stations[curIdx], whiteCenterY, route_color);
        draw_partial_transfer_info(g, curIdx, stations);
        draw_exit_info(g, curIdx, stations, route_color);
        return;
    }

    const double lineY = K(300);
    const double lineL = 500;
    const double lineR = TEX_W - 500;

    g.set_color(GRAY_COLOR);
    g.draw_line(lineL, lineY, lineR, lineY, 6.0);

    const double rectX = lineL + 30;
    const double rectW = lineR - lineL - 60;
    const double rectH = K(64);

    g.set_color(GRAY_COLOR);
    g.fill_round_rect(rectX, lineY - rectH * 0.5, rectW, rectH, rectH, rectH);

    /* 环线: 下一站恒在中间，前后各 2 站（环绕） */
    int displayIdx[5];
    for (int k = 0; k < 5; k++) {
        const int d = k - 2;
        displayIdx[k] = ((nxtIdx + d) % n + n) % n;
    }
    const double spacing = rectW / 6;
    const int count = 5;

    g.set_color(route_color);   /* 环线: 整条胶囊用线路色 */
    g.fill_round_rect(rectX, lineY - rectH * 0.5, rectW, rectH, rectH, rectH);

    g.set_color(WHITE_COLOR);
    g.draw_round_rect(rectX, lineY - rectH * 0.5, rectW, rectH, rectH, rectH, 2.0);

    const double dotR = K(24);
    for (int k = 0; k < count; k++) {
        const int actualIdx = displayIdx[k];
        const StationInfo& station = stations[actualIdx];
        const double x = rectX + (k + 1) * spacing;

        g.set_color(actualIdx == nxtIdx ? RED_COLOR : GREEN_COLOR);
        g.fill_oval(x - dotR, lineY - dotR, dotR * 2, dotR * 2);

        const bool isAbove = (k % 2 == 0);
        const double aboveOff = K(80);
        const double textY1 = isAbove ? (lineY - aboveOff) : (lineY + aboveOff);
        const double textY2 = textY1 + K(26);
        draw_centered_text(g, station.nameCn, x, textY1, K(36), BLACK_COLOR);
        draw_centered_text(g, station.nameEn, x, textY2, K(22), BLACK_COLOR);
    }

    draw_partial_transfer_info(g, nxtIdx, stations);
    draw_exit_info(g, nxtIdx, stations, route_color);
}

/* ==================== draw_linear.js ==================== */

/* 直线完整图换乘徽章（竖向堆叠，header 同款排版） */
void draw_linear_transfer_badges(Gfx2D& g, const std::vector<Transfer>& transfers,
                                 const LinearPos& pos) {
    std::vector<ParsedTransferRoute> items;
    for (const Transfer& t : transfers) {
        ParsedTransferRoute item = parse_transfer_route(t.name);
        item.color = t.color;
        items.push_back(item);
    }
    std::sort(items.begin(), items.end(), compare_transfer_route);

    const double badgeH = K(30);
    const double gap = K(4);
    const double padX = K(14);
    const double lineY = pos.y;
    const double step = badgeH + gap;

    RouteNameOpts nameOpts;
    nameOpts.number_bold = false;
    nameOpts.number_center = true;

    std::vector<double> widths(items.size());
    for (size_t k = 0; k < items.size(); k++) {
        widths[k] = measure_route_name_block(items[k].cn, items[k].en, badgeH, nameOpts)
                    + padX * 2;
    }

    const double topGap = K(16);
    double startY;
    if (pos.above) {
        startY = lineY + topGap;
    } else {
        startY = lineY - topGap - (items.size() - 1) * step - badgeH;
    }

    for (size_t j = 0; j < items.size(); j++) {
        const double w = widths[j];
        const double y = startY + j * step;
        const double x = pos.x - w * 0.5;

        g.set_color(items[j].color);
        g.fill_round_rect(x, y, w, badgeH, badgeH, badgeH);
        draw_route_name_block(g, x + padX, y, w - padX * 2, badgeH,
                              items[j].cn, items[j].en,
                              Gfx2D::contrast_text(items[j].color), nameOpts);
    }
}

void draw_linear_full_map(Gfx2D& g, const JsLcdState& state, const ScreenInfo& info) {
    const auto& stations = *info.stations;
    const int n = (int)stations.size();
    const uint32_t route_color = info.routeColor ? info.routeColor : LINE_COLOR;
    const int highlightIdx = info.isDoorOpen ? info.currentIdx : info.nextIdx;

    const double lineY = (RING_TOP_Y + RING_BOTTOM_Y) * 0.5;
    const double startX = RING_LEFT_X + RING_PADDING;
    const double endX = RING_RIGHT_X - RING_PADDING;
    const double rowW = endX - startX;

    /* ---- 自适应字号 ---- */
    const double spacing = n > 1 ? rowW / (n - 1) : rowW;
    const double availW = spacing * 0.98;

    const double baseCnSize = K(32), baseEnSize = K(26);
    double maxCnW = 0, maxEnW = 0;
    for (const StationInfo& st : stations) {
        maxCnW = std::max(maxCnW, Gfx2D::text_width(baseCnSize, st.nameCn.c_str()));
        maxEnW = std::max(maxEnW, Gfx2D::text_width(baseEnSize, st.nameEn.c_str()));
    }
    const double MIN_CN_SIZE = 36, MIN_EN_SIZE = 28;
    const double cnSize = compute_adaptive_font_size(baseCnSize, maxCnW, availW, MIN_CN_SIZE);
    const double enSize = compute_adaptive_font_size(baseEnSize, maxEnW, availW, MIN_EN_SIZE);

    /* ---- 轨道 ---- */
    g.set_color(GRAY_COLOR);
    g.draw_line(startX, lineY, endX, lineY, RING_STROKE);

    /* 非环线: 仅"当前站 → 终点站"段涂线路色 */
    if (info.currentIdx < n - 1) {
        const LinearPos pStart = get_linear_station_pos(info.currentIdx, n);
        const LinearPos pEnd = get_linear_station_pos(n - 1, n);
        g.set_color(route_color);
        g.draw_line(pStart.x, lineY, pEnd.x, lineY, RING_STROKE);
    }

    /* 进度闪烁叠加 */
    if (!info.isDoorOpen) {
        const LinearPos p1 = get_linear_station_pos(info.currentIdx, n);
        const LinearPos p2 = get_linear_station_pos(info.nextIdx, n);
        const double borderStroke = RING_STROKE + K(3);
        g.set_color(BLACK_COLOR);
        g.draw_line(p1.x, p1.y, p2.x, p2.y, borderStroke);
        g.set_color(state.blinkState ? BLINK_GREEN : BLINK_RED);
        g.draw_line(p1.x, p1.y, p2.x, p2.y, RING_STROKE);
    }

    /* ---- 绘制循环 ---- */
    const double dotR = K(14);
    const double halfTrack = RING_STROKE * 0.5;
    const double edgeGap = K(12);
    const double lineGap = K(2);
    const double enCapHeight = std::lround(enSize * 0.72);
    const double cnAscent = Gfx2D::text_ascent(cnSize);
    const double cnDescent = Gfx2D::text_descent(cnSize);
    const double enDescent = Gfx2D::text_descent(enSize);

    for (int i = 0; i < n; i++) {
        const LinearPos pos = get_linear_station_pos(i, n);

        uint32_t fillColor, nameColor;
        if (i < highlightIdx)      { fillColor = WHITE_COLOR;  nameColor = DOT_GRAY; }
        else if (i == highlightIdx){ fillColor = RED_COLOR;    nameColor = route_color; }
        else                       { fillColor = GREEN_COLOR;  nameColor = BLACK_COLOR; }

        g.set_color(fillColor);
        g.fill_oval(pos.x - dotR, pos.y - dotR, dotR * 2, dotR * 2);
        g.set_color(BLACK_COLOR);
        g.draw_oval(pos.x - dotR, pos.y - dotR, dotR * 2, dotR * 2, 3.0);

        /* 奇偶交替上下 + 字形度量定位（JS 同款数学） */
        double cnY, enY;
        if (pos.above) {
            const double enBaseline = pos.y - halfTrack - enDescent - edgeGap;
            const double cnBaseline = enBaseline - enCapHeight - lineGap - cnDescent;
            cnY = cnBaseline;
            enY = enBaseline;
        } else {
            const double cnBaseline2 = pos.y + halfTrack + cnAscent + edgeGap;
            const double enBaseline2 = cnBaseline2 + cnDescent + lineGap + enCapHeight;
            cnY = cnBaseline2;
            enY = enBaseline2;
        }

        draw_adaptive_centered_text(g, stations[i].nameCn, pos.x, cnY, cnSize, availW, nameColor);
        draw_adaptive_centered_text(g, stations[i].nameEn, pos.x, enY, enSize, availW, nameColor);

        if (!stations[i].transfers.empty()) {
            draw_linear_transfer_badges(g, stations[i].transfers, pos);
        }
    }
}

void draw_linear_partial_map(Gfx2D& g, const JsLcdState& state, const ScreenInfo& info) {
    const auto& stations = *info.stations;
    const int n = (int)stations.size();
    const int curIdx = info.currentIdx;
    const int nxtIdx = info.nextIdx;
    const uint32_t route_color = info.routeColor ? info.routeColor : LINE_COLOR;

    if (info.isDoorOpen) {
        const bool isTerminus = (curIdx >= n - 1);
        const double whiteCenterY = (HEADER_H + TEX_H) * 0.5;
        const double bigNameCenterY = isTerminus ? (whiteCenterY - 50) : whiteCenterY;
        draw_big_station_name(g, stations[curIdx], bigNameCenterY, route_color);
        if (isTerminus) {
            draw_terminus_message(g, bigNameCenterY + 140);
        }
        draw_partial_transfer_info(g, curIdx, stations);
        draw_exit_info(g, curIdx, stations, route_color);
        return;
    }

    const double lineY = K(300);
    const double lineL = 500;
    const double lineR = TEX_W - 500;

    g.set_color(GRAY_COLOR);
    g.draw_line(lineL, lineY, lineR, lineY, 6.0);

    const double rectX = lineL + 30;
    const double rectW = lineR - lineL - 60;
    const double rectH = K(64);

    g.set_color(GRAY_COLOR);
    g.fill_round_rect(rectX, lineY - rectH * 0.5, rectW, rectH, rectH, rectH);

    /* 非环线: 从 max(0, nxtIdx-2) 起显示，最多 5 站 */
    int startIdx = std::max(0, nxtIdx - 2);
    int endIdx = std::min(n - 1, startIdx + 4);
    if (endIdx - startIdx < 4) startIdx = std::max(0, endIdx - 4);
    const int cnt = endIdx - startIdx + 1;
    int displayIdx[5];
    for (int k = 0; k < cnt; k++) displayIdx[k] = startIdx + k;
    const double spacing = rectW / (cnt + 1);
    const int count = cnt;

    int curLocalIdx = -1;
    for (int k = 0; k < count; k++) {
        if (displayIdx[k] == curIdx) { curLocalIdx = k; break; }
    }

    /* 非环线: 仅"未经过段"涂线路色（clip 实现同 JS） */
    double boundaryX = -1;
    if (curIdx <= 0) {
        g.set_color(route_color);
        g.fill_round_rect(rectX, lineY - rectH * 0.5, rectW, rectH, rectH, rectH);
    } else if (curLocalIdx >= 0 && curLocalIdx < count) {
        boundaryX = rectX + (curLocalIdx + 1) * spacing;
        g.push_clip(boundaryX, lineY - rectH * 0.5, rectX + rectW - boundaryX, rectH);
        g.set_color(route_color);
        g.fill_round_rect(rectX, lineY - rectH * 0.5, rectW, rectH, rectH, rectH);
        g.pop_clip();
    } else {
        g.set_color(route_color);
        g.fill_round_rect(rectX, lineY - rectH * 0.5, rectW, rectH, rectH, rectH);
    }

    g.set_color(WHITE_COLOR);
    g.draw_round_rect(rectX, lineY - rectH * 0.5, rectW, rectH, rectH, rectH, 2.0);

    if (boundaryX > 0) {
        const double circleR = rectH * 0.5;
        g.set_color(route_color);
        g.fill_oval(boundaryX - circleR, lineY - circleR, circleR * 2, circleR * 2);
    }

    const double dotR = K(24);
    for (int k = 0; k < count; k++) {
        const int actualIdx = displayIdx[k];
        const StationInfo& station = stations[actualIdx];
        const double x = rectX + (k + 1) * spacing;

        const bool isPassed = actualIdx < curIdx;
        g.set_color(isPassed ? DEEP_GRAY_DOT
                             : (actualIdx == nxtIdx ? RED_COLOR : GREEN_COLOR));
        g.fill_oval(x - dotR, lineY - dotR, dotR * 2, dotR * 2);

        const uint32_t nameColor = isPassed ? DOT_GRAY : BLACK_COLOR;
        const bool isAbove = (k % 2 == 0);
        const double aboveOff = K(80);
        const double textY1 = isAbove ? (lineY - aboveOff) : (lineY + aboveOff);
        const double textY2 = textY1 + K(26);
        draw_centered_text(g, station.nameCn, x, textY1, K(36), nameColor);
        draw_centered_text(g, station.nameEn, x, textY2, K(22), nameColor);
    }

    draw_partial_transfer_info(g, nxtIdx, stations);
    draw_exit_info(g, nxtIdx, stations, route_color);
}

/* ==================== main.js: drawOneScreen ==================== */

void draw_one_screen(Gfx2D& g, const JsLcdState& state, const ScreenInfo& info, int pageMode) {
    /* 白底 */
    g.set_color(WHITE_COLOR);
    g.fill_rect(0, 0, TEX_W, TEX_H);

    draw_header(g, info);

    if (info.isCircular) {
        if (pageMode == 0) draw_circular_full_map(g, state, info);
        else               draw_circular_partial_map(g, state, info);
    } else {
        if (pageMode == 0) draw_linear_full_map(g, state, info);
        else               draw_linear_partial_map(g, state, info);
    }
}

/* ==================== the script ==================== */

struct JsLcdScript : VehicleScript<JsLcdState> {
    static constexpr auto ID = "jslcd:vehicle_lcd";

    /* physical aspect — computed once from the LCD quad (main.js) */
    double aspect = 3.5;
    double scale_x = 1.0, scale_y = 1.0;
    int raster_w = 0, raster_h = 0;

    void create(VehicleContext& ctx, JsLcdState& state, const Train& train) override {
        aspect = compute_lcd_aspect();
        raster_w = RASTER_W;
        raster_h = raster_h_for_aspect(aspect);
        scale_x = scale_x_for(aspect);
        scale_y = scale_y_for(aspect);

        state.cars.clear();
        ensure_cars(ctx.input(), state, train);

        if (ctx.host() && ctx.host()->acquire_model) {
            /* one shared quad model per side (the JS builds one DisplayHelper
               model per car; the quads are identical, so the native port
               deduplicates them — the host refcounts by path). */
            state.modelLeft = ctx.host()->acquire_model(
                ctx.host()->user, "jslcd:models/lcd_door_left.json");
            state.modelRight = ctx.host()->acquire_model(
                ctx.host()->user, "jslcd:models/lcd_door_right.json");
        }
        state.paintedCarSig.clear();
    }

    void render(VehicleContext& ctx, JsLcdState& state, const Train& train) override {
        const int carCount = train.car_count();
        ensure_cars(ctx.input(), state, train);

        const int64_t now = train.game_time_millis();

        /* ---- siding name → 车号 (JS: vehicle.getSiding().getName()) ---- */
        std::string sidingName(train.siding_name().str());

        /* ---- route cache (JS: cachedRouteId !== routeId → rebuild) ---- */
        const mtr::StopList thisRouteStops = train.this_route_stops();
        const bool hasRoute = thisRouteStops.size() > 0;
        if (hasRoute) {
            std::string routeId = std::to_string(train.this_route_id());
            if (state.cachedRouteId != routeId) {
                state.cachedRouteId = routeId;
                state.stations = get_stations_from_stops(
                    thisRouteStops, get_non_extra_parts(train.route_name().str()));
                state.stationsRev++;

                const CircularResult circ =
                    resolve_circular_state(train, state.circularLocked, state.circularState);
                state.isCircular = circ.isCircular;
                state.circularState = circ.circularState;

                /* route name/color (JS: getRouteInfo → firstStop.route) */
                std::string cleanName = get_non_extra_parts(train.route_name().str());
                std::string routeCn, routeEn;
                const size_t bar = cleanName.find('|');
                if (bar != std::string::npos) {
                    routeCn = cleanName.substr(0, bar);
                    routeEn = cleanName.substr(bar + 1);
                    const size_t a = routeCn.find_first_not_of(' ');
                    const size_t b = routeCn.find_last_not_of(' ');
                    routeCn = a == std::string::npos ? "" : routeCn.substr(a, b - a + 1);
                    const size_t a2 = routeEn.find_first_not_of(' ');
                    const size_t b2 = routeEn.find_last_not_of(' ');
                    routeEn = a2 == std::string::npos ? "" : routeEn.substr(a2, b2 - a2 + 1);
                } else {
                    split_cjk_non_cjk(cleanName, routeCn, routeEn);
                    if (routeCn.empty() && !cleanName.empty()) routeCn = cleanName;
                }
                state.routeNameCn = routeCn;
                state.routeNameEn = routeEn;
                state.routeColor = train.route_color() != 0
                    ? (static_cast<uint32_t>(train.route_color()) | 0xFF000000u)
                    : LINE_COLOR;

                /* destination (JS: lastStop.destinationName) */
                const mtr::Stop lastStop = thisRouteStops.at(thisRouteStops.size() - 1);
                std::string destCn, destEn;
                split_cjk_non_cjk(lastStop.destination_name().str(), destCn, destEn);
                state.destinationCn = destCn;
                state.destinationEn = destEn;

                state.lastIdxShown = -1;
            }
        }
        const bool hasStations = !state.stations.empty();

        /* ---- door state machine (util.js) ---- */
        const double doorVal = vehicle_door_value(train);
        const double absSpeed = vehicle_speed_ms(train);
        if (state.isDoorOpen) {
            if (doorVal < 0.05 && absSpeed > 0.3) state.isDoorOpen = false;
        } else {
            if (doorVal > 0.5 || (absSpeed < 0.1 && doorVal > 0.1)) state.isDoorOpen = true;
        }

        /* ---- page cycle + blink (main.js) ---- */
        const int64_t cycleElapsed = now - state.lastCycleTime;
        if (state.pageMode == 0) {
            if (cycleElapsed >= CYCLE_FULL_MS) { state.pageMode = 1; state.lastCycleTime = now; }
        } else {
            if (cycleElapsed >= CYCLE_PARTIAL_MS) { state.pageMode = 0; state.lastCycleTime = now; }
        }
        if (!state.isDoorOpen && now - state.lastBlinkTime >= BLINK_INTERVAL_MS) {
            state.blinkState = !state.blinkState;
            state.lastBlinkTime = now;
        }

        /* ---- stop counting on door-close→open debounce (main.js) ---- */
        if (hasStations) {
            if (state.isDoorOpen && !state.lastDoorOpen) {
                if (now - state.lastCountTime > STOP_DEBOUNCE_MS) {
                    state.stopCountIdx++;
                    const int n = (int)state.stations.size();
                    if (state.isCircular && n > 0) {
                        state.stopCountIdx = ((state.stopCountIdx % n) + n) % n;
                    } else if (state.stopCountIdx >= n) {
                        state.stopCountIdx = n - 1;
                    }
                    state.lastCountTime = now;
                }
            }
        }
        state.lastDoorOpen = state.isDoorOpen;

        /* ---- current/next index (util.js getCurrentStationIdx) ---- */
        int currentIdx = 0, nextIdx = 0;
        if (hasStations) {
            const int n = (int)state.stations.size();
            int rawN = train.next_stop_index();
            if (rawN < 0) rawN = 0;
            if (rawN > n) rawN = n;
            if (state.isCircular) {
                const int m = ((rawN % n) + n) % n;
                currentIdx = state.isDoorOpen ? m : ((m - 1 + n) % n);
            } else {
                if (rawN >= n) currentIdx = n - 1;
                else if (state.isDoorOpen) currentIdx = rawN;
                else currentIdx = std::max(0, rawN - 1);
            }
            currentIdx = std::max(0, std::min(currentIdx, n - 1));
            if (state.isCircular) nextIdx = (currentIdx + 1) % n;
            else nextIdx = std::min(currentIdx + 1, n - 1);

            if (state.lastIdxShown != currentIdx) state.lastIdxShown = currentIdx;
        }

        /* ---- 行驶方向左右屏 (util.js getTravelLeftSide) ---- */
        int leftMode = 0, rightMode = 0;   /* 0 full, 1 partial */
        if (hasStations) {
            char side;
            bool reversed;
            get_travel_left_side(train, side, reversed);
            const int cyclingMode = state.isDoorOpen ? 1 : state.pageMode;
            if (side == 'L') { leftMode = cyclingMode; rightMode = 0; }
            else             { leftMode = 0; rightMode = cyclingMode; }
        }

        /* ---- repaint-on-change signature (FNV-1a mix) ----
           JS repaints every frame; the native port repaints only when
           any of these inputs actually change. */
        uint64_t sig = 1469598103934665603ULL;
        auto mix = [&](uint64_t v) {
            sig ^= v;
            sig *= 1099511628211ULL;
        };
        mix(static_cast<uint64_t>(state.pageMode));
        mix(static_cast<uint64_t>(state.blinkState ? 1 : 0));
        mix(static_cast<uint64_t>(state.isDoorOpen ? 1 : 0));
        mix(static_cast<uint64_t>(hasRoute ? 1 : 0));
        mix(static_cast<uint64_t>(currentIdx));
        mix(static_cast<uint64_t>(nextIdx));
        mix(static_cast<uint64_t>(state.stationsRev));
        mix(std::hash<std::string>()(sidingName));
        mix(leftMode);

        /* ---- per-car painting (repaint budget) ----
           The JS repaints every car every frame; the native port
           repaints only stale cars and at most REPAINT_BUDGET cars per
           frame — the dirty-rect model defers the matching uploads for
           free, so a full-train repaint burst (route change, page flip,
           blink) spreads over a few 60 fps frames instead of one
           budget-blowing 40 ms frame. Steady state (nothing changed)
           stays at zero raster work either way. */
        constexpr int REPAINT_BUDGET = 2;
        int repaint_budget = REPAINT_BUDGET;

        for (int ci = 0; ci < carCount && ci < (int)state.cars.size(); ci++) {
            CarScreens& cs = state.cars[ci];

            const bool stale = ci >= (int)state.paintedCarSig.size()
                             || state.paintedCarSig[ci] != sig;
            if (!stale || repaint_budget <= 0) {
                /* up to date (or this frame's budget is spent): the
                   textures keep their content; upload() is a no-op
                   because the dirty box was cleared when last shipped */
            } else if (!hasRoute || !hasStations) {
                /* JS: clearScreen(g) 白屏 */
                repaint_budget--;
                {
                    Gfx2D gL(cs.left, ctx.host());
                    gL.set_scale(scale_x, scale_y);
                    gL.set_color(WHITE_COLOR);
                    gL.fill_rect(0, 0, TEX_W, TEX_H);
                }
                if (ci >= (int)state.paintedCarSig.size()) state.paintedCarSig.push_back(sig);
                else state.paintedCarSig[ci] = sig;
            } else {
                ScreenInfo info;
                info.stations = &state.stations;
                info.currentIdx = currentIdx;
                info.nextIdx = nextIdx;
                info.isCircular = state.isCircular;
                info.circularState = state.circularState;
                info.destinationCn = state.destinationCn;
                info.destinationEn = state.destinationEn;
                info.isDoorOpen = state.isDoorOpen;
                info.blinkState = state.blinkState;
                info.routeColor = state.routeColor;
                info.routeNameCn = state.routeNameCn;
                info.routeNameEn = state.routeNameEn;
                info.vehicleNum = get_car_display_num(sidingName, ci);

                Gfx2D gL(cs.left, ctx.host());
                gL.set_scale(scale_x, scale_y);
                draw_one_screen(gL, state, info, leftMode);

                if (rightMode == leftMode) {
                    /* identical content by construction → memcpy */
                    std::memcpy(cs.right.pixels(), cs.left.pixels(),
                                static_cast<size_t>(raster_w) * raster_h * 4);
                    cs.right.dirty_all();
                } else {
                    Gfx2D gR(cs.right, ctx.host());
                    gR.set_scale(scale_x, scale_y);
                    draw_one_screen(gR, state, info, rightMode);
                }

                repaint_budget--;
                if (ci >= (int)state.paintedCarSig.size()) state.paintedCarSig.push_back(sig);
                else state.paintedCarSig[ci] = sig;
            }

            /* upload dirty rects (JS: dh.upload() every frame) */
            cs.left.upload(ctx.frame());
            cs.right.upload(ctx.frame());

            /* model attach every frame (JS: ctx.drawCarModel(...)) */
            ctx.draw_car_model(state.modelLeft, ci, nullptr);
            ctx.draw_car_model(state.modelRight, ci, nullptr);
        }

    }

    void dispose(VehicleContext& ctx, JsLcdState& state, const Train&) override {
        for (CarScreens& cs : state.cars) {
            cs.left.close(&ctx.input());
            cs.right.close(&ctx.input());
        }
        state.cars.clear();
        if (state.modelLeft != -1 && ctx.host() && ctx.host()->release_model) {
            ctx.host()->release_model(ctx.host()->user, state.modelLeft);
        }
        if (state.modelRight != -1 && ctx.host() && ctx.host()->release_model) {
            ctx.host()->release_model(ctx.host()->user, state.modelRight);
        }
        state.modelLeft = state.modelRight = -1;
    }

private:
    void ensure_cars(const JcmFrameInput& in, JsLcdState& state, const Train& train) {
        const int carCount = train.car_count();
        if ((int)state.cars.size() == carCount) return;
        state.cars.clear();
        state.paintedCarSig.clear();
        for (int i = 0; i < carCount; i++) {
            CarScreens cs;
            cs.left.create(in, raster_w, raster_h);
            cs.right.create(in, raster_w, raster_h);
            state.cars.push_back(std::move(cs));
        }
    }
};

MTR_REGISTER_VEHICLE_SCRIPT(JsLcdScript)

} /* anonymous namespace */
