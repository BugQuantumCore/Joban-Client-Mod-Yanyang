// draw_circular.js
// 环线分支：完整环形线路图 + 部分线路图。
importPackage(java.awt);
importPackage(java.awt.geom);

// 完整线路图
function drawCircularFullMap(g, state, info) {
    var stations = info.stations;
    var n = stations.length;

    var routeColor = info.routeColor || LINE_COLOR;
    var reversed = String(info.circularState) === "ANTICLOCKWISE";
    var clockwise = !reversed;

    var highlightIdx = info.isDoorOpen ? info.currentIdx : info.nextIdx;

    // 整条灰色轨道
    g.setStroke(new java.awt.BasicStroke(RING_STROKE,
        java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
    g.setColor(GRAY_COLOR);
    g.draw(new java.awt.geom.RoundRectangle2D.Double(
        RING_LEFT_X, RING_TOP_Y,
        RING_RIGHT_X - RING_LEFT_X,
        RING_BOTTOM_Y - RING_TOP_Y,
        RING_RADIUS * 2, RING_RADIUS * 2
    ));

    // 环线：整条用线路色
    g.setColor(routeColor);
    g.draw(new java.awt.geom.RoundRectangle2D.Double(
        RING_LEFT_X, RING_TOP_Y,
        RING_RIGHT_X - RING_LEFT_X,
        RING_BOTTOM_Y - RING_TOP_Y,
        RING_RADIUS * 2, RING_RADIUS * 2
    ));

    if (!info.isDoorOpen) drawProgressOverlay(g, state, info, reversed, clockwise);

    // ==================== 自适应字号 ====================
    // 1. 计算顶部行 / 底部行相邻站点的最小间距
    var topStartX = RING_LEFT_X + RING_RADIUS + RING_PADDING;
    var topEndX = RING_RIGHT_X - RING_RADIUS - RING_PADDING;
    var topCount = Math.ceil(n / 2);
    var botCount = n - topCount;
    var rowW = topEndX - topStartX;

    var topSpacing = topCount > 1 ? rowW / (topCount - 1) : rowW;
    var botSpacing = botCount > 1 ? rowW / (botCount - 1) : rowW;
    var minSpacing = Math.min(topSpacing, botSpacing);

    // 每站可用宽度（预留 2% 间隙）
    var availW = minSpacing * 0.98;

    // 2. 基准字号
    var baseCnSize = Math.round(32 * LAYOUT_K);   // 53
    var baseEnSize = Math.round(26 * LAYOUT_K);   // 43

    // 3. 测量基准字号下最长文本的宽度
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

    // 4. 统一字号（中/英分别计算）
    var MIN_CN_SIZE = 36;
    var MIN_EN_SIZE = 28;
    var cnSize = computeAdaptiveFontSize(baseCnSize, maxCnW, availW, MIN_CN_SIZE);
    var enSize = computeAdaptiveFontSize(baseEnSize, maxEnW, availW, MIN_EN_SIZE);

    var nameFontCn = FONT_HAN_SANS.deriveFont(cnSize);
    var nameFontEn = FONT_ARIAL.deriveFont(enSize);

    // ==================== 绘制循环 ====================
    for (var i = 0; i < n; i++) {
        var pos = getStationPos(i, n, reversed);
        var r = Math.round(14 * LAYOUT_K);   // 23

        var fillColor = (i === highlightIdx) ? RED_COLOR : GREEN_COLOR;
        var strokeColor = BLACK_COLOR;
        var nameColor = (i === highlightIdx) ? routeColor : BLACK_COLOR;

        g.setColor(fillColor);
        g.fillOval(pos.x - r, pos.y - r, r * 2, r * 2);
        g.setColor(strokeColor);
        g.setStroke(new java.awt.BasicStroke(3));
        g.drawOval(pos.x - r, pos.y - r, r * 2, r * 2);

        // 站点名偏移
        var topCnOff = Math.round(42 * LAYOUT_K);   // 70
        var topEnOff = Math.round(20 * LAYOUT_K);   // 33
        var botCnOff = Math.round(50 * LAYOUT_K);   // 83
        var botEnOff = Math.round(72 * LAYOUT_K);   // 120

        if (pos.section === "top") {
            drawAdaptiveCenteredText(g, stations[i].nameCn, pos.x, pos.y - topCnOff,
                nameFontCn, availW, nameColor);
            drawAdaptiveCenteredText(g, stations[i].nameEn, pos.x, pos.y - topEnOff,
                nameFontEn, availW, nameColor);
        } else {
            drawAdaptiveCenteredText(g, stations[i].nameCn, pos.x, pos.y + botCnOff,
                nameFontCn, availW, nameColor);
            drawAdaptiveCenteredText(g, stations[i].nameEn, pos.x, pos.y + botEnOff,
                nameFontEn, availW, nameColor);
        }

        if (stations[i].transfers && stations[i].transfers.length > 0) {
            drawTransferBadges(g, stations[i].transfers, pos);
        }
    }
}

// 部分线路图
function drawCircularPartialMap(g, state, info) {
    var stations = info.stations;
    var n = stations.length;
    var curIdx = info.currentIdx;
    var nxtIdx = info.nextIdx;

    var routeColor = info.routeColor || LINE_COLOR;

    if (info.isDoorOpen) {
        var whiteCenterY = (HEADER_H + TEX_H) / 2;
        drawBigStationName(g, stations[curIdx], whiteCenterY, routeColor);
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

    // 环线：下一站恒在中间，前后各 2 站（环绕）
    var displayIdx = [];
    for (var k = 0; k < 5; k++) {
        var d = k - 2;
        displayIdx.push(((nxtIdx + d) % n + n) % n);
    }
    var spacing = rectW / 6;
    var count = displayIdx.length;

    // 环线：整条胶囊用线路色
    g.setColor(routeColor);
    g.fillRoundRect(rectX, lineY - rectH / 2, rectW, rectH, rectH, rectH);

    g.setColor(WHITE_COLOR);
    g.setStroke(new java.awt.BasicStroke(2));
    g.drawRoundRect(rectX, lineY - rectH / 2, rectW, rectH, rectH, rectH);

    var nameFontCn = FONT_HAN_SANS.deriveFont(Math.round(36 * LAYOUT_K));  // 60
    var nameFontEn = FONT_ARIAL.deriveFont(Math.round(22 * LAYOUT_K));     // 37

    for (var k = 0; k < count; k++) {
        var actualIdx = displayIdx[k];
        var station = stations[actualIdx];
        if (!station) continue;
        var x = rectX + (k + 1) * spacing;
        var r = Math.round(24 * LAYOUT_K);   // 40

        g.setColor(actualIdx === nxtIdx ? RED_COLOR : GREEN_COLOR);
        g.fillOval(x - r, lineY - r, r * 2, r * 2);

        var isAbove = (k % 2 === 0);
        var aboveOff = Math.round(80 * LAYOUT_K);   // 133
        var textY1 = isAbove ? (lineY - aboveOff) : (lineY + aboveOff);
        var textY2 = textY1 + Math.round(26 * LAYOUT_K);   // +43
        drawCenteredText(g, station.nameCn, x, textY1, nameFontCn, BLACK_COLOR);
        drawCenteredText(g, station.nameEn, x, textY2, nameFontEn, BLACK_COLOR);
    }

    drawPartialTransferInfo(g, nxtIdx, stations);
    drawExitInfo(g, nxtIdx, stations, routeColor);
}