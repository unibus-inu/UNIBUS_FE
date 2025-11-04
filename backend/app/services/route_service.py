import json
import math
import functools
from typing import List, Tuple, Dict
from sqlalchemy.orm import Session
from app.repositories.route_repo import get_stop
from app.utils.geo import haversine_m

MetersPerDeg = 111320.0  # approximate, good enough for campus scale

LOOP_ROUTES = {"inu-a"}  # TODO: DB 컬럼으로 승격 예정

def _to_xy_m(lat: float, lon: float, lat0: float) -> Tuple[float, float]:
    """Project lat/lon (deg) to local x/y meters using equirectangular projection centered at lat0."""
    x = lon * math.cos(math.radians(lat0)) * MetersPerDeg
    y = lat * MetersPerDeg
    return x, y

@functools.lru_cache(maxsize=64)
def _route_cache(route_id: str, polyline_json: str) -> Dict[str, object]:
    """
    Cache parsed polyline and precomputed segment lengths/cumulative distances.
    Cache key uses (route_id, polyline_json) so updates to polyline bust the cache.
    """
    coords: List[List[float]] = json.loads(polyline_json)  # [[lat, lon], ...]
    if len(coords) < 2:
        raise ValueError("polyline must have >= 2 points")
    lat0 = sum(pt[0] for pt in coords) / len(coords)

    seg_len: List[float] = []
    cum: List[float] = [0.0]
    for i in range(len(coords) - 1):
        a = coords[i]
        b = coords[i + 1]
        d = haversine_m(a[0], a[1], b[0], b[1])
        seg_len.append(d)
        cum.append(cum[-1] + d)
    total = cum[-1]

    # return 직전에 추가
    closure_len = haversine_m(coords[-1][0], coords[-1][1], coords[0][0], coords[0][1])
    return {
        "coords": coords, "seg_len": seg_len, "cum": cum,
        "total": total, "lat0": lat0, "closure_len": closure_len
    }

def _project_point_to_segment_m(px: float, py: float, ax: float, ay: float, bx: float, by: float) -> Tuple[float, float, float, float]:
    """
    Project P onto segment AB in meters space.
    Returns (t in [0,1], lateral_distance_m, qx, qy)
    """
    vx, vy = bx - ax, by - ay
    wx, wy = px - ax, py - ay
    denom = vx * vx + vy * vy
    t = 0.0 if denom == 0 else max(0.0, min(1.0, (wx * vx + wy * vy) / denom))
    qx, qy = ax + t * vx, ay + t * vy
    dx, dy = px - qx, py - qy
    return t, math.hypot(dx, dy), qx, qy

def map_match_to_polyline(route: Dict[str, object], lat: float, lon: float) -> Tuple[int, float, float]:
    """
    Return (segment_index, lateral_distance_m, fraction_along_segment).
    """
    coords: List[List[float]] = route["coords"]  # type: ignore[index]
    lat0: float = route["lat0"]  # type: ignore[assignment]
    px, py = _to_xy_m(lat, lon, lat0)
    best_idx, best_dist, best_t = 0, float("inf"), 0.0
    for i in range(len(coords) - 1):
        (alat, alon) = coords[i]
        (blat, blon) = coords[i + 1]
        ax, ay = _to_xy_m(alat, alon, lat0)
        bx, by = _to_xy_m(blat, blon, lat0)
        t, dist_m, _, _ = _project_point_to_segment_m(px, py, ax, ay, bx, by)
        if dist_m < best_dist:
            best_idx, best_dist, best_t = i, dist_m, t
    return best_idx, best_dist, best_t

def distance_along_polyline(route: Dict[str, object], seg_idx: int, frac_t: float, stop_lat: float, stop_lon: float) -> float:
    """
    Distance in meters from current (seg_idx, frac_t) to the stop, following the polyline forward.
    If the stop lies 'behind', we wrap around as if the route loops.
    """
    cum: List[float] = route["cum"]  # type: ignore[assignment]
    seg_len: List[float] = route["seg_len"]  # type: ignore[assignment]
    total: float = route["total"]  # type: ignore[assignment]

    s0 = cum[seg_idx] + seg_len[seg_idx] * frac_t
    st_i, _, st_t = map_match_to_polyline(route, stop_lat, stop_lon)
    s_stop = cum[st_i] + seg_len[st_i] * st_t

    if s_stop >= s0:
        return s_stop - s0
    else:
        # Treat as loop route: remaining to end + from start to stop
        return (total - s0) + s_stop

def remaining_distance_mapmatched(db: Session, route_id: str, polyline_json: str,
                                  curr_lat: float, curr_lon: float, stop_id: str) -> Tuple[float, float]:
    route = _route_cache(route_id, polyline_json)
    seg_idx, lateral_m, frac = map_match_to_polyline(route, curr_lat, curr_lon)

    # 현재 s0, 정류장 s_stop
    s0 = route["cum"][seg_idx] + route["seg_len"][seg_idx] * frac
    stop = get_stop(db, stop_id)
    st_i, _, st_t = map_match_to_polyline(route, stop.lat, stop.lon)
    s_stop = route["cum"][st_i] + route["seg_len"][st_i] * st_t

    total = route["total"]
    if s_stop >= s0:
        rem = s_stop - s0
    else:
        # 루프 노선이면 마지막→처음 구간(closure)을 더해 wrap
        closure = route.get("closure_len", 0.0) if route_id in LOOP_ROUTES else 0.0
        rem = (total - s0) + closure + s_stop

    return rem, lateral_m

def remaining_distance_simple(db, curr_lat: float, curr_lon: float, stop_id: str) -> float:
    """단순 구면거리(Haversine)로 정류장까지의 직선거리(m)를 반환."""
    from app.repositories.route_repo import get_stop
    from app.utils.geo import haversine_m
    stop = get_stop(db, stop_id)
    return haversine_m(curr_lat, curr_lon, stop.lat, stop.lon)

