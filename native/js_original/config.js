// config.js
// 颜色工具、常量、字体、LOGO 配置与加载
importPackage(java.awt);
importPackage(java.awt.geom);

// ==================== 安全颜色构造 ====================
function clr(r, g, b) {
    var ri = Math.max(0, Math.min(255, r | 0));
    var gi = Math.max(0, Math.min(255, g | 0));
    var bi = Math.max(0, Math.min(255, b | 0));
    return new java.awt.Color((ri << 16) | (gi << 8) | bi);
}
function clra(r, g, b, a) {
    var ri = Math.max(0, Math.min(255, r | 0));
    var gi = Math.max(0, Math.min(255, g | 0));
    var bi = Math.max(0, Math.min(255, b | 0));
    var ai = Math.max(0, Math.min(255, a | 0));
    return new java.awt.Color((ai << 24) | (ri << 16) | (gi << 8) | bi, true);
}
function safeColor(v, fr, fg, fb) {
    try {
        if (v instanceof java.awt.Color) return v;
        if (typeof v === "number") return new java.awt.Color(v & 0xFFFFFF);
        if (v != null && typeof v.getRGB === "function") return new java.awt.Color(v.getRGB() & 0xFFFFFF);
    } catch (e) { }
    return clr(fr, fg, fb);
}
function getContrastTextColor(bg) {
    try {
        var r = bg.getRed();
        var g = bg.getGreen();
        var b = bg.getBlue();
        var y = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
        return y > 0.5 ? BLACK_COLOR : WHITE_COLOR;
    } catch (e) {
        return BLACK_COLOR;
    }
}

// ==================== 物理比例与纹理尺寸 ====================
// 物理 LCD 长宽比 = 21 : 6 = 3.5
var LCD_ASPECT = 3.5;

// 纹理物理尺寸（与物理矩形比例一致）
var SCR_W = 3304;
var SCR_H = Math.round(SCR_W / LCD_ASPECT);   // = 944

// 逻辑绘图尺寸（与物理矩形比例一致）
var TEX_W = 2800;
var TEX_H = Math.round(TEX_W / LCD_ASPECT);   // = 800

// 缩放因子（此时 SCALE_X === SCALE_Y，纹理等比缩放，无拉伸）
var SCALE_X = SCR_W / TEX_W;   // ≈ 1.18
var SCALE_Y = SCR_H / TEX_H;   // ≈ 1.18

// 纵向布局放大系数：相对原 480 高设计
var LAYOUT_K = TEX_H / 480;    // ≈ 1.6667

// ==================== 颜色常量 ====================
var LINE_COLOR = clr(0, 155, 192);
var GRAY_COLOR = clr(118, 130, 137);
var GRAY_ALPHA_C = clra(118, 130, 137, 180);
var RED_COLOR = clr(237, 28, 36);
var GREEN_COLOR = clr(0, 200, 80);
var WHITE_COLOR = java.awt.Color.WHITE;
var BLACK_COLOR = java.awt.Color.BLACK;
var DOT_GRAY = clr(180, 180, 180);
var DEEP_GRAY_DOT = clr(90, 100, 107);
var HEADER_BG = clr(179, 229, 252);
var BLINK_RED = clra(237, 28, 36, 255);
var BLINK_GREEN = clra(0, 200, 80, 255);

// ==================== 时间常量 ====================
var CYCLE_FULL_MS = 10000;
var CYCLE_PARTIAL_MS = 10000;
var BLINK_INTERVAL_MS = 1000;
var STOP_DEBOUNCE_MS = 3000;

// ==================== 纵向布局常量（按 LAYOUT_K 缩放）====================
var HEADER_H = Math.round(130 * LAYOUT_K);    // 217
var RING_TOP_Y = Math.round(240 * LAYOUT_K);    // 400
var RING_BOTTOM_Y = Math.round(390 * LAYOUT_K);    // 650
var RING_LEFT_X = 200;                            // 横向不变
var RING_RIGHT_X = TEX_W - 200;                    // 横向不变
var RING_RADIUS = (RING_BOTTOM_Y - RING_TOP_Y) / 2;  // 125
var RING_STROKE = Math.round(24 * LAYOUT_K);     // 40
var RING_PADDING = Math.round(60 * LAYOUT_K);     // 100

// ==================== 行驶方向左右屏约定 ====================
// ★ 未倒行（isReversed() === false，即列车朝模型"车头"方向运行）时，
//   模型哪一侧 LCD 位于"行驶方向左侧"："L" = LCD_BASE_L（+X）侧，"R" = LCD_BASE_R（-X）侧。
// 校准说明：按 MTR 官方模型约定（+X 为出库前进方向的右侧），标准模型的
//   行驶方向左屏应为 "R"；但侧别最终取决于模型实际制作。装车后观察一次：
//   若行驶中轮播屏出现在行驶方向右侧，把本值改为 "R" 即可。
//   该校准一次性对【所有线路、往返两个方向】生效——折返换边由
//   isReversed() 自动处理，不在此常量职责范围内。
var TRAVEL_LEFT_MODEL_SIDE_WHEN_FORWARD = "L";

