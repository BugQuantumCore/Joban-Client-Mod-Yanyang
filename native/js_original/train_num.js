// train_num.js
importPackage(java.awt);
importPackage(java.awt.geom);

include("draw_num.js");
include("train_num_util.js");

const num_leftPos = [[1.470637, 1.353106, 1.75], [1.479363, 0.353144, 1.75], [1.479363, 0.353144, -1.75], [1.470637, 1.353106, -1.75]];
const num_rightPos = [[-1.470637, 1.353106, -1.75], [-1.479363, 0.353144, -1.75], [-1.479363, 0.353144, 1.75], [-1.470637, 1.353106, 1.75]];
const vehicle_num_forwardsPos = getCubeVertices([-1.4375, 0.625, -10.4875], [1.4375, 0, -10.4875], [0, 0.3125, -10.4875], -10, 0, 0);
const vehicle_num_backwardsPos = getCubeVertices([1.4375, 0.625, 10.4875], [-1.4375, 0, 10.4875], [0, 0.3125, 10.4875], 10, 0, 0);

let num_slotCfg = {
    "version": 1,
    "texSize": [1120, 240],
    "slots": [
        { "name": "num_left", "texArea": [0, 0, 1120, 240], "pos": [num_leftPos], "offsets": [[0, 0, 0]] },
        { "name": "num_right", "texArea": [0, 0, 1120, 240], "pos": [num_rightPos], "offsets": [[0, 0, 0]] }
    ]
};

let vehicleNumForwards_slotCfg = {
    "version": 1,
    "texSize": [1120, 240],
    "slots": [
        { "name": "vehicle_num_forwards", "texArea": [0, 0, 1120, 240], "pos": [vehicle_num_forwardsPos], "offsets": [[0, 0, 0]] }
    ]
};

let vehicleNumBackwards_slotCfg = {
    "version": 1,
    "texSize": [1120, 240],
    "slots": [
        { "name": "vehicle_num_backwards", "texArea": [0, 0, 1120, 240], "pos": [vehicle_num_backwardsPos], "offsets": [[0, 0, 0]] }
    ]
};

var num_dhBase = new DisplayHelper(num_slotCfg);
var vehicleNumForwards_dhBase = new DisplayHelper(vehicleNumForwards_slotCfg);
var vehicleNumBackwards_dhBase = new DisplayHelper(vehicleNumBackwards_slotCfg);

// ==================== 透明清空辅助 ====================

/**
 * 用 AlphaComposite.CLEAR 清空纹理，得到完全透明背景
 * （替代原来的白底填充）
 */
function clearTextureTransparent(g) {
    if (!g) return;
    try {
        g.setComposite(java.awt.AlphaComposite.getInstance(
            java.awt.AlphaComposite.CLEAR, 1.0));
        g.fillRect(0, 0, 1120, 240);
        g.setComposite(java.awt.AlphaComposite.getInstance(
            java.awt.AlphaComposite.SRC_OVER, 1.0));
    } catch (e) {
        // fallback：用打包 int 的构造创建 alpha=0 的透明色
        try {
            g.setComposite(java.awt.AlphaComposite.getInstance(
                java.awt.AlphaComposite.SRC_OVER, 1.0));
            g.setColor(new java.awt.Color(0x00000000, true));
            g.fillRect(0, 0, 1120, 240);
        } catch (e2) { }
    }
}

function clearDhTexture(dh, slotName) {
    if (!dh) return;
    try {
        var g = dh.graphicsFor(slotName);
        if (!g) return;
        clearTextureTransparent(g);
    } catch (e) { }
}

// ==================== 生命周期 ====================

