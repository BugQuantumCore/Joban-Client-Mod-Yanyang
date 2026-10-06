// main.js
// LCD 主入口
importPackage(java.awt);
importPackage(java.awt.geom);

include(Resources.id("mtrsteamloco:scripts/display_helper.js"));
include("train_num_util.js");
include("config.js");
include("mtr_util.js");
include("util.js");
include("circular.js");
include("data.js");
include("draw_common.js");
include("draw_header.js");
include("draw_circular.js");
include("draw_linear.js");

// ==================== 物理自适应 ====================

// 1. 先定义 LCD 的物理四边形（模型给定，旋转后）
var LCD_POS_L = [
    [0.8627472, 2.4004532, -10.5625],
    [1.1772487, 2.1962136, -10.5625],
    [1.1772487, 2.1962136, -9.25],
    [0.8627472, 2.4004532, -9.25]
];
var LCD_POS_R = [
    [-0.8627472, 2.4004532, -9.4375],
    [-1.1772487, 2.1962136, -9.4375],
    [-1.1772487, 2.1962136, -10.75],
    [-0.8627472, 2.4004532, -10.75]
];

function computeLcdAspectFromSlot(slotCfg) {
    try {
        var p = slotCfg.slots[0].pos[0];
        var w = Math.sqrt(
            Math.pow(p[1][0] - p[0][0], 2) +
            Math.pow(p[1][1] - p[0][1], 2) +
            Math.pow(p[1][2] - p[0][2], 2)
        );
        var h = Math.sqrt(
            Math.pow(p[2][0] - p[1][0], 2) +
            Math.pow(p[2][1] - p[1][1], 2) +
            Math.pow(p[2][2] - p[1][2], 2)
        );
        return (h > 0) ? (w / h) : 3.5;
    } catch (e) {
        return 3.5;
    }
}

// 2. 用一个最小包裹对象调用 computeLcdAspectFromSlot()，
//    因为此时 SLOT_CFG_L 尚未构造完成。
var _aspectCfg = { slots: [{ pos: [LCD_POS_L] }] };
var LCD_ASPECT = computeLcdAspectFromSlot(_aspectCfg);   // ≈ 3.5

// 3. 依据物理比例反推纹理物理尺寸，并重算纵向缩放因子。
//    TEX_W / TEX_H 保持原样（2800 / 480），逻辑布局不动。
SCR_W = 3304;
SCR_H = Math.round(SCR_W / LCD_ASPECT);   // ≈ 944
SCALE_X = SCR_W / TEX_W;                    // ≈ 1.18
SCALE_Y = SCR_H / TEX_H;                    // ≈ 1.967

// 4. 用新的 SCR_W / SCR_H 构造完整 SLOT_CFG
var SLOT_CFG_L = {
    "version": 1,
    "texSize": [SCR_W, SCR_H],
    "slots": [{
        "name": "lcd_door_left",
        "texArea": [0, 0, SCR_W, SCR_H],
        "pos": [LCD_POS_L],
        "offsets": [[0, 0, 0], [0, 0, 5], [0, 0, 10], [0, 0, 15], [0, 0, 20]]
    }]
};

var SLOT_CFG_R = {
    "version": 1,
    "texSize": [SCR_W, SCR_H],
    "slots": [{
        "name": "lcd_door_right",
        "texArea": [0, 0, SCR_W, SCR_H],
        "pos": [LCD_POS_R],
        "offsets": [[0, 0, 0], [0, 0, 5], [0, 0, 10], [0, 0, 15], [0, 0, 20]]
    }]
};

var dhBaseL = new DisplayHelper(SLOT_CFG_L);
var dhBaseR = new DisplayHelper(SLOT_CFG_R);

// 生命周期
function create(ctx, state, vehicle) {
    var carCount = 0;
    try { carCount = vehicle.getCarCount(); } catch (e) { }
    state.carDhsL = [];
    state.carDhsR = [];
    for (var i = 0; i < carCount; i++) {
        state.carDhsL.push(dhBaseL.create());
        state.carDhsR.push(dhBaseR.create());
    }

    var now = nowMs();
    state.lastCycleTime = now;
    state.lastBlinkTime = now;
    state.blinkState = false;
    state.pageMode = "full";
    state._lastPageMode = "full";
    state.isDoorOpen = false;
    state._lastDoorOpen = false;
    state.cachedRouteId = null;
    state.cachedStations = null;
    state.destinationCn = "";
    state.destinationEn = "";
    state.circularState = null;
    state.isCircular = false;
    state._lastCurrentIdx = 0;

    state._stopCountIdx = -1;
    state._lastRouteIdForIdx = null;
    state._lastCountTime = 0;

    state._doorApiLogged = false;
    state._lastIdxShown = -1;

    state._isCircularLocked = false;
    state._circularChecked = false;

    state.routeColor = LINE_COLOR;
    state.routeNameCn = "";
    state.routeNameEn = "";
    state.routeLogo = loadRouteLogo();
}

