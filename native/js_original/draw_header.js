// draw_header.js
// 顶部信息栏
importPackage(java.awt);

include("config.js");

/**
 * 判断线路名是否为“数字号线”格式（如 "10号线" + "Line 10"）。
 */
function isNumberRouteName(routeCn, routeEn) {
    var m = (routeCn || "").match(/^(\d+)\u53f7\u7ebf$/);
    return !!(m && routeEn && routeEn === ("Line " + m[1]));
}

/**
 * 绘制“线路名区块”（顶部信息栏与换乘徽章共用）。
 * 背景底色与形状由调用方绘制，本函数只负责文字排版：
 *  - 数字号线：左侧大号数字，右侧双行（号线 / 英文），左对齐
 *  - 非数字线（如“凌波线”）：中文行 + 英文行，在 [x, x+w] 内水平居中
 * 字号按区块高度 h 自适应，因此同一函数既可用于顶部信息栏，
 * 也可用于较小的换乘徽章。
 *
 * @param {Graphics2D} g
 * @param {Number} x,y,w,h  可用矩形（逻辑坐标）
 * @param {String} routeCn  中文线路名
 * @param {String} routeEn  英文线路名
 * @param {Color}  textColor 文字颜色（对比色）
 * @param {Object} opts     可选：{ scale, numberBold, numberFont }
 *                          scale       整体缩放（默认 1）
 *                          numberBold  数字线大号数字是否加粗（默认 true）
 */
function drawRouteNameBlock(g, x, y, w, h, routeCn, routeEn, textColor, opts) {
    if (!routeCn && !routeEn) return;
    opts = opts || {};
    var scale = opts.scale && opts.scale > 0 ? opts.scale : 1;
    var numberBold = (opts.numberBold === undefined) ? true : !!opts.numberBold;
    // 数字线是否在给定区域内水平居中（顶部信息栏保持左对齐；换乘徽章居中）
    var numberCenter = !!opts.numberCenter;

    // 垂直居中于 [y, y+h]
    var cy = y + h / 2;

    if (isNumberRouteName(routeCn, routeEn)) {
        // ---------- 数字号线：大号数字 + （号线 / 英文） ----------
        var m = routeCn.match(/^(\d+)\u53f7\u7ebf$/);
        var numStr = m[1];

        // 字号按块高自适应
        var numSize = Math.max(1, Math.round(h * 0.62 * scale));
        var suffixSize = Math.max(1, Math.round(h * 0.26 * scale));
        var enSize = Math.max(1, Math.round(h * 0.20 * scale));

        var numFont = (numberBold ? FONT_ARIAL_BOLD : FONT_ARIAL).deriveFont(numSize);
        var suffixFont = FONT_HAN_SANS.deriveFont(suffixSize);
        var enFont = FONT_ARIAL.deriveFont(enSize);

        g.setFont(numFont);
        var numFm = g.getFontMetrics();
        var numW = numFm.stringWidth(numStr);

        g.setFont(suffixFont);
        var suffixFm = g.getFontMetrics();
        var suffixW = suffixFm.stringWidth("\u53f7\u7ebf");
        g.setFont(enFont);
        var enFm = g.getFontMetrics();
        var enW = enFm.stringWidth(routeEn || "");

        var lineGap = Math.max(1, Math.round(h * 0.02 * scale));
        var suffixH = suffixFm.getAscent() + suffixFm.getDescent();
        var enH = enFm.getAscent() + enFm.getDescent();
        var blockH = suffixH + lineGap + enH;

        var gapNX = Math.round(h * 0.06 * scale);
        var rightW = Math.max(suffixW, enW);
        var totalW = numW + gapNX + rightW;

        // 起始 X：居中模式则整体在 [x, x+w] 内居中；否则左对齐于 x
        var startX = numberCenter ? (x + (w - totalW) / 2) : x;

        var numBaseline = cy + (numFm.getAscent() - numFm.getDescent()) / 2;
        var rightX = startX + numW + gapNX;

        g.setFont(numFont);
        g.setColor(textColor);
        g.drawString(numStr, startX, numBaseline);

        var blockTop = cy - blockH / 2;
        var suffixBaseline = blockTop + suffixFm.getAscent();
        var enBaseline = blockTop + suffixH + lineGap + enFm.getAscent();

        g.setFont(suffixFont);
        g.setColor(textColor);
        g.drawString("\u53f7\u7ebf", rightX, suffixBaseline);

        g.setFont(enFont);
        g.setColor(textColor);
        g.drawString(routeEn || "", rightX, enBaseline);
    } else {
        // ---------- 非数字线：中文行 + 英文行（水平居中） ----------
        var baseCn = Math.max(1, Math.round(h * 0.30 * scale));
        var baseEn = Math.max(1, Math.round(h * 0.20 * scale));
        var centerX = x + w / 2;

        var cnFont = FONT_HAN_SANS.deriveFont(baseCn);
        g.setFont(cnFont);
        var cnFm = g.getFontMetrics();
        var cnW = cnFm.stringWidth(routeCn || "");
        var cnH = cnFm.getAscent() + cnFm.getDescent();

        var enFont2 = FONT_ARIAL.deriveFont(baseEn);
        g.setFont(enFont2);
        var enFm2 = g.getFontMetrics();
        var enW = routeEn ? enFm2.stringWidth(routeEn) : 0;
        var enH2 = (routeEn ? enFm2.getAscent() + enFm2.getDescent() : 0);

        var gap2 = routeEn ? Math.max(1, Math.round(h * 0.05 * scale)) : 0;
        var totalH2 = cnH + gap2 + enH2;
        var top2 = cy - totalH2 / 2;

        g.setFont(cnFont);
        g.setColor(textColor);
        g.drawString(routeCn || "", centerX - cnW / 2, top2 + cnFm.getAscent());

        if (routeEn) {
            g.setFont(enFont2);
            g.setColor(textColor);
            g.drawString(routeEn, centerX - enW / 2,
                top2 + cnH + gap2 + enFm2.getAscent());
        }
    }
}

