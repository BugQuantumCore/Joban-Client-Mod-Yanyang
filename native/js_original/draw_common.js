// draw_common.js
// 居中文本、混排文本、站点位置、环线弧段、进度闪烁、换乘徽章、出站口、大站名、终点信息
importPackage(java.awt);
importPackage(java.awt.geom);

// 基础绘制
function drawCenteredText(g, text, cx, y, font, color) {
    if (!text) return;
    g.setFont(font);
    var fm = g.getFontMetrics();
    g.setColor(color);
    g.drawString(text, cx - fm.stringWidth(text) / 2, y);
}

function drawArrow(g, x, y, direction, color, size) {
    g.setColor(color);
    var path = new java.awt.geom.GeneralPath();
    switch (direction) {
        case "right":
            path.moveTo(x - size, y - size * 0.6);
            path.lineTo(x + size, y);
            path.lineTo(x - size, y + size * 0.6); break;
        case "left":
            path.moveTo(x + size, y - size * 0.6);
            path.lineTo(x - size, y);
            path.lineTo(x + size, y + size * 0.6); break;
        case "up":
            path.moveTo(x - size * 0.6, y + size);
            path.lineTo(x, y - size);
            path.lineTo(x + size * 0.6, y + size); break;
        case "down":
            path.moveTo(x - size * 0.6, y - size);
            path.lineTo(x, y + size);
            path.lineTo(x + size * 0.6, y - size); break;
    }
    path.closePath();
    g.fill(path);
}

// 混排文字
function measureMixedText(g, text, sizeCn, sizeEn) {
    if (!text) return 0;
    var s = "" + text;
    var totalW = 0;
    var i = 0;
    while (i < s.length) {
        var isCJK = /[\u3000-\u303f\u4e00-\u9fff\uff00-\uffef]/.test(s[i]);
        var j = i;
        while (j < s.length) {
            var isCJK2 = /[\u3000-\u303f\u4e00-\u9fff\uff00-\uffef]/.test(s[j]);
            if (isCJK2 !== isCJK) break;
            j++;
        }
        var seg = s.substring(i, j);
        g.setFont(isCJK ? FONT_HAN_SANS.deriveFont(sizeCn) : FONT_ARIAL.deriveFont(sizeEn));
        totalW += g.getFontMetrics().stringWidth(seg);
        i = j;
    }
    return totalW;
}

function drawMixedText(g, text, x, y, sizeCn, sizeEn, color) {
    if (!text) return 0;
    g.setColor(color);
    var s = "" + text;
    var curX = x;
    var i = 0;
    while (i < s.length) {
        var isCJK = /[\u3000-\u303f\u4e00-\u9fff\uff00-\uffef]/.test(s[i]);
        var j = i;
        while (j < s.length) {
            var isCJK2 = /[\u3000-\u303f\u4e00-\u9fff\uff00-\uffef]/.test(s[j]);
            if (isCJK2 !== isCJK) break;
            j++;
        }
        var seg = s.substring(i, j);
        g.setFont(isCJK ? FONT_HAN_SANS.deriveFont(sizeCn) : FONT_ARIAL.deriveFont(sizeEn));
        g.drawString(seg, curX, y);
        curX += g.getFontMetrics().stringWidth(seg);
        i = j;
    }
    return curX - x;
}

// 站点位置
function getStationPos(index, total, reversed) {
    var topStartX = RING_LEFT_X + RING_RADIUS + RING_PADDING;
    var topEndX = RING_RIGHT_X - RING_RADIUS - RING_PADDING;
    var topCount = Math.ceil(total / 2);
    var botCount = total - topCount;

    if (index < topCount) {
        var t = (topCount <= 1) ? 0.5 : index / (topCount - 1);
        if (reversed) t = 1 - t;
        return { x: topStartX + t * (topEndX - topStartX), y: RING_TOP_Y, section: "top" };
    } else {
        var i = index - topCount;
        var t2 = (botCount <= 1) ? 0.5 : i / (botCount - 1);
        if (reversed) t2 = 1 - t2;
        return { x: topEndX - t2 * (topEndX - topStartX), y: RING_BOTTOM_Y, section: "bottom" };
    }
}

// 直线（非环线）站点位置：所有站点在一条水平线上等距排开
// 返回 { x, y, above }，above 表示站名是否画在线上方（用于交替避让）
function getLinearStationPos(index, total) {
    var startX = RING_LEFT_X + RING_PADDING;
    var endX = RING_RIGHT_X - RING_PADDING;
    var y = (RING_TOP_Y + RING_BOTTOM_Y) / 2;

    var x;
    if (total <= 1) {
        x = (startX + endX) / 2;
    } else {
        var t = index / (total - 1);
        x = startX + t * (endX - startX);
    }
    return { x: x, y: y, above: (index % 2 === 1) };
}

