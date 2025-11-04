import requests
from typing import Tuple, List
from app.core.config import settings

TMAP_BASE = "https://apis.openapi.sk.com"

class TmapError(RuntimeError): ...

def car_route_distance_time(start_lon: float, start_lat: float, end_lon: float, end_lat: float) -> Tuple[float, int]:
    """
    T map 자동차길찾기. (m, sec) 반환. 실패 시 예외.
    """
    if not settings.TMAP_APP_KEY:
        raise TmapError("TMAP_APP_KEY not set")

    url = f"{TMAP_BASE}/tmap/tmap/routes?version=1"
    headers = {"appKey": settings.TMAP_APP_KEY, "Accept": "application/json"}
    body = {
        "reqCoordType": "WGS84GEO",
        "resCoordType": "WGS84GEO",
        "sort": "index",
        "startX": start_lon, "startY": start_lat,
        "endX": end_lon,   "endY": end_lat,
    }
    r = requests.post(url, headers=headers, json=body, timeout=4)
    r.raise_for_status()
    data = r.json()

    total_dist = None
    total_time = None
    for feat in data.get("features", []):
        props = feat.get("properties", {})
        # 보통 마지막 feature properties에 총 거리/시간이 있음
        if "totalDistance" in props: total_dist = props["totalDistance"]
        if "totalTime" in props:     total_time = props["totalTime"]

    if total_dist is None or total_time is None:
        raise TmapError("No totalDistance/totalTime in response")
    return float(total_dist), int(total_time)

def car_route_polyline(start_lon: float, start_lat: float, end_lon: float, end_lat: float) -> List[List[float]]:
    url = f"{TMAP_BASE}/tmap/tmap/routes?version=1"
    headers = {"appKey": settings.TMAP_APP_KEY, "Accept": "application/json"}
    body = {
        "reqCoordType": "WGS84GEO", "resCoordType": "WGS84GEO", "sort": "index",
        "startX": start_lon, "startY": start_lat, "endX": end_lon, "endY": end_lat
    }
    r = requests.post(url, headers=headers, json=body, timeout=4)
    r.raise_for_status()
    coords: List[List[float]] = []
    for feat in r.json().get("features", []):
        geom = feat.get("geometry", {})
        if geom.get("type") == "LineString":
            for x, y in geom.get("coordinates", []):  # [lon, lat]
                coords.append([y, x])                  # [lat, lon]
    if not coords:
        raise TmapError("Empty polyline from Tmap")
    return coords