function render(ctx, state, vehicle) {
    if (!state) return;

    var carCount = 0;
    try { carCount = vehicle.getCarCount(); } catch (e) { }

    if (!state.carDhsL || state.carDhsL.length !== carCount) {
        if (state.carDhsL) for (var k = 0; k < state.carDhsL.length; k++) {
            try { state.carDhsL[k].close(); } catch (e) { }
        }
        if (state.carDhsR) for (var k = 0; k < state.carDhsR.length; k++) {
            try { state.carDhsR[k].close(); } catch (e) { }
        }
        state.carDhsL = [];
        state.carDhsR = [];
        for (var k = 0; k < carCount; k++) {
            state.carDhsL.push(dhBaseL.create());
            state.carDhsR.push(dhBaseR.create());
        }
    }

    var now = nowMs();

    var sidingName = "";
    try {
        var sid = vehicle.getSiding();
        if (sid != null) sidingName = "" + sid.getName();
    } catch (e) { }

    var thisRouteStops = null;
    try { thisRouteStops = vehicle.getThisRouteStops(); } catch (e) { }
    var hasRoute = (thisRouteStops != null && thisRouteStops.size() > 0);

    if (hasRoute) {
        var firstStop = null;
        try { firstStop = thisRouteStops.get(0); } catch (e) { }
        var routeId = (firstStop && firstStop.route) ? ("" + firstStop.route.name) : "unknown";

        if (state.cachedRouteId !== routeId) {
            state.cachedRouteId = routeId;
            state.cachedStations = getStationsFromStops(thisRouteStops);

            if (!state._isCircularLocked) {
                resolveCircularState(vehicle, state);
            }

            var trainStatus = null;
            try { trainStatus = getTrainStatus(vehicle); } catch (e) { }

            var routeInfo = null;
            try { routeInfo = getRouteInfo(vehicle, trainStatus, firstStop); } catch (e) { }

            if (routeInfo != null) {
                state.routeColor = routeInfo.routeColor;

                // ★ 优先按 "|" 拆分中英文，避免 TextUtil 把 "10号线" 中的数字切到英文部分
                var cleanName = getNonExtraParts(routeInfo.routeName);
                var routeCn = "";
                var routeEn = "";

                var barIdx = cleanName.indexOf("|");
                if (barIdx >= 0) {
                    routeCn = cleanName.substring(0, barIdx).trim();
                    routeEn = cleanName.substring(barIdx + 1).trim();
                } else {
                    var parts = splitCjkNonCjk(cleanName);
                    routeCn = parts.cn || cleanName;
                    routeEn = parts.en || "";
                }
                state.routeNameCn = routeCn;
                state.routeNameEn = routeEn;
            }

            var lastStop = null;
            try { lastStop = thisRouteStops.get(thisRouteStops.size() - 1); } catch (e) { }
            var destName = "";
            try {
                if (lastStop && lastStop.destinationName != null) destName = "" + lastStop.destinationName;
            } catch (e) { }
            var destParts = splitCjkNonCjk(destName);
            state.destinationCn = destParts.cn;
            state.destinationEn = destParts.en;

            state._lastIdxShown = -1;
        }
    }

    var stations = state.cachedStations || [];
    var hasStations = stations.length > 0;

    var doorVal = getVehicleDoorValue(vehicle);
    var absSpeed = getVehicleSpeedMs(vehicle);

    if (!state._doorApiLogged) {
        state._doorApiLogged = true;
        print("[LCD] doorValue=" + doorVal.toFixed(2) + " speedMs=" + absSpeed.toFixed(2));
    }

    if (state.isDoorOpen) {
        if (doorVal < 0.05 && absSpeed > 0.3) state.isDoorOpen = false;
    } else {
        if (doorVal > 0.5 || (absSpeed < 0.1 && doorVal > 0.1)) state.isDoorOpen = true;
    }

    var cycleElapsed = now - state.lastCycleTime;
    if (state.pageMode === "full") {
        if (cycleElapsed >= CYCLE_FULL_MS) { state.pageMode = "partial"; state.lastCycleTime = now; }
    } else {
        if (cycleElapsed >= CYCLE_PARTIAL_MS) { state.pageMode = "full"; state.lastCycleTime = now; }
    }

    if (!state.isDoorOpen
        && now - state.lastBlinkTime >= BLINK_INTERVAL_MS) {
        state.blinkState = !state.blinkState;
        state.lastBlinkTime = now;
    }

    if (hasStations) {
        if (state.isDoorOpen && !state._lastDoorOpen) {
            if (now - state._lastCountTime > STOP_DEBOUNCE_MS) {
                state._stopCountIdx++;
                if (state.isCircular && stations.length > 0) {
                    state._stopCountIdx =
                        ((state._stopCountIdx % stations.length) + stations.length) % stations.length;
                } else if (state._stopCountIdx >= stations.length) {
                    state._stopCountIdx = stations.length - 1;
                }
                state._lastCountTime = now;
            }
        }
    }
    state._lastDoorOpen = state.isDoorOpen;

    var currentIdx = 0, nextIdx = 0, leftMode = "full", rightMode = "full";
    if (hasStations) {
        currentIdx = getCurrentStationIdx(vehicle, thisRouteStops, stations, state);
        currentIdx = Math.max(0, Math.min(currentIdx, stations.length - 1));
        if (state.isCircular) {
            nextIdx = (currentIdx + 1) % stations.length;
        } else {
            nextIdx = Math.min(currentIdx + 1, stations.length - 1);
        }
        // ===== 行驶方向左屏轮播、右屏固定（通用实现，与线路/方向无关）=====
        // "左屏"指行驶方向左侧。折返倒行时车体不旋转、模型侧与行驶左右关系翻转；
        // 环线/灯泡线换向时车体旋转、关系不变——isReversed() 两种情况都能覆盖。
        var ts = getTravelLeftSide(vehicle);
        state._reversedRaw = ts.reversed;
        state._travelApiOk = ts.apiOk;
        if (state._lastTravelLeftSide != null && state._lastTravelLeftSide !== ts.side) {
            print("[LCD] 行驶方向换边：左屏由模型 " + state._lastTravelLeftSide
                + " 屏 → " + ts.side + " 屏（isReversed=" + ts.reversed + "）");
        }
        state._lastTravelLeftSide = ts.side;
        var cyclingMode = state.isDoorOpen ? "partial" : state.pageMode;
        if (ts.side === "L") {
            leftMode = cyclingMode;  // 模型 L（+X）此刻在行驶方向左侧 → 轮播
            rightMode = "full";      // 模型 R（-X）此刻在行驶方向右侧 → 固定完整图
        } else {
            leftMode = "full";
            rightMode = cyclingMode;
        }
    }

    for (var ci = 0; ci < carCount; ci++) {
        var dhL = state.carDhsL[ci];
        var dhR = state.carDhsR[ci];
        if (!dhL || !dhR) continue;

        var gL = dhL.graphicsFor("lcd_door_left");
        var gR = dhR.graphicsFor("lcd_door_right");

        if (gL) {
            if (!hasRoute || !hasStations) {
                clearScreen(gL);
            } else {
                var carDisplay = getCarDisplayNum(sidingName, ci);
                var infoL = {
                    vehicleNum: carDisplay,
                    stations: stations,
                    currentIdx: currentIdx,
                    nextIdx: nextIdx,
                    isCircular: state.isCircular,
                    circularState: state.circularState,
                    destinationCn: state.destinationCn,
                    destinationEn: state.destinationEn,
                    isDoorOpen: state.isDoorOpen,
                    blinkState: state.blinkState,
                    routeColor: state.routeColor,
                    routeNameCn: state.routeNameCn,
                    routeNameEn: state.routeNameEn,
                    routeLogo: state.routeLogo
                };
                drawOneScreen(gL, state, infoL, leftMode);
            }
            try { dhL.upload(); } catch (e) { }
        }

        if (gR) {
            if (!hasRoute || !hasStations) {
                clearScreen(gR);
            } else {
                var carDisplay2 = getCarDisplayNum(sidingName, ci);
                var infoR = {
                    vehicleNum: carDisplay2,
                    stations: stations,
                    currentIdx: currentIdx,
                    nextIdx: nextIdx,
                    isCircular: state.isCircular,
                    circularState: state.circularState,
                    destinationCn: state.destinationCn,
                    destinationEn: state.destinationEn,
                    isDoorOpen: state.isDoorOpen,
                    blinkState: state.blinkState,
                    routeColor: state.routeColor,
                    routeNameCn: state.routeNameCn,
                    routeNameEn: state.routeNameEn,
                    routeLogo: state.routeLogo
                };
                drawOneScreen(gR, state, infoR, rightMode);
            }
            try { dhR.upload(); } catch (e) { }
        }

        try { ctx.drawCarModel(dhL.model, ci, null); } catch (e) { }
        try { ctx.drawCarModel(dhR.model, ci, null); } catch (e) { }
    }

    // ==================== JCM 调试信息 ====================
    // 仅在 JCM 设置中开启 Script debug mode 时才会在屏幕左上角显示
    try {
        if (typeof ctx.debugModeEnabled === "function" && ctx.debugModeEnabled()) {
            ctx.setDebugInfo("LCD Circular", state.isCircular
                + (state._isCircularLocked ? " (locked)" : ""));
            ctx.setDebugInfo("LCD CircState", state.circularState || "null");
            ctx.setDebugInfo("LCD Cur/Next", currentIdx + " / " + nextIdx);
            ctx.setDebugInfo("LCD Stations", stations.length + " stops, stopCnt=" + state._stopCountIdx);
            ctx.setDebugInfo("LCD DoorOpen", state.isDoorOpen);
            ctx.setDebugInfo("LCD PageMode", state.pageMode);
            ctx.setDebugInfo("LCD Route", state.cachedRouteId || "unknown");
            ctx.setDebugInfo("LCD RouteName", (state.routeNameCn || "") + " / " + (state.routeNameEn || ""));
            ctx.setDebugInfo("LCD RouteColor", state.routeColor
                ? ("#" + ("000000" + (state.routeColor.getRGB() & 0xFFFFFF).toString(16)).slice(-6))
                : "------");
            ctx.setDebugInfo("LCD Logo", state.routeLogo ? "OK" : "NULL");
            ctx.setDebugInfo("LCD Blink", state.blinkState);
            ctx.setDebugInfo("LCD Reversed", state._reversedRaw);
            ctx.setDebugInfo("LCD TravelLeft", state._lastTravelLeftSide || "-");
        }
    } catch (e) {
        // 静默忽略：调试模式不可用时不影响正常渲染
    }
}