// 环线弧段构造
function appendRingSegment(path, pa, pb, clockwise) {
    var sa = pa.section, sb = pb.section;
    if (sa === sb) {
        path.lineTo(pb.x, pb.y);
        return;
    }

    var useRightArc;
    if (sa === "top" && sb === "bottom") useRightArc = clockwise;
    else useRightArc = !clockwise;

    var cx, cy;
    if (useRightArc) {
        cx = RING_RIGHT_X - RING_RADIUS;
    } else {
        cx = RING_LEFT_X + RING_RADIUS;
    }
    cy = (RING_TOP_Y + RING_BOTTOM_Y) / 2;

    if (sa === "top") path.lineTo(cx, RING_TOP_Y);
    else path.lineTo(cx, RING_BOTTOM_Y);

    var steps = 24;
    if (sa === "top") {
        if (useRightArc) {
            for (var s = 1; s <= steps; s++) {
                var a = -Math.PI / 2 + (s / steps) * Math.PI;
                path.lineTo(cx + RING_RADIUS * Math.cos(a), cy + RING_RADIUS * Math.sin(a));
            }
        } else {
            for (var s2 = 1; s2 <= steps; s2++) {
                var a2 = -Math.PI / 2 - (s2 / steps) * Math.PI;
                path.lineTo(cx + RING_RADIUS * Math.cos(a2), cy + RING_RADIUS * Math.sin(a2));
            }
        }
    } else {
        if (useRightArc) {
            for (var s3 = 1; s3 <= steps; s3++) {
                var a3 = Math.PI / 2 - (s3 / steps) * Math.PI;
                path.lineTo(cx + RING_RADIUS * Math.cos(a3), cy + RING_RADIUS * Math.sin(a3));
            }
        } else {
            for (var s4 = 1; s4 <= steps; s4++) {
                var a4 = Math.PI / 2 + (s4 / steps) * Math.PI;
                path.lineTo(cx + RING_RADIUS * Math.cos(a4), cy + RING_RADIUS * Math.sin(a4));
            }
        }
    }
    path.lineTo(pb.x, pb.y);
}

// 进度闪烁
function drawArrowChar(g, ch, cx, cy) {
    var fontSize = 30;
    var font = FONT_HAN_SANS.deriveFont(fontSize);
    g.setFont(font);
    g.setColor(WHITE_COLOR);

    var fm = g.getFontMetrics();
    var textW = fm.stringWidth(ch);
    var ascent = fm.getAscent();
    var descent = fm.getDescent();

    var x = cx - textW / 2;
    var y = cy + (ascent - descent) / 2;

    g.drawString(ch, x, y);
}

