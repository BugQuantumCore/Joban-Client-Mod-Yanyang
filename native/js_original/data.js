// data.js
// 车站信息、换乘信息与换乘徽章绘制。
importPackage(java.awt);

// 车站信息
function getStationsFromStops(thisRouteStops) {
    var stations = [];
    var n = thisRouteStops.size();

    for (var i = 0; i < n; i++) {
        var stop = null;
        try { stop = thisRouteStops.get(i); } catch (e) { }
        if (!stop) { stations.push(emptyStation()); continue; }

        var fullName = "";
        try {
            if (stop.station != null && stop.station.name != null) fullName = "" + stop.station.name;
            else if (stop.platform != null && stop.platform.name != null) fullName = "" + stop.platform.name;
        } catch (e) { }
        var parts = splitCjkNonCjk(fullName);

        if ((!parts.cn || parts.cn.length === 0) && fullName.length > 0) {
            parts.cn = fullName;
        }

        var transfers = [];
        try {
            var routeInterchanges = stop.routeInterchanges;
            if (routeInterchanges != null) {
                var thisRouteName = (stop.route && stop.route.name) ? getNonExtraParts(stop.route.name) : "";
                for (var j = 0; j < routeInterchanges.size(); j++) {
                    var ir = routeInterchanges.get(j);
                    if (!ir || ir.name == null) continue;
                    var irName = "" + ir.name;
                    if (!isInterchangeRouteKept(irName)) continue;
                    if (getNonExtraParts(irName) === thisRouteName) continue;
                    transfers.push({ name: irName, color: safeColor(ir.color, 100, 100, 100) });
                }
            }
        } catch (e) { }

        var exits = [];
        try {
            var st = stop.station;
            if (st != null && typeof st.getExits === "function") {
                var exitList = st.getExits();
                for (var k = 0; k < exitList.size(); k++) {
                    var ex = exitList.get(k);
                    if (ex == null) continue;
                    var exName = (typeof ex.getName === "function") ? ("" + ex.getName()) : "";
                    var dests = [];
                    try {
                        var dl = (typeof ex.getDestinations === "function") ? ex.getDestinations() : null;
                        if (dl != null) for (var d = 0; d < dl.size(); d++) {
                            var dv = dl.get(d);
                            if (dv != null) dests.push("" + dv);
                        }
                    } catch (e2) { }
                    exits.push({ name: exName, destinations: dests });
                }
            }
        } catch (e) { }

        stations.push({
            id: getStationIdStr(stop),
            nameCn: parts.cn,
            nameEn: parts.en,
            transfers: transfers,
            exits: exits
        });
    }

    if (stations.length > 1) {
        var first = stations[0];
        var last = stations[stations.length - 1];
        var sameId2 = (first.id != null && last.id != null && first.id === last.id);
        var sameName = (first.nameCn && last.nameCn
            && first.nameCn.trim() === last.nameCn.trim()
            && (first.nameEn || "").trim() === (last.nameEn || "").trim());
        if (sameId2 || sameName) stations.pop();
    }
    return stations;
}

function emptyStation() {
    return { id: null, nameCn: "…", nameEn: "…", transfers: [], exits: [] };
}

// 换乘信息
function parseTransferRoute(name) {
    var clean = getNonExtraParts(name);

    // 明确按单个 "|" 拆分中英文：
    //   中文部分 = "|" 之前（如 "凌波线"）
    //   英文部分 = "|" 之后（如 "Lingbo Line"）
    // 这样显示名不会带出 "凌波线|Lingbo Line" 的英文尾巴。
    var cn = "";
    var en = "";
    var barIdx = clean.indexOf("|");
    if (barIdx >= 0) {
        cn = clean.substring(0, barIdx).trim();
        en = clean.substring(barIdx + 1).trim();
    } else {
        var parts = splitCjkNonCjk(clean);
        cn = parts.cn;
        en = parts.en;
    }
    // 若中文为空，退化为整段文本
    if (!cn && clean) cn = clean;

    var idMatch = cn.match(/^([A-Za-z]+)?(\d+)/);
    var id, letter, num, type;
    if (idMatch) {
        letter = idMatch[1] || "";
        num = parseInt(idMatch[2], 10) || 0;
        id = letter + idMatch[2];
        type = (letter === "") ? "number" : "letter-number";
    } else {
        id = cn;
        letter = "";
        num = 0;
        type = "chinese";
    }
    return { id: id, cn: cn, en: en, letter: letter, num: num, type: type };
}

