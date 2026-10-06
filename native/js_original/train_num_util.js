// train_num_util.js
// 从侧线名中提取车号、车厢编号等信息，供车号系统和 LCD 系统共用

/**
 * 解析侧线名称
 * 侧线名格式示例：
 *   "10010/01-02-03-04-05-06"  → 车号 10010，车厢编号 [01,02,03,04,05,06]
 *   "10010 Tc1 A1-1"           → 车号 10010，编组 Tc1，车厢 A1-1
 *   "01-02-03-04-05-06"        → 无车号，仅车厢编号
 * @param {string} sidingName - 侧线名称
 * @returns {{ vehicleNum: string, carNums: string[], formation: string, extra: string }}
 */
function parseSidingName(sidingName) {
    var result = {
        vehicleNum: "",
        carNums: [],
        formation: "",
        extra: ""
    };
    if (!sidingName) return result;

    // 处理 "车号/车厢编号" 格式
    var idx = sidingName.indexOf("/");
    if (idx !== -1) {
        result.vehicleNum = sidingName.substring(0, idx).trim();
        var carPart = sidingName.substring(idx + 1).trim();
        result.carNums = carPart.split("-").map(function (s) { return s.trim(); });
        return result;
    }

    // 处理 "车号 编组 车厢" 格式（PDF 中的格式，如 "10010 Tc1 A1-1"）
    var parts = sidingName.split(/\s+/);
    if (parts.length >= 3) {
        result.vehicleNum = parts[0];
        result.formation = parts[1];   // Tc1, Mp2 等
        result.extra = parts[2];       // A1-1
        var carMatch = result.extra.match(/\d+/g);
        if (carMatch) result.carNums = carMatch;
        return result;
    }

    // 纯车厢编号格式
    result.carNums = sidingName.split("-").map(function (s) { return s.trim(); });
    return result;
}

/**
 * 获取用于 LCD 右上角显示的车号文本（如 "10010 Tc1"）
 */
function getVehicleDisplayNum(sidingName) {
    var parsed = parseSidingName(sidingName);
    if (parsed.formation) return parsed.vehicleNum + " " + parsed.formation;

    // 侧线名如 "10001/10001Tc1-10001Mp1-..."，
    // carNums[0] 是 "10001Tc1"，用 vehicleNum 做前缀去除后得到 "Tc1"
    if (parsed.vehicleNum && parsed.carNums.length > 0) {
        var first = "" + parsed.carNums[0];
        var prefix = "" + parsed.vehicleNum;
        if (first.indexOf(prefix) === 0) {
            var suffix = first.substring(prefix.length);
            if (suffix) return parsed.vehicleNum + " " + suffix;
        }
    }
    return parsed.vehicleNum || sidingName;
}

/**
 * 获取所有车厢编号
 */
function getCarNumbers(sidingName) {
    return parseSidingName(sidingName).carNums;
}