/**
 * 测量“header 同款”线路名区块在给定高度下所需的宽度（不含左右内边距）。
 */
function measureRouteNameBlock(g, routeCn, routeEn, h, opts) {
    opts = opts || {};
    var scale = opts.scale && opts.scale > 0 ? opts.scale : 1;
    var numberBold = (opts.numberBold === undefined) ? true : !!opts.numberBold;

    if (isNumberRouteName(routeCn, routeEn)) {
        var m = routeCn.match(/^(\d+)\u53f7\u7ebf$/);
        var numStr = m[1];

        var numFont = (numberBold ? FONT_ARIAL_BOLD : FONT_ARIAL)
            .deriveFont(Math.max(1, Math.round(h * 0.62 * scale)));
        var suffixFont = FONT_HAN_SANS.deriveFont(Math.max(1, Math.round(h * 0.26 * scale)));
        var enFont = FONT_ARIAL.deriveFont(Math.max(1, Math.round(h * 0.20 * scale)));

        g.setFont(numFont);
        var numW = g.getFontMetrics().stringWidth(numStr);
        g.setFont(suffixFont);
        var suffixW = g.getFontMetrics().stringWidth("\u53f7\u7ebf");
        g.setFont(enFont);
        var enW = g.getFontMetrics().stringWidth(routeEn || "");

        return Math.round(numW + Math.round(h * 0.06 * scale) + Math.max(suffixW, enW));
    }

    // 非数字线：取中文 / 英文行较宽者
    var cnFont = FONT_HAN_SANS.deriveFont(Math.max(1, Math.round(h * 0.30 * scale)));
    var enFont2 = FONT_ARIAL.deriveFont(Math.max(1, Math.round(h * 0.20 * scale)));
    g.setFont(cnFont);
    var cnW = g.getFontMetrics().stringWidth(routeCn || "");
    g.setFont(enFont2);
    var enW2 = g.getFontMetrics().stringWidth(routeEn || "");
    return Math.round(Math.max(cnW, enW2));
}