function create(ctx, state, vehicle) {
    var carCount = 0;
    try { carCount = vehicle.getCarCount(); } catch (e) { }

    state.carDhsNum = [];
    for (var i = 0; i < carCount; i++) {
        state.carDhsNum.push(num_dhBase.create());
    }
    state.dhForwards = vehicleNumForwards_dhBase.create();
    state.dhBackwards = vehicleNumBackwards_dhBase.create();

    // 初始清成透明
    for (var i = 0; i < state.carDhsNum.length; i++) {
        clearDhTexture(state.carDhsNum[i], "num_left");
        clearDhTexture(state.carDhsNum[i], "num_right");
        try { state.carDhsNum[i].upload(); } catch (e) { }
    }
    clearDhTexture(state.dhForwards, "vehicle_num_forwards");
    try { state.dhForwards.upload(); } catch (e) { }
    clearDhTexture(state.dhBackwards, "vehicle_num_backwards");
    try { state.dhBackwards.upload(); } catch (e) { }

    state.sidingNumBefore = null;
}

function render(ctx, state, vehicle) {
    var carCount = 0;
    try { carCount = vehicle.getCarCount(); } catch (e) { }

    // 车厢数变化时重建 DH
    if (!state.carDhsNum || state.carDhsNum.length !== carCount) {
        if (state.carDhsNum) for (var k = 0; k < state.carDhsNum.length; k++) {
            try { state.carDhsNum[k].close(); } catch (e) { }
        }
        state.carDhsNum = [];
        for (var k = 0; k < carCount; k++) {
            state.carDhsNum.push(num_dhBase.create());
        }
        state.sidingNumBefore = null;
    }

    // 读取侧线名（用 "" + 强制转原生 JS 字符串）
    var sidingNameNow = "";
    try {
        var sid = vehicle.getSiding();
        if (sid != null) {
            var rawName = sid.getName();
            if (rawName != null) sidingNameNow = "" + rawName;
        }
    } catch (e) { }

    // 只在侧线名变化时更新纹理内容
    if (sidingNameNow.length > 0 && state.sidingNumBefore !== sidingNameNow) {
        state.sidingNumBefore = sidingNameNow;

        var parsed = parseSidingName(sidingNameNow);
        var vehicleNum = parsed.vehicleNum;
        var carNum = parsed.carNums;

        print("本列车车号为：" + vehicleNum + "，每节车厢的编号分别是：");
        for (var i = 0; i < carNum.length; i++) print(carNum[i]);

        for (var i = 0; i < carCount; i++) {
            var dh = state.carDhsNum[i];
            if (!dh) continue;

            var gL = dh.graphicsFor("num_left");
            var gR = dh.graphicsFor("num_right");
            if (!gL || !gR) continue;

            // ★ 透明清空（替换原来的白色填充）
            clearTextureTransparent(gL);
            clearTextureTransparent(gR);

            if (carCount !== carNum.length) {
                drawError(gL);
                drawError(gR);
            } else {
                drawNum(gL, carNum[i], 60);
                drawNum(gR, carNum[i], 60);
            }
            try { dh.upload(); } catch (e) { }
        }

        // 车头牌
        if (state.dhForwards) {
            var gF = state.dhForwards.graphicsFor("vehicle_num_forwards");
            if (gF) {
                clearTextureTransparent(gF);
                drawNum(gF, vehicleNum, 30);
                try { state.dhForwards.upload(); } catch (e) { }
            }
        }
        // 车尾牌
        if (state.dhBackwards) {
            var gB = state.dhBackwards.graphicsFor("vehicle_num_backwards");
            if (gB) {
                clearTextureTransparent(gB);
                drawNum(gB, vehicleNum, 30);
                try { state.dhBackwards.upload(); } catch (e) { }
            }
        }
    }

    // 每帧把模型挂到车辆
    for (var i = 0; i < carCount; i++) {
        if (state.carDhsNum[i]) {
            try { ctx.drawCarModel(state.carDhsNum[i].model, i, null); } catch (e) { }
        }
    }
    if (state.dhForwards) {
        try { ctx.drawCarModel(state.dhForwards.model, 0, null); } catch (e) { }
    }
    if (state.dhBackwards && carCount > 0) {
        try { ctx.drawCarModel(state.dhBackwards.model, carCount - 1, null); } catch (e) { }
    }
}

function dispose(ctx, state, vehicle) {
    if (state && state.carDhsNum) {
        for (var i = 0; i < state.carDhsNum.length; i++) {
            try { state.carDhsNum[i].close(); } catch (e) { }
        }
        state.carDhsNum = null;
    }
    if (state && state.dhForwards) {
        try { state.dhForwards.close(); } catch (e) { }
        state.dhForwards = null;
    }
    if (state && state.dhBackwards) {
        try { state.dhBackwards.close(); } catch (e) { }
        state.dhBackwards = null;
    }
}

