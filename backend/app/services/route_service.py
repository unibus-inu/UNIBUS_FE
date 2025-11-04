from app.repositories.route_repo import get_stop
from app.utils.geo import haversine_m

def remaining_distance_simple(db, curr_lat: float, curr_lon: float, stop_id: str) -> float:
    """MVP: '현재위치 ↔ 정류장' 직선거리 × 보정계수(1.25)"""
    stop = get_stop(db, stop_id)
    d = haversine_m(curr_lat, curr_lon, stop.lat, stop.lon)
    return d * 1.25