// ==================== 字体 ====================
var FONT_SANS_LAO = Resources.readFont(Resources.idr("fonts/sans_lao/sans_lao.ttf"));
var FONT_HAN_SANS = Resources.readFont(Resources.idr("fonts/source-han-sans/source-han-sans-medium.otf"));
var FONT_ARIAL = Resources.readFont(Resources.idr("fonts/arial/arial.ttf"));
var FONT_ARIAL_BOLD = Resources.readFont(Resources.idr("fonts/arial/arialbd.ttf"));
var FONT_HONGLEIXINGSHU = Resources.readFont(Resources.idr("fonts/honglei/hongleixingshu.otf"));

// 字号按 LAYOUT_K 缩放
var FONT_VEHICLE_NUM = FONT_SANS_LAO.deriveFont(Math.round(36 * LAYOUT_K));  // 60
var FONT_BADGE = FONT_HAN_SANS.deriveFont(Math.round(18 * LAYOUT_K));         // 30

// ==================== 顶部信息栏：终点站 / 环线方向文案 ====================
// 环线方向直接采用 API（SimplifiedRoute.getCircularState，经 circular.js 判定）的数据：
//   CLOCKWISE（顺时针）→ 内环；ANTICLOCKWISE（逆时针）→ 外环。
// 依据：右侧通行线网中顺时针列车走行内侧轨道，故顺时针 = 内环。
// 若服务器约定相反，交换下面两行的显示文案即可。
var CIRC_CN = { "CLOCKWISE": "内环", "ANTICLOCKWISE": "外环" };
var CIRC_EN = { "CLOCKWISE": "Inner Loop", "ANTICLOCKWISE": "Outer Loop" };

// ==================== 线路 LOGO 配置 ====================
var ROUTE_LOGO_CONFIG = {
    "logoPath": null
};

var ROUTE_LOGO_FALLBACK_PATHS = [
    "mtr:textures/sign/logo.png",
    "mtr:textures/block/sign/logo.png",
    "mtr:textures/block/sign/logo_grayscale.png",
    "mtr:textures/block/sign/station.png",
    "mtr:wr2a03/logo.png"
];

// 预先在脚本顶层解析资源 ID。
// 注意：Resources.idr / Resources.idRelative 只能在脚本顶层作用域直接调用，
// 不能在函数（包括 IIFE）内部调用，否则会抛出
// "Cannot use idr/idRelative in functions."。
// 因此这里在顶层把每条路径解析为 Idr 对象，函数内只做读取。
var ROUTE_LOGO_PATHS = [];
if (ROUTE_LOGO_CONFIG && ROUTE_LOGO_CONFIG.logoPath) {
    ROUTE_LOGO_PATHS.push(ROUTE_LOGO_CONFIG.logoPath);
}
for (var _li = 0; _li < ROUTE_LOGO_FALLBACK_PATHS.length; _li++) {
    ROUTE_LOGO_PATHS.push(ROUTE_LOGO_FALLBACK_PATHS[_li]);
}

var ROUTE_LOGO_IDS = [];
for (var _lk = 0; _lk < ROUTE_LOGO_PATHS.length; _lk++) {
    var _lp = ROUTE_LOGO_PATHS[_lk];
    var _lidr = null;
    try {
        _lidr = Resources.idr(_lp);
    } catch (e) {
        print("[LCD] 解析 logo 资源路径失败 " + _lp + "：" + e);
    }
    if (_lidr != null) {
        ROUTE_LOGO_IDS.push({ path: _lp, idr: _lidr });
    }
}

function loadRouteLogo() {
    // Resources.readBufferedImage(Idr) 返回 BufferedImage 或 null。
    // （ScriptResourceUtil 没有 readImage 方法，故不再调用。）
    for (var k = 0; k < ROUTE_LOGO_IDS.length; k++) {
        var entry = ROUTE_LOGO_IDS[k];
        var img = tryReadImage(entry);
        if (img != null) {
            print("[LCD] 线路 logo 加载成功：" + entry.path);
            return img;
        }
    }
    print("[LCD] 所有路径均无法加载线路 logo，将只显示线路名。");
    return null;
}

// 读取已解析好的资源条目，返回图像对象或 null
function tryReadImage(entry) {
    try {
        var img = Resources.readBufferedImage(entry.idr);
        if (img != null) return img;
    } catch (e) {
        print("[LCD] readBufferedImage 失败 " + entry.path + "：" + e);
    }
    return null;
}