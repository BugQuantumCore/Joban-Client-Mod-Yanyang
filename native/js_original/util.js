// util.js
// 时间、文本拆分、ID、车号、车辆状态、当前站索引
importPackage(java.awt);

// 基础工具
function nowMs() { return Date.now(); }

function getNonExtraParts(name) {
    if (!name) return "";
    var s = "" + name;
    var idx = s.indexOf("||");
    return idx >= 0 ? s.substring(0, idx) : s;
}

function isInterchangeRouteKept(name) {
    if (!name) return false;
    var s = "" + name;
    if (s.indexOf("巴士") >= 0) return false;
    if (s.indexOf("晋祠专线") >= 0) return false;
    if (s.indexOf("观光") >= 0) return false;
    if (s.indexOf("空中捷运") >= 0) return false;
    if (s.indexOf("快速直达专线") >= 0) return false;
    if (s.indexOf("公交") >= 0) return false;
    return true;
}

function splitCjkNonCjk(full) {
    var cn = "", en = "";
    try {
        if (typeof TextUtil !== "undefined") {
            if (TextUtil.getCjkParts) cn = TextUtil.getCjkParts(full);
            if (TextUtil.getNonCjkParts) en = TextUtil.getNonCjkParts(full);
        }
    } catch (e) { }
    if (!cn && !en && full) {
        var s = "" + full;
        var idx = s.indexOf("|");
        if (idx >= 0) { cn = s.substring(0, idx); en = s.substring(idx + 1); }
        else cn = s;
    }
    return { cn: cn, en: en };
}

function getStationIdStr(stop) {
    try {
        if (stop.station != null) {
            if (stop.station.id != null) return "s" + stop.station.id;
            if (typeof stop.station.getId === "function") return "s" + stop.station.getId();
        }
        if (stop.platform != null) {
            if (stop.platform.id != null) return "p" + stop.platform.id;
            if (typeof stop.platform.getId === "function") return "p" + stop.platform.getId();
        }
    } catch (e) { }
    return null;
}

function getCarDisplayNum(sidingName, carIndex) {
    var parsed = parseSidingName(sidingName);
    if (!parsed.carNums || carIndex < 0 || carIndex >= parsed.carNums.length) {
        return parsed.vehicleNum || "";
    }
    var carName = "" + parsed.carNums[carIndex];
    var prefix = "" + (parsed.vehicleNum || "");
    var suffix = carName;
    if (prefix && carName.indexOf(prefix) === 0) suffix = carName.substring(prefix.length);
    if (parsed.vehicleNum) return parsed.vehicleNum + " " + suffix;
    return suffix;
}

// ==================== 行驶方向左右屏判定 ====================
// 折返（原址倒行）时车体不旋转，模型屏与"行驶方向左右"的对应关系翻转；
// 环线/灯泡线换向时车体旋转，对应关系不变。JCM 的 VehicleWrapper.isReversed()
// （文档："Whether the vehicle is running in reverse. Changed after a turnback
// rail."）正是该状态的开关，两种换向方式均可正确覆盖，无需按线路特判。
var _travelLeftWarned = false;
function getTravelLeftSide(vehicle) {
    var reversed = false, apiOk = true;
    try {
        reversed = !!vehicle.isReversed();
    } catch (e) {
        apiOk = false;
        if (!_travelLeftWarned) {
            _travelLeftWarned = true;
            print("[LCD] vehicle.isReversed() 不可用（" + e + "），折返后左右屏将不换边");
        }
    }
    var forwardLeft = (TRAVEL_LEFT_MODEL_SIDE_WHEN_FORWARD === "R") ? "R" : "L";
    var reverseLeft = (forwardLeft === "L") ? "R" : "L";
    return { side: reversed ? reverseLeft : forwardLeft, reversed: reversed, apiOk: apiOk };
}

// 车辆状态
function getVehicleSpeedMs(vehicle) {
    if (!vehicle) return 0;
    try {
        if (typeof vehicle.getSpeedMs === "function") {
            var v = vehicle.getSpeedMs();
            if (typeof v === "number" && isFinite(v)) return Math.abs(v);
        }
    } catch (e) { }
    try {
        if (typeof vehicle.getSpeedKmh === "function") {
            var v2 = vehicle.getSpeedKmh();
            if (typeof v2 === "number" && isFinite(v2)) return Math.abs(v2) / 3.6;
        }
    } catch (e) { }
    return 0;
}

function getVehicleDoorValue(vehicle) {
    if (!vehicle) return 0;
    try {
        if (typeof vehicle.getDoorValue === "function") {
            var v = vehicle.getDoorValue();
            if (typeof v === "number" && isFinite(v)) {
                return Math.max(0, Math.min(1, v));
            }
        }
    } catch (e) { }
    return 0;
}

// 当前站索引
function getCurrentStationIdx(vehicle, thisRouteStops, stations, state) {
    if (!thisRouteStops || !stations || stations.length === 0) return 0;

    try {
        if (typeof vehicle.getNextStopIndex === "function") {
            var rawNextIdx = null;
            try {
                rawNextIdx = vehicle.getNextStopIndex(thisRouteStops, 0.5);
            } catch (e1) {
                try {
                    rawNextIdx = vehicle.getNextStopIndex(thisRouteStops);
                } catch (e2) { }
            }
            if (rawNextIdx != null
                && typeof rawNextIdx === "number"
                && isFinite(rawNextIdx)
                && rawNextIdx >= 0) {

                var rawN = Math.floor(rawNextIdx);
                var currentIdx;

                if (state.isCircular) {
                    var n = ((rawN % stations.length) + stations.length) % stations.length;
                    if (state.isDoorOpen) {
                        currentIdx = n;
                    } else {
                        currentIdx = (n - 1 + stations.length) % stations.length;
                    }
                } else {
                    if (rawN >= stations.length) {
                        currentIdx = stations.length - 1;
                    } else if (state.isDoorOpen) {
                        currentIdx = rawN;
                    } else {
                        currentIdx = Math.max(0, rawN - 1);
                    }
                }

                if (state._lastIdxShown !== currentIdx) {
                    state._lastIdxShown = currentIdx;
                    var curCn = stations[currentIdx] ? stations[currentIdx].nameCn : "?";
                    var nxtCn = stations[(currentIdx + 1) % stations.length]
                        ? stations[(currentIdx + 1) % stations.length].nameCn : "?";
                    print("[LCD] nextStop=" + rawN + " len=" + stations.length
                        + " doorOpen=" + state.isDoorOpen
                        + " → idx=" + currentIdx
                        + " cur=" + curCn + " next=" + nxtCn);
                }
                return currentIdx;
            }
        }
    } catch (e) {
        print("[LCD] getNextStopIndex 异常：" + e);
    }
    return Math.max(0, Math.min(state._stopCountIdx, stations.length - 1));
}