function drawHeader(g, info) {
    if (!_lcdHdrDbg) {
        _lcdHdrDbg = true;
        print("[LCD][终点站] 新版 drawHeader 已生效");
    }
    var routeColor = info.routeColor || LINE_COLOR;
    var textColor = getContrastTextColor(routeColor);
    g.setColor(routeColor);
    g.fillRect(0, 0, TEX_W, HEADER_H);
    var leftX = 24;
    if (info.routeLogo != null) {
        var lh = Math.round(HEADER_H - Math.round(24 * LAYOUT_K));
        var logoW = 0, logoH = 0;
        try { logoW = info.routeLogo.getWidth(); logoH = info.routeLogo.getHeight(); }
        catch (e) {
            try { logoW = info.routeLogo.getWidth(null); logoH = info.routeLogo.getHeight(null); }
            catch (e2) { }
        }
        if (logoW > 0 && logoH > 0 && lh > 0) {
            var lw = Math.round(lh * (logoW / logoH));
            var ly = Math.round((HEADER_H - lh) / 2);
            try { g.drawImage(info.routeLogo, Math.round(leftX), ly, lw, lh, null); }
            catch (e3) { print("[LCD] 线路 logo 绘制失败：" + e3); }
            leftX += lw + Math.round(36 * LAYOUT_K);
        }
    }
    var routeCn = info.routeNameCn || "";
    var routeEn = info.routeNameEn || "";
    var nameRightX = leftX;
    if (routeCn || routeEn) {
        var nameOpts = { scale: 1.0, numberBold: true };
        var contentW = measureRouteNameBlock(g, routeCn, routeEn, HEADER_H, nameOpts);
        drawRouteNameBlock(g, leftX, 0, contentW, HEADER_H, routeCn, routeEn, textColor, nameOpts);
        nameRightX = leftX + contentW;
    }
    var centerX = TEX_W / 2;
    var label = info.isDoorOpen ? "到达" : "下一站";
    var labelEn = info.isDoorOpen ? "Arrived" : "Next Station";
    var bigStation = info.isDoorOpen ? info.stations[info.currentIdx] : info.stations[info.nextIdx];
    // 中部文字左缘：按中/英两行较宽者计算，再留 30px 间隙
    var centerLimit = centerX;
    if (bigStation) {
        g.setFont(FONT_HAN_SANS.deriveFont(Math.round(40 * LAYOUT_K)));
        var halfCn = g.getFontMetrics().stringWidth(label + " " + bigStation.nameCn) / 2;
        g.setFont(FONT_ARIAL.deriveFont(Math.round(28 * LAYOUT_K)));
        var halfEn = g.getFontMetrics().stringWidth(labelEn + " " + bigStation.nameEn) / 2;
        centerLimit = centerX - Math.max(halfCn, halfEn) - Math.round(30 * LAYOUT_K);
    }
    drawDestinationBlock(g, info, nameRightX + Math.round(36 * LAYOUT_K), centerLimit, textColor);
    if (bigStation) {
        drawCenteredText(g, label + " " + bigStation.nameCn, centerX,
            Math.round(62 * LAYOUT_K), FONT_HAN_SANS.deriveFont(Math.round(40 * LAYOUT_K)), textColor);
        drawCenteredText(g, labelEn + " " + bigStation.nameEn, centerX,
            Math.round(102 * LAYOUT_K), FONT_ARIAL.deriveFont(Math.round(28 * LAYOUT_K)), textColor);
    }
    drawVehicleNumBox(g, info, routeColor);
}

// ==================== 诊断标记（排查用，确认显示后可删）====================
var _lcdHdrDbg = false;
var _lcdDestDbg = false;

/**
 * 顶部信息栏"终点站 / 环线方向"区块（线路名右侧）。
 * - 环线：直接采用 API 判定结果——CLOCKWISE→内环、ANTICLOCKWISE→外环
 *   （文案取 config.js 的 CIRC_CN / CIRC_EN，CircularState 缺失时兜底"环线/Loop"）；
 * - 非环线：显示"开往 终点站"（To 终点站英文名）；终点站取线路末站名，
 *   且优先使用 API 的 destinationName（为空时回退末站站名）。
 * - 中/英两行与中部"下一站"文字共用基线；可用空间不足时两行等比缩小。
 *
 * @param {Number} startX 区块左缘（线路名右缘 + 间距）
 * @param {Number} limitX 右侧边界（中部"下一站"文字左缘再留间隙），防止重叠
 */
function drawDestinationBlock(g, info, startX, limitX, textColor) {
    try {
        var isCirc = !!info.isCircular;
        var destCn = "", destEn = "";
        if (isCirc) {
            var cs = String(info.circularState || "");
            var cnMap = (typeof CIRC_CN !== "undefined") ? CIRC_CN : {};
            var enMap = (typeof CIRC_EN !== "undefined") ? CIRC_EN : {};
            destCn = cnMap[cs] || "环线";
            destEn = enMap[cs] || "Loop";
        } else {
            var last = (info.stations && info.stations.length > 0)
                ? info.stations[info.stations.length - 1] : null;
            destCn = info.destinationCn || (last ? last.nameCn : "");
            destEn = info.destinationEn || (last ? last.nameEn : "");
        }
        var cnText = isCirc ? destCn : (destCn ? "开往 " + destCn : "");
        var enText = isCirc ? destEn : (destEn ? "To " + destEn : "");

        if (!_lcdDestDbg) {
            _lcdDestDbg = true;
            print("[LCD][终点站] 区块生效。isCirc=" + isCirc + " circState=" + info.circularState
                + " destCn=[" + destCn + "] destEn=[" + destEn + "]"
                + " startX=" + Math.round(startX) + " limitX=" + Math.round(limitX));
        }
        if (!cnText && !enText) {
            print("[LCD][终点站] 无内容可显示（终点站数据为空）");
            return;
        }

        var availW = limitX - startX;
        if (availW < Math.round(60 * LAYOUT_K)) {
            print("[LCD][终点站] 空间不足(" + Math.round(availW) + "px)，跳过绘制");
            return;
        }

        var cnSize = Math.round(32 * LAYOUT_K);
        var enSize = Math.round(22 * LAYOUT_K);
        g.setFont(FONT_HAN_SANS.deriveFont(cnSize));
        var cnW = g.getFontMetrics().stringWidth(cnText);
        g.setFont(FONT_ARIAL.deriveFont(enSize));
        var enW = g.getFontMetrics().stringWidth(enText);
        var maxW = Math.max(cnW, enW);
        if (maxW > availW && maxW > 0) {
            var k = availW / maxW;
            cnSize = Math.max(1, Math.floor(cnSize * k));
            enSize = Math.max(1, Math.floor(enSize * k));
        }

        var cnBase = Math.round(62 * LAYOUT_K);    // 与中部中文行同基线
        var enBase = Math.round(102 * LAYOUT_K);   // 与中部英文行同基线
        if (cnText) {
            g.setFont(FONT_HAN_SANS.deriveFont(cnSize));
            g.setColor(textColor);
            g.drawString(cnText, startX, cnBase);
        }
        if (enText) {
            g.setFont(FONT_ARIAL.deriveFont(enSize));
            g.setColor(textColor);
            g.drawString(enText, startX, enBase);
        }
    } catch (e) {
        print("[LCD][终点站] 绘制异常：" + e);
    }
}

