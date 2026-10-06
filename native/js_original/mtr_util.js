// mtr_util.js
// 状态判定、下一路线、路线信息提取
importPackage(java.awt);

// 列车状态常量
const STATUS_NO_ROUTE = "no_route";
const STATUS_WAITING_FOR_DEPARTURE = "waiting_for_departure";
const STATUS_LEAVING_DEPOT = "leaving_depot";
const STATUS_ON_ROUTE = "on_route";
const STATUS_ARRIVED = "arrived";
const STATUS_CHANGING_ROUTE = "changing_route";
const STATUS_RETURNING_TO_DEPOT = "returning_to_depot";

function getTrainStatus(vehicle) {
    let trainStatus = null;
    if (vehicle.getStops().size() == 0) {
        trainStatus = STATUS_NO_ROUTE;
    } else if (!vehicle.isOnRoute()) {
        trainStatus = STATUS_WAITING_FOR_DEPARTURE;
    } else if (vehicle.getNextStopIndex(vehicle.getStops()) == vehicle.getStops().size()) {
        trainStatus = STATUS_RETURNING_TO_DEPOT;
    } else if (vehicle.getRailProgress() == vehicle.getStops().get(vehicle.getNextStopIndex(vehicle.getStops())).distance) {
        trainStatus = STATUS_ARRIVED;
    } else if (vehicle.getNextStopIndex(vehicle.getStops()) == 0) {
        trainStatus = STATUS_LEAVING_DEPOT;
    } else if (vehicle.getNextStopIndex(vehicle.getThisRouteStops()) == vehicle.getThisRouteStops().size()) {
        trainStatus = STATUS_CHANGING_ROUTE;
    } else {
        trainStatus = STATUS_ON_ROUTE;
    }
    return trainStatus;
}

function getNextRoute(vehicle) {
    let stops = vehicle.getStops();
    let thisRouteStops = vehicle.getThisRouteStops();
    if (stops == null || thisRouteStops == null || stops.size() == 0 || thisRouteStops.size() == 0) {
        return null;
    }
    let lastThisRouteStop = thisRouteStops.get(thisRouteStops.size() - 1);
    if (stops.get(stops.size() - 1) != lastThisRouteStop) {
        let idx = stops.indexOf(lastThisRouteStop);
        if (idx < 0 || idx + 1 >= stops.size()) return null;
        let nextStop = stops.get(idx + 1);
        if (nextStop == null || nextStop.route == null || nextStop.route.isHidden) return null;
        return nextStop.route;
    }
    return null;
}

function getRouteInfo(vehicle, trainStatus, stop) {
    if (stop == null || stop.route == null) return null;
    return {
        routeName: "" + stop.route.name,
        routeColor: safeColor(stop.route.color, 0, 155, 192),
        destination: "" + stop.destinationName
    };
}