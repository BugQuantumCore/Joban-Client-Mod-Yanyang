// draw_linear.js
// 非环线分支：直线线路图 + 部分线路图。
importPackage(java.awt);
importPackage(java.awt.geom);

// 完整线路图（非环线）：所有站点在一条水平直线上等距排开
function drawLinearFullMap(g, state, info) {
    var stations = info.stations;
    var n = stations.length;

    var routeColor = info.routeColor || LINE_COLOR;

    // 高亮站：开门时为当前站，否则为下一站
    var highlightIdx = info.isDoorOpen ? info.currentIdx : info.nextIdx;

    var lineY = (RING_TOP_Y + RING_BOTTOM_Y) / 2;
    var startX = RING_LEFT_X + RING_PADDING;
    var endX = RING_RIGHT_X - RING_PADDING;
    var rowW = endX - startX;

    // ==================== 自适应字号 ====================
    var spacing = n > 1 ? rowW / (n - 1) : rowW;
    var availW = spacing * 0.98;

    var baseCnSize = Math.round(32 * LAYOUT_K);   // 53
    var baseEnSize = Math.round(26 * LAYOUT_K);   // 43

    g.setFont(FONT_HAN_SANS.deriveFont(baseCnSize));
    var maxCnW = 0;
    for (var i = 0; i < n; i++) {
        var w = g.getFontMetrics().stringWidth(stations[i].nameCn || "");
        if (w > maxCnW) maxCnW = w;
    }
    g.setFont(FONT_ARIAL.deriveFont(baseEnSize));
    var maxEnW = 0;
    for (var i = 0; i < n; i++) {
        var w = g.getFontMetrics().stringWidth(stations[i].nameEn || "");
        if (w > maxEnW) maxEnW = w;
    }

    var MIN_CN_SIZE = 36;
    var MIN_EN_SIZE = 28;
    var cnSize = computeAdaptiveFontSize(baseCnSize, maxCnW, availW, MIN_CN_SIZE);
    var enSize = computeAdaptiveFontSize(baseEnSize, maxEnW, availW, MIN_EN_SIZE);

    var nameFontCn = FONT_HAN_SANS.deriveFont(cnSize);
    var nameFontEn = FONT_ARIAL.deriveFont(enSize);

    // ==================== 轨道 ====================
    // 整条灰色轨道
    g.setStroke(new java.awt.BasicStroke(RING_STROKE,
        java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
    g.setColor(GRAY_COLOR);
    g.drawLine(startX, lineY, endX, lineY);

    // 非环线：仅"当前站 → 终点站"段涂线路色
    if (info.currentIdx < n - 1) {
        var pStart = getLinearStationPos(info.currentIdx, n);
        var pEnd = getLinearStationPos(n - 1, n);
        g.setColor(routeColor);
        g.drawLine(pStart.x, lineY, pEnd.x, lineY);
    }

    // 进度闪烁叠加
    if (!info.isDoorOpen) drawLinearProgressOverlay(g, state, info);

    // ==================== 绘制循环 ====================
    for (var i = 0; i < n; i++) {
        var pos = getLinearStationPos(i, n);
        var r = Math.round(14 * LAYOUT_K);   // 23

        // 非环线配色：
        //   已过站 = 白点 + 灰名
        //   高亮站 = 红点 + 线路色名
        //   未到站 = 绿点 + 黑名
        var fillColor, nameColor;
        if (i < highlightIdx) {
            fillColor = WHITE_COLOR; nameColor = DOT_GRAY;
        } else if (i === highlightIdx) {
            fillColor = RED_COLOR; nameColor = routeColor;
        } else {
            fillColor = GREEN_COLOR; nameColor = BLACK_COLOR;
        }

        g.setColor(fillColor);
        g.fillOval(pos.x - r, pos.y - r, r * 2, r * 2);
        g.setColor(BLACK_COLOR);
        g.setStroke(new java.awt.BasicStroke(3));
        g.drawOval(pos.x - r, pos.y - r, r * 2, r * 2);

        // 站名：奇偶交替上下，避免拥挤
        // 上方站点：中文在外(更靠上)、英文在内(更靠下)
        // 下方站点：中文在内(更靠上)、英文在外(更靠下)
        //
        // 为使“上下方站名的字形边缘到线路边缘的距离相等”，
        // 这里基于字形度量（ascent / descent）而非固定基线偏移来定位：
        //   内行字形边缘   ↔ 线路边缘      间距 = EDGE_GAP
        //   外行字形边缘   ↔ 内行字形边缘  间距 = LINE_GAP
        var cnFm = g.getFontMetrics(nameFontCn);
        var enFm = g.getFontMetrics(nameFontEn);
        var cnAscent = cnFm.getAscent();
        var cnDescent = cnFm.getDescent();
        var enDescent = enFm.getDescent();
        // 英文的大写字母实际高度（ascent 含较多空白，用 capHeight 更贴近真实字形边缘）
        var enCapHeight = Math.round(enSize * 0.72);

        var halfTrack = RING_STROKE / 2;
        var edgeGap = Math.round(12 * LAYOUT_K);   // 字形边缘到线路边缘的间距（20）
        var lineGap = Math.round(2 * LAYOUT_K);    // 内外两行字形边缘之间的间距（3）

        var cnY, enY;
        if (pos.above) {
            // 内行 = 英文：英文底部(基线+descent)距线路上边缘 edgeGap
            var enBaseline = pos.y - halfTrack - enDescent - edgeGap;
            // 外行 = 中文：中文底部(基线+descent)在内行英文字形顶部之上 lineGap
            var cnBaseline = enBaseline - enCapHeight - lineGap - cnDescent;
            cnY = cnBaseline;
            enY = enBaseline;
        } else {
            // 内行 = 中文：中文顶部(基线-ascent)距线路下边缘 edgeGap
            var cnBaseline2 = pos.y + halfTrack + cnAscent + edgeGap;
            // 外行 = 英文：英文字形顶部在内行中文底部(cnDescent)之下 lineGap
            var enBaseline2 = cnBaseline2 + cnDescent + lineGap + enCapHeight;
            cnY = cnBaseline2;
            enY = enBaseline2;
        }

        drawAdaptiveCenteredText(g, stations[i].nameCn, pos.x, cnY,
            nameFontCn, availW, nameColor);
        drawAdaptiveCenteredText(g, stations[i].nameEn, pos.x, enY,
            nameFontEn, availW, nameColor);

        if (stations[i].transfers && stations[i].transfers.length > 0) {
            drawLinearTransferBadges(g, stations[i].transfers, pos);
        }
    }
}

// 非环线完整图的进度闪烁叠加（当前站 → 下一站 的水平线段）
function drawLinearProgressOverlay(g, state, info) {
    var n = info.stations.length;
    var p1 = getLinearStationPos(info.currentIdx, n);
    var p2 = getLinearStationPos(info.nextIdx, n);

    // 与轨道同宽并自适应：黑边略宽于轨道，闪烁填充略窄于轨道
    var borderStroke = RING_STROKE + Math.round(3 * LAYOUT_K);
    var fillStroke = RING_STROKE;

    g.setStroke(new java.awt.BasicStroke(borderStroke,
        java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
    g.setColor(BLACK_COLOR);
    g.drawLine(p1.x, p1.y, p2.x, p2.y);

    g.setStroke(new java.awt.BasicStroke(fillStroke,
        java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
    g.setColor(state.blinkState ? BLINK_GREEN : BLINK_RED);
    g.drawLine(p1.x, p1.y, p2.x, p2.y);
}

// 非环线完整图的换乘线路（竖向排列，从上向下）
// 排布顺序：数字从小到大 → 首字母从小到大 → 拼音首字母从小到大
// 显示样式：与顶部信息栏同款（数字号线显示“大号数字 + 号线/英文”，
//           其他线路显示“中文名 + 英文名”），底色与圆角形状保持不变。
function drawLinearTransferBadges(g, transfers, pos) {
    var items = [];
    for (var i = 0; i < transfers.length; i++) {
        var parsed = parseTransferRoute(transfers[i].name);
        parsed.color = transfers[i].color;
        items.push(parsed);
    }
    items.sort(compareTransferRoute);

    // 压缩色块高度、色块间距以及与站点线的距离，以便容纳更多换乘线路
    var badgeH = Math.round(30 * LAYOUT_K);    // 50
    var gap = Math.round(4 * LAYOUT_K);         // 7
    var padX = Math.round(14 * LAYOUT_K);       // 23
    var lineY = pos.y;
    var step = badgeH + gap;

    // 换乘徽章的数字线大号数字使用普通 Arial（非加粗），并在徽章内水平居中
    var nameOpts = { scale: 1.0, numberBold: false, numberCenter: true };

    // 计算每个徽章的宽度（根据 header 同款内容）
    var widths = [];
    for (var k = 0; k < items.length; k++) {
        widths.push(measureRouteNameBlock(g, items[k].cn, items[k].en, badgeH, nameOpts) + padX * 2);
    }

    var topGap = Math.round(16 * LAYOUT_K);   // 换乘块与站点线的间距（压缩）
    var startY;
    if (pos.above) {
        // 站名在上、换乘在线下方：从线下第一行开始向下
        startY = lineY + topGap;
    } else {
        // 站名在下、换乘在线上方：整块底边贴近站点线，列表从上向下
        startY = lineY - topGap - (items.length - 1) * step - badgeH;
    }

    for (var j = 0; j < items.length; j++) {
        var it = items[j];
        var w = widths[j];
        var y = startY + j * step;
        var x = pos.x - w / 2;

        // 底色：换乘线路颜色（形状不变：圆角矩形）
        g.setColor(it.color);
        g.fillRoundRect(x, y, w, badgeH, badgeH, badgeH);

        // 文字颜色：对底色取对比色
        var textColor = getContrastTextColor(it.color);
        drawRouteNameBlock(g, x + padX, y, w - padX * 2, badgeH,
            it.cn, it.en, textColor, nameOpts);
    }
}

// 部分线路图
function drawLinearPartialMap(g, state, info) {
    var stations = info.stations;
    var n = stations.length;
    var curIdx = info.currentIdx;
    var nxtIdx = info.nextIdx;

    var routeColor = info.routeColor || LINE_COLOR;

    if (info.isDoorOpen) {
        var isTerminus = (curIdx >= n - 1);
        var whiteCenterY = (HEADER_H + TEX_H) / 2;
        var bigNameCenterY = isTerminus ? (whiteCenterY - 50) : whiteCenterY;
        drawBigStationName(g, stations[curIdx], bigNameCenterY, routeColor);
        if (isTerminus) {
            drawTerminusMessage(g, bigNameCenterY + 140);
        }
        drawPartialTransferInfo(g, curIdx, stations);
        drawExitInfo(g, curIdx, stations, routeColor);
        return;
    }

    var lineY = Math.round(300 * LAYOUT_K);   // 500
    var lineL = 500;
    var lineR = TEX_W - 500;

    g.setColor(GRAY_COLOR);
    g.setStroke(new java.awt.BasicStroke(6));
    g.drawLine(lineL, lineY, lineR, lineY);

    var rectX = lineL + 30;
    var rectW = lineR - lineL - 60;
    var rectH = Math.round(64 * LAYOUT_K);   // 107

    g.setColor(GRAY_COLOR);
    g.fillRoundRect(rectX, lineY - rectH / 2, rectW, rectH, rectH, rectH);

    // 非环线：从 max(0, nxtIdx-2) 起显示，最多 5 站
    var displayIdx = [];
    var startIdx = Math.max(0, nxtIdx - 2);
    var endIdx = Math.min(n - 1, startIdx + 4);
    if (endIdx - startIdx < 4) startIdx = Math.max(0, endIdx - 4);
    var cnt = endIdx - startIdx + 1;
    for (var k = 0; k < cnt; k++) displayIdx.push(startIdx + k);
    var spacing = rectW / (cnt + 1);
    var count = displayIdx.length;

    var curLocalIdx = -1;
    for (var k = 0; k < count; k++) {
        if (displayIdx[k] === curIdx) { curLocalIdx = k; break; }
    }

    // 非环线：仅"未经过段"涂蓝
    var boundaryX = -1;
    if (curIdx <= 0) {
        g.setColor(routeColor);
        g.fillRoundRect(rectX, lineY - rectH / 2, rectW, rectH, rectH, rectH);
    } else if (curLocalIdx >= 0 && curLocalIdx < count) {
        boundaryX = rectX + (curLocalIdx + 1) * spacing;
        var oldClip = g.getClip();
        try {
            g.clip(new java.awt.geom.Rectangle2D.Double(
                boundaryX, lineY - rectH / 2, rectX + rectW - boundaryX, rectH));
            g.setColor(routeColor);
            g.fillRoundRect(rectX, lineY - rectH / 2, rectW, rectH, rectH, rectH);
        } catch (e) { }
        g.setClip(oldClip);
    } else {
        g.setColor(routeColor);
        g.fillRoundRect(rectX, lineY - rectH / 2, rectW, rectH, rectH, rectH);
    }

    g.setColor(WHITE_COLOR);
    g.setStroke(new java.awt.BasicStroke(2));
    g.drawRoundRect(rectX, lineY - rectH / 2, rectW, rectH, rectH, rectH);

    if (boundaryX > 0) {
        var circleR = rectH / 2;
        g.setColor(routeColor);
        g.fillOval(boundaryX - circleR, lineY - circleR, circleR * 2, circleR * 2);
    }

    var nameFontCn = FONT_HAN_SANS.deriveFont(Math.round(36 * LAYOUT_K));  // 60
    var nameFontEn = FONT_ARIAL.deriveFont(Math.round(22 * LAYOUT_K));     // 37

    for (var k = 0; k < count; k++) {
        var actualIdx = displayIdx[k];
        var station = stations[actualIdx];
        if (!station) continue;
        var x = rectX + (k + 1) * spacing;
        var r = Math.round(24 * LAYOUT_K);   // 40

        var isPassed = actualIdx < curIdx;
        if (isPassed) {
            g.setColor(DEEP_GRAY_DOT);
        } else {
            g.setColor(actualIdx === nxtIdx ? RED_COLOR : GREEN_COLOR);
        }
        g.fillOval(x - r, lineY - r, r * 2, r * 2);

        var nameColor = isPassed ? DOT_GRAY : BLACK_COLOR;

        var isAbove = (k % 2 === 0);
        var aboveOff = Math.round(80 * LAYOUT_K);   // 133
        var textY1 = isAbove ? (lineY - aboveOff) : (lineY + aboveOff);
        var textY2 = textY1 + Math.round(26 * LAYOUT_K);   // +43
        drawCenteredText(g, station.nameCn, x, textY1, nameFontCn, nameColor);
        drawCenteredText(g, station.nameEn, x, textY2, nameFontEn, nameColor);
    }

    drawPartialTransferInfo(g, nxtIdx, stations);
    drawExitInfo(g, nxtIdx, stations, routeColor);
}