/**
 * 车号显示区：底色随线路颜色变化，并带弱阴影 / 玻璃质感。
 *
 * 布局规则：
 *  - 车号卡片包裹车号文字，四周（上下左右）留有等距边距 pad；
 *  - 卡片高度保持不变，宽度按“文字宽 + 左右 pad”自适应（即拉长到四周等距）；
 *  - 卡片右边界到 LCD 右边界的距离，等于卡片上边界到 LCD 上边界的距离。
 */
function drawVehicleNumBox(g, info, routeColor) {
    var numText = info.vehicleNum || "";

    // ★ 车号为空（= 侧线名格式不正确，或该车厢无编号可显示）时：
    //   不绘制车号文字，也跳过整个"液态玻璃"卡片（阴影/底色/高光/描边）。
    if (!numText) return;

    // 车号文字的字形度量
    g.setFont(FONT_VEHICLE_NUM);
    var fm = g.getFontMetrics();
    var textW = fm.stringWidth(numText);
    var ascent = fm.getAscent();
    var descent = fm.getDescent();
    var textH = ascent + descent;

    // 卡片高度保持不变
    var boxH = Math.round(80 * LAYOUT_K);   // 133
    // 四周等距边距：由“高度不变”反推（上下等距）
    var pad = Math.max(0, Math.round((boxH - textH) / 2));

    // 宽度：文字宽 + 左右各 pad（与上下等距，因此比原底色更紧凑/贴合）
    var boxW = textW + pad * 2;

    // 垂直位置：保持居中于顶部信息栏
    var boxY = (HEADER_H - boxH) / 2;

    // 右边界到 LCD 右边界的距离 == 上边界到 LCD 上边界的距离（= boxY）
    var boxX = (TEX_W - boxY) - boxW;

    // 文字垂直居中于卡片
    var baseY = boxY + (boxH + ascent - descent) / 2;
    var textCx = boxX + boxW / 2;

    var radius = 14;

    // ---------- 弱阴影 ----------
    try {
        var shSteps = 6;
        for (var s = shSteps; s >= 1; s--) {
            var alpha = Math.round(28 * (1 - (s - 1) / shSteps));
            g.setColor(clra(0, 0, 0, alpha));
            g.fillRoundRect(boxX - s, boxY - s + 3, boxW + s * 2, boxH + s * 2,
                radius + s, radius + s);
        }
    } catch (e) { }

    // ---------- 卡片底色（随线路颜色变化，不透明，避免透出阴影） ----------
    g.setColor(routeColor);
    g.fillRoundRect(boxX, boxY, boxW, boxH, radius, radius);

    // ---------- 顶部高光（磨砂 / 液态玻璃质感） ----------
    try {
        var hl = clra(255, 255, 255, 60);
        g.setColor(hl);
        g.fillRoundRect(boxX + 3, boxY + 3, boxW - 6, Math.round(boxH * 0.45),
            radius, radius);
        // 更亮的一条细高光，强化玻璃质感
        g.setColor(clra(255, 255, 255, 90));
        g.fillRoundRect(boxX + 3, boxY + 3, boxW - 6, Math.round(boxH * 0.16),
            radius, radius);
    } catch (e) { }

    // 描边（浅色描边模拟玻璃边缘）
    try {
        g.setColor(clra(255, 255, 255, 130));
        g.setStroke(new java.awt.BasicStroke(2));
        g.drawRoundRect(boxX, boxY, boxW, boxH, radius, radius);
    } catch (e) { }

    // 车号文字（对底色取对比色）
    g.setFont(FONT_VEHICLE_NUM);
    g.setColor(getContrastTextColor(routeColor));
    g.drawString(numText, textCx - textW / 2, baseY);
}