function dispose(ctx, state, vehicle) {
    if (state && state.carDhsL) {
        for (var i = 0; i < state.carDhsL.length; i++) {
            try { state.carDhsL[i].close(); } catch (e) { }
        }
        state.carDhsL = null;
    }
    if (state && state.carDhsR) {
        for (var i = 0; i < state.carDhsR.length; i++) {
            try { state.carDhsR[i].close(); } catch (e) { }
        }
        state.carDhsR = null;
    }
}

// 屏幕分派
function clearScreen(g) {
    var oldT = g.getTransform();
    g.scale(SCALE_X, SCALE_Y);
    g.setColor(WHITE_COLOR);
    g.fillRect(0, 0, TEX_W, TEX_H);
    g.setTransform(oldT);
}

/**
 * 每帧对每块屏只调用一次。
 * 根据 state.isCircular 选择环线或非环线分支，二者互斥。
 */
function drawOneScreen(g, state, info, pageMode) {
    var oldT = g.getTransform();
    g.scale(SCALE_X, SCALE_Y);
    g.setColor(WHITE_COLOR);
    g.fillRect(0, 0, TEX_W, TEX_H);

    drawHeader(g, info);

    // 用本帧 info.isCircular 作为唯一判据，保证与 data 一致
    state._lastCurrentIdx = info.currentIdx;
    if (info.isCircular) {
        // 环线分支
        if (pageMode === "full") {
            drawCircularFullMap(g, state, info);
        } else {
            drawCircularPartialMap(g, state, info);
        }
    } else {
        // 非环线分支
        if (pageMode === "full") {
            drawLinearFullMap(g, state, info);
        } else {
            drawLinearPartialMap(g, state, info);
        }
    }

    g.setTransform(oldT);
}