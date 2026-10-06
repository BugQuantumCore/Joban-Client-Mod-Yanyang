// circular.js
// 坏（huán）线判定
importPackage(java.awt);

/**
 * 读取某个 SimplifiedRoute 的 CircularState 字符串。
 * Route.CircularState 的三个取值：
  *   NONE          → 非环线
 *   CLOCKWISE     → 顺时针环线
 *   ANTICLOCKWISE → 逆时针环线
 * 返回 null 表示无法读取。
 */
function readRouteCircularState(route) {
    if (route == null) return null;
    try {
        if (typeof route.getCircularState === "function") {
            var v = route.getCircularState();
            return v == null ? null : ("" + v);
        }
        if (route.circularState != null) return "" + route.circularState;
    } catch (e) { }
    return null;
}

/**
 * 判断 CircularState 字符串是否为环线取值（CLOCKWISE / ANTICLOCKWISE）。
 */
function isCircularStateValue(v) {
    return v === "CLOCKWISE" || v === "ANTICLOCKWISE";
}

/**
 * 获取某条 route 的站台数（getPlatforms().size()）。
 */
function getRoutePlatformCount(route) {
    try {
        if (route != null && typeof route.getPlatforms === "function") {
            var pl = route.getPlatforms();
            if (pl != null) return pl.size();
        }
    } catch (e) { }
    return 0;
}

/**
 * 环线判定核心逻辑。仅需在 JS 中判定一次，结果写入 state.isCircular / state.circularState。
 * 环线与普通线二者互斥，state.isCircular 决定后续走哪条绘制分支。
 *
 * 判定方式（满足其一即为环线）：
 *  (i)  读取当前 route 的 SimplifiedRoute.getCircularState()。
 *       若为 CLOCKWISE / ANTICLOCKWISE，则为环线，并“锁定”
 *       （在未重载前该 vehicle 恒为环线，不随后续 getCircularState() 变化而改变）。
 *  (ii) 若当前 route 的 getCircularState() 为 NONE，则遍历该列车将要经过的所有 route：
 *       从最小索引 i = 0 开始，读取 getStops().get(i).route.getCircularState()；
 *       若为 NONE，则令 i += getStops().get(i).route.getPlatforms().size()，继续判定；
  *       直至 i >= getStops().size() 为止。期间任意一个不为 NONE 即为环线。
 *       若全部为 NONE，则为非环线。
 *       注意：方式 (ii) 命中不锁定，列车换到非环线后仍会重新判定。
 *
 * 返回值：{ isCircular: Boolean, circularState: String|null }
 */
function resolveCircularState(vehicle, state) {
    // 仅在方式 (i) 命中后锁定；锁定后直接沿用，不再重新判定
    if (state._isCircularLocked) {
        state.isCircular = true;
        return { isCircular: true, circularState: state.circularState };
    }

    // 每次判定前先重置，避免残留上一次的取值
    state.isCircular = false;
    state.circularState = null;
    var result = { isCircular: false, circularState: null };

    // ---------- 获取当前 route ----------
    var currentRoute = null;
    try {
        var thisRouteStops = vehicle.getThisRouteStops();
        if (thisRouteStops != null && thisRouteStops.size() > 0) {
            var firstStop = thisRouteStops.get(0);
            if (firstStop) currentRoute = firstStop.route;
        }
    } catch (e) { }

    // ---------- 方式 (i)：当前 route 自身即为环线（命中后锁定） ----------
    var cs = readRouteCircularState(currentRoute);
    if (isCircularStateValue(cs)) {
        result.isCircular = true;
        result.circularState = cs;
        state.isCircular = true;
        state.circularState = cs;
        state._isCircularLocked = true;   // 锁定：未重载前恒为环线
        print("[LCD] 环线检测命中 (i)：当前 route circularState=" + cs);
        return result;
    }

    // ---------- 方式 (ii)：遍历列车将要经过的所有 route（不锁定） ----------
    var stops = null;
    try { stops = vehicle.getStops(); } catch (e) { }
    if (stops == null || stops.size() === 0) {
        print("[LCD] 环线检测未命中：无 stops → 非环线");
        return result;
    }

    var total = stops.size();
    var i = 0;
    var safety = total + 8;   // 防止死循环

    while (i < total && safety-- > 0) {
        var stop = null;
        try { stop = stops.get(i); } catch (e) { }
        if (stop == null) break;

        var stopRoute = stop.route;
        var sv = readRouteCircularState(stopRoute);

        if (isCircularStateValue(sv)) {
            result.isCircular = true;
            result.circularState = sv;
            state.isCircular = true;
            state.circularState = sv;
            // 方式 (ii) 不锁定：允许列车换到非环线后重新判定
            print("[LCD] 环线检测命中 (ii)：索引 " + i + " circularState=" + sv);
            return result;
        }

        // 当前 route 为 NONE/null：按该 route 的站台数前进
        var pc = getRoutePlatformCount(stopRoute);
        if (pc <= 0) {
            // 无法推进，避免死循环，直接终止遍历
            print("[LCD] 环线检测 (ii)：索引 " + i + " 的 route 站台数为 0，终止遍历");
            break;
        }
        i += pc;
    }

    // 全部为 NONE → 非环线
    print("[LCD] 环线检测未命中 → 非环线");
    return result;
}