function drawProgressOverlay(g, state, info, reversed, clockwise) {
    var n = info.stations.length;
    var p1 = getStationPos(info.currentIdx, n, reversed);
    var p2 = getStationPos(info.nextIdx, n, reversed);

    var path = new java.awt.geom.GeneralPath();
    path.moveTo(p1.x, p1.y);
    appendRingSegment(path, p1, p2, clockwise);

    var fillStroke = RING_STROKE;                                  // 与轨道同宽，自适应
    var borderStroke = RING_STROKE + Math.round(3 * LAYOUT_K);     // 黑边略宽于轨道

    g.setStroke(new java.awt.BasicStroke(borderStroke,
        java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
    g.setColor(BLACK_COLOR);
    g.draw(path);

    g.setStroke(new java.awt.BasicStroke(fillStroke,
        java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
    g.setColor(state.blinkState ? BLINK_GREEN : BLINK_RED);
    g.draw(path);

    var midX, midY, arrowChar;
    if (p1.section === p2.section) {
        midX = (p1.x + p2.x) / 2;
        midY = p1.y;
        arrowChar = (p2.x > p1.x) ? "→" : "←";
    } else {
        var ringCenterX = (RING_LEFT_X + RING_RIGHT_X) / 2;
        midX = ((p1.x + p2.x) / 2 > ringCenterX) ? RING_RIGHT_X : RING_LEFT_X;
        midY = (RING_TOP_Y + RING_BOTTOM_Y) / 2;
        arrowChar = (p2.section === "bottom") ? "↓" : "↑";
    }
    drawArrowChar(g, arrowChar, midX, midY);
}

// 换乘徽章
// 提取“环线图徽章”上要显示的简短线路标识：
//   - 数字线路：仅数字（如 "10号线" → "10"）
//   - 字母+数字线路：字母+数字（如 "S1线" → "S1"）
//   - 中文命名线路：仅中文名，去掉末尾的 "捷运" / "线" 等字样
function normalizeLineId(name) {
    if (!name) return "";
    var s = "" + name;

    // 先去掉英文/杂项部分（保留 "|" 之前的中文主体）
    var clean = getNonExtraParts(s);
    var barIdx = clean.indexOf("|");
    if (barIdx >= 0) clean = clean.substring(0, barIdx);
    clean = clean.trim();

    // 字母 + 数字（如 S1 / 10）
    var m = clean.match(/^([A-Za-z]*\d+)/);
    if (m) return m[1];

    // 中文命名线路：去掉末尾的 "捷运" / "线" / "号线" 等后缀
    var cn = clean.replace(/(空中)?捷运$/, "").replace(/号线$/, "").replace(/线$/, "").trim();
    return cn || clean;
}

function drawTransferBadges(g, transfers, pos) {
    var badgeH = Math.round(26 * LAYOUT_K);   // 43
    var gap = Math.round(6 * LAYOUT_K);   // 10
    var widths = [], totalW = 0;

    for (var i = 0; i < transfers.length; i++) {
        var text = normalizeLineId(transfers[i].name);
        g.setFont(FONT_BADGE);
        var tw = g.getFontMetrics().stringWidth(text) + 22;
        var w = Math.max(tw, badgeH);
        widths.push(w);
        totalW += w + gap;
    }
    totalW -= gap;

    var startX = pos.x - totalW / 2;
    var yOff = Math.round(32 * LAYOUT_K);   // 53
    var y = pos.y + (pos.section === "top" ? yOff : -yOff);

    for (var j = 0; j < transfers.length; j++) {
        var tr = transfers[j];
        var text = normalizeLineId(tr.name);
        var w = widths[j];
        var cx = startX + w / 2;
        g.setColor(tr.color);
        g.fillRoundRect(cx - w / 2, y - badgeH / 2, w, badgeH, badgeH, badgeH);
        g.setFont(FONT_BADGE);
        g.setColor(WHITE_COLOR);
        g.drawString(text, cx - g.getFontMetrics().stringWidth(text) / 2,
            y + Math.round(7 * LAYOUT_K));   // +12
        startX += w + gap;
    }
}

// 出站口信息
function drawExitInfo(g, curIdx, stations, routeColor) {
    var rc = routeColor || LINE_COLOR;
    var x = TEX_W - 420;
    var y = Math.round(178 * LAYOUT_K);   // 297

    var exits = stations[curIdx].exits;
    if (!exits || exits.length === 0) return;

    var validExits = [];
    for (var i = 0; i < exits.length; i++) {
        var e = exits[i];
        if (e.destinations && e.destinations.length > 0) validExits.push(e);
    }
    if (validExits.length === 0) return;

    drawMixedText(g, "出站口 Exits", x, y,
        Math.round(34 * LAYOUT_K), Math.round(34 * LAYOUT_K), BLACK_COLOR);  // 57

    var prefixCount = {};
    for (var j = 0; j < validExits.length; j++) {
        var nameJ = validExits[j].name || "";
        var mJ = nameJ.match(/^([A-Za-z]+)/);
        if (mJ) {
            var letterJ = mJ[1];
            prefixCount[letterJ] = (prefixCount[letterJ] || 0) + 1;
        }
    }

    for (var k = 0; k < validExits.length; k++) {
        var exit = validExits[k];
        var ey = y + Math.round(52 * LAYOUT_K) + k * Math.round(64 * LAYOUT_K);
        //      y + 87 + k*107
        var displayName = exit.name;
        var mK = (exit.name || "").match(/^([A-Za-z]+)/);
        if (mK && prefixCount[mK[1]] === 1) displayName = mK[1];

        var p = splitCjkNonCjk(exit.destinations[0]);

        g.setFont(FONT_ARIAL.deriveFont(Math.round(26 * LAYOUT_K)));    // 43
        g.setColor(rc);
        g.drawString(displayName, x, ey);

        g.setFont(FONT_HAN_SANS.deriveFont(Math.round(20 * LAYOUT_K)));  // 33
        g.setColor(BLACK_COLOR);
        g.drawString(p.cn, x + 70, ey);

        g.setFont(FONT_ARIAL.deriveFont(Math.round(17 * LAYOUT_K)));     // 28
        g.drawString(p.en, x + 70, ey + Math.round(20 * LAYOUT_K));      // +33
    }
}

// 大站名 / 终点信息
function drawBigStationName(g, station, centerY, routeColor) {
    var rc = routeColor || LINE_COLOR;
    var cnSize = Math.round(150 * LAYOUT_K);   // 250
    var enSize = Math.round(42 * LAYOUT_K);   // 70
    var gap = Math.round(24 * LAYOUT_K);   // 40
    var cnVisualH = cnSize * 0.75;
    var enVisualH = enSize * 0.7;
    var totalH = cnVisualH + gap + enVisualH;
    var top = centerY - totalH / 2;
    var cnBaseline = top + cnVisualH;
    var enBaseline = cnBaseline + gap + enVisualH;

    var cnFont = FONT_HONGLEIXINGSHU != null
        ? FONT_HONGLEIXINGSHU.deriveFont(cnSize)
        : FONT_HAN_SANS.deriveFont(cnSize);
    g.setFont(cnFont);
    g.setColor(rc);
    var cnW = g.getFontMetrics().stringWidth(station.nameCn);
    g.drawString(station.nameCn, TEX_W / 2 - cnW / 2, cnBaseline);

    g.setFont(FONT_ARIAL.deriveFont(enSize));
    g.setColor(BLACK_COLOR);
    var enW = g.getFontMetrics().stringWidth(station.nameEn);
    g.drawString(station.nameEn, TEX_W / 2 - enW / 2, enBaseline);
}

function drawTerminusMessage(g, y) {
    var cnText = "本次列车已抵达终点站，请所有乘客全部下车。";
    var enText = "This train has arrived at the terminus. Please alight from the train.";
    g.setFont(FONT_HAN_SANS.deriveFont(Math.round(26 * LAYOUT_K)));   // 43
    g.setColor(BLACK_COLOR);
    var cnW = g.getFontMetrics().stringWidth(cnText);
    g.drawString(cnText, TEX_W / 2 - cnW / 2, y);
    g.setFont(FONT_ARIAL.deriveFont(Math.round(20 * LAYOUT_K)));       // 33
    g.setColor(BLACK_COLOR);
    var enW = g.getFontMetrics().stringWidth(enText);
    g.drawString(enText, TEX_W / 2 - enW / 2, y + Math.round(36 * LAYOUT_K)); // +60
}

// ==================== 自适应字号与压缩 ====================
/**
 * 计算线路图的统一字号。
 *   1) 若基准字号下最长文字宽度 <= availW，则保持基准字号；
 *   2) 否则按比例缩小，缩到 minSize 为下限。
 *
 * @param {Number} baseSize 基准字号
 * @param {Number} maxW     基准字号下最长文本的像素宽度
 * @param {Number} availW   每站可用像素宽度
 * @param {Number} minSize  最小字号
 * @return {Number}         统一字号
 */
function computeAdaptiveFontSize(baseSize, maxW, availW, minSize) {
    if (maxW <= 0 || availW >= maxW) return baseSize;
    var targetSize = baseSize * (availW / maxW);
    if (targetSize < minSize) targetSize = minSize;
    return Math.round(targetSize);
}

/**
 * 计算单个文字的水平压缩比。
 * - 若文字宽度已能放入 availW，返回 1.0（不压缩）。
 * - 否则以 5% 为一级向下取整（即 100%、95%、90%…），最低 50%。
 *
 * @param {Number} textWidth 当前字号下文字的自然像素宽度
 * @param {Number} availW    可用像素宽度
 * @return {Number}          压缩比（0.5 ~ 1.0）
 */
function computePerTextCompressX(textWidth, availW) {
    if (textWidth <= availW || textWidth <= 0) return 1.0;

    var naturalRatio = availW / textWidth;
    // 以 5% 为一级，向下取整，+1e-9 消除浮点误差
    var steps = Math.floor(naturalRatio * 20 + 1e-9);
    if (steps < 10) steps = 10;   // 最低 50%
    if (steps > 20) steps = 20;
    return steps / 20;
}

/**
 * 绘制水平居中、可按需水平压缩的文本。
 * - 只有该文字实际宽度超过 availW 时才会被压缩。
 * - 压缩只作用于水平方向，字号/字高不变。
 */
function drawAdaptiveCenteredText(g, text, cx, y, font, availW, color) {
    if (!text) return;
    g.setFont(font);
    var fm = g.getFontMetrics();
    var origW = fm.stringWidth(text);
    var compressX = computePerTextCompressX(origW, availW);
    var drawW = origW * compressX;

    var oldT = g.getTransform();
    g.translate(cx - drawW / 2, y);
    g.scale(compressX, 1.0);
    g.setColor(color);
    g.drawString(text, 0, 0);
    g.setTransform(oldT);
}