function getChinesePinyinInitial(ch) {
    var map = {
        "一": "Y", "二": "E", "三": "S", "四": "S", "五": "W",
        "六": "L", "七": "Q", "八": "B", "九": "J", "十": "S",
        "东": "D", "西": "X", "南": "N", "北": "B", "中": "Z",
        "上": "S", "下": "X", "大": "D", "小": "X", "新": "X",
        "老": "L", "高": "G", "低": "D", "快": "K", "速": "S",
        "慢": "M", "机": "J", "场": "C", "车": "C", "站": "Z",
        "公": "G", "交": "J", "巴": "B", "士": "S", "观": "G",
        "光": "G", "空": "K", "捷": "J", "运": "Y", "地": "D",
        "铁": "T", "号": "H", "线": "X", "支": "Z", "延": "Y",
        "长": "C", "环": "H", "内": "N", "外": "W", "首": "S",
        "都": "D", "国": "G", "际": "J", "园": "Y", "山": "S",
        "河": "H", "湖": "H", "江": "J", "海": "H", "湾": "W",
        "桥": "Q", "路": "L", "街": "J", "门": "M", "口": "K"
    };
    return map[ch] || "\uFFFF";
}

function compareTransferRoute(a, b) {
    var order = { "number": 0, "letter-number": 1, "chinese": 2 };
    var oa = order[a.type];
    var ob = order[b.type];
    if (oa !== ob) return oa - ob;
    if (a.type === "number") return a.num - b.num;
    if (a.type === "letter-number") {
        if (a.letter !== b.letter) return a.letter < b.letter ? -1 : 1;
        return a.num - b.num;
    }
    var pa = getChinesePinyinInitial(a.cn.charAt(0));
    var pb = getChinesePinyinInitial(b.cn.charAt(0));
    if (pa !== pb) return pa < pb ? -1 : 1;
    return a.cn < b.cn ? -1 : (a.cn > b.cn ? 1 : 0);
}

function wrapTextByWidth(g, text, font, maxW) {
    if (!text) return [""];
    g.setFont(font);
    var fm = g.getFontMetrics();
    if (fm.stringWidth(text) <= maxW) return [text];
    var lines = [];
    var current = "";
    for (var i = 0; i < text.length; i++) {
        var ch = text.charAt(i);
        var test = current + ch;
        if (fm.stringWidth(test) > maxW && current.length > 0) {
            lines.push(current);
            current = ch;
        } else {
            current = test;
        }
    }
    if (current.length > 0) lines.push(current);
    return lines;
}

function drawPartialTransferInfo(g, curIdx, stations) {
    var transfers = stations[curIdx].transfers;
    if (!transfers || transfers.length === 0) return;

    var items = [];
    for (var i = 0; i < transfers.length; i++) {
        var item = parseTransferRoute(transfers[i].name);
        item.color = transfers[i].color;
        items.push(item);
    }
    items.sort(compareTransferRoute);

    // 注意：必须显示在顶部信息栏以下的白色区域
    var titleX = 40;
    var titleY = HEADER_H + Math.round(48 * LAYOUT_K);   // 217 + 80 = 297

    g.setFont(FONT_HAN_SANS.deriveFont(Math.round(34 * LAYOUT_K)));
    g.setColor(BLACK_COLOR);
    g.drawString("换乘", titleX, titleY);
    g.setFont(FONT_ARIAL.deriveFont(Math.round(30 * LAYOUT_K)));
    g.drawString("Transfer", titleX + Math.round(108 * LAYOUT_K), titleY);

    var cols = 3;
    var badgeGap = Math.round(14 * LAYOUT_K);
    var startX = 40;
    var startY = titleY + Math.round(32 * LAYOUT_K);      // 标题下的第一行
    var availW = 480;
    var availH = TEX_H - startY - 20;

    var rows = Math.ceil(items.length / cols);
    var maxByW = Math.floor((availW - (cols - 1) * badgeGap) / cols);
    var maxByH = Math.floor((availH - (rows - 1) * badgeGap) / rows);
    var badgeSize = Math.min(maxByW, maxByH, Math.round(130 * LAYOUT_K));
    if (badgeSize < 1) badgeSize = 1;

    var badgeW = badgeSize;
    var badgeH = Math.round(badgeSize * 0.55);

    // 使用“直线线路图同款”的线路名排版；数字线大号数字用普通 Arial
    // numberCenter: 数字线也在卡片内水平居中
    var nameOpts = { scale: 1.0, numberBold: false, numberCenter: true };
    var padX = Math.round(10 * LAYOUT_K);

    for (var i = 0; i < items.length; i++) {
        var item = items[i];
        var col = i % cols;
        var row = Math.floor(i / cols);
        var bx = startX + col * (badgeW + badgeGap);
        var by = startY + row * (badgeH + badgeGap);

        // 底色与圆角形状保持不变
        g.setColor(item.color);
        g.fillRoundRect(bx, by, badgeW, badgeH, badgeH, badgeH);

        // 文字：直线线路图同款（数字线为大号数字 + 号线/英文；其余为中文 + 英文，居中）
        drawRouteNameBlock(g, bx + padX, by, badgeW - padX * 2, badgeH,
            item.cn, item.en, getContrastTextColor(item.color), nameOpts);
    }
}