// ==================== 工具 ====================

function getCubeVertices(p1, p2, center, rx, ry, rz) {
    const rxRad = rx * Math.PI / 180;
    const ryRad = ry * Math.PI / 180;
    const rzRad = rz * Math.PI / 180;

    let c = new Vector3f(center[0], center[1], center[2]);

    if (p1[1] == p2[1]) {
        let v1 = new Vector3f(p1[0], p1[1], p1[2]);
        let v2 = new Vector3f(p2[0], p1[1], p1[2]);
        let v3 = new Vector3f(p2[0], p2[1], p2[2]);
        let v4 = new Vector3f(p1[0], p2[1], p2[2]);

        v1.sub(c); v1.rotX(rxRad); v1.rotY(ryRad); v1.rotZ(rzRad); v1.add(c);
        v2.sub(c); v2.rotX(rxRad); v2.rotY(ryRad); v2.rotZ(rzRad); v2.add(c);
        v3.sub(c); v3.rotX(rxRad); v3.rotY(ryRad); v3.rotZ(rzRad); v3.add(c);
        v4.sub(c); v4.rotX(rxRad); v4.rotY(ryRad); v4.rotZ(rzRad); v4.add(c);

        return [
            [v1.x(), v1.y(), v1.z()],
            [v2.x(), v2.y(), v2.z()],
            [v3.x(), v3.y(), v3.z()],
            [v4.x(), v4.y(), v4.z()]
        ];
    }
    else if (p1[0] == p2[0]) {
        let v1 = new Vector3f(p1[0], p1[1], p1[2]);
        let v2 = new Vector3f(p1[0], p2[1], p1[2]);
        let v3 = new Vector3f(p2[0], p2[1], p2[2]);
        let v4 = new Vector3f(p2[0], p1[1], p2[2]);

        v1.sub(c); v1.rotX(rxRad); v1.rotY(ryRad); v1.rotZ(rzRad); v1.add(c);
        v2.sub(c); v2.rotX(rxRad); v2.rotY(ryRad); v2.rotZ(rzRad); v2.add(c);
        v3.sub(c); v3.rotX(rxRad); v3.rotY(ryRad); v3.rotZ(rzRad); v3.add(c);
        v4.sub(c); v4.rotX(rxRad); v4.rotY(ryRad); v4.rotZ(rzRad); v4.add(c);

        return [
            [v1.x(), v1.y(), v1.z()],
            [v2.x(), v2.y(), v2.z()],
            [v3.x(), v3.y(), v3.z()],
            [v4.x(), v4.y(), v4.z()]
        ];
    }
    else if (p1[2] == p2[2]) {
        let v1 = new Vector3f(p1[0], p1[1], p1[2]);
        let v2 = new Vector3f(p1[0], p1[1], p2[2]);
        let v3 = new Vector3f(p2[0], p2[1], p2[2]);
        let v4 = new Vector3f(p2[0], p2[1], p1[2]);

        v1.sub(c); v1.rotX(rxRad); v1.rotY(ryRad); v1.rotZ(rzRad); v1.add(c);
        v2.sub(c); v2.rotX(rxRad); v2.rotY(ryRad); v2.rotZ(rzRad); v2.add(c);
        v3.sub(c); v3.rotX(rxRad); v3.rotY(ryRad); v3.rotZ(rzRad); v3.add(c);
        v4.sub(c); v4.rotX(rxRad); v4.rotY(ryRad); v4.rotZ(rzRad); v4.add(c);

        return [
            [v1.x(), v1.y(), v1.z()],
            [v2.x(), v2.y(), v2.z()],
            [v3.x(), v3.y(), v3.z()],
            [v4.x(), v4.y(), v4.z()]
        ];
    }
    else {
        throw "指定对角顶点不与地面垂直或平行：顶点一 [" + p1 + "]，顶点二 [" + p2 + "]。";
    }
}