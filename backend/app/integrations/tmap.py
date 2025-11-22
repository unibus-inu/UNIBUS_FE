# app/integrations/tmap.py
from __future__ import annotations
import requests
from typing import List, Tuple
from app.core.config import settings

TMAP_BASE = "https://apis.openapi.sk.com"

def car_route_distance_time(start_lon: float, start_lat: float, end_lon: float, end_lat: float, timeout: int = 6) -> Tuple[float, int]:
    """티맵 자동차 경로 요약(distance m, duration s) 반환 (교통 반영)."""
    headers = {"appKey": settings.TMAP_APP_KEY, "Accept": "application/json", "Content-Type": "application/json"}
    body = {
        "reqCoordType": "WGS84GEO", "resCoordType": "WGS84GEO", "sort": "index",
        "startX": start_lon, "startY": start_lat, "endX": end_lon, "endY": end_lat
    }
    r = requests.post(f"{TMAP_BASE}/tmap/routes?version=1", headers=headers, json=body, timeout=timeout)
    r.raise_for_status()
    data = r.json()
    # 1) 루트(distance/duration)
    if "distance" in data and "duration" in data:
        return float(data["distance"]), int(data["duration"])
    # 2) features[].properties.totalDistance/totalTime
    total_dist = None; total_time = None
    for feat in data.get("features", []):
        props = feat.get("properties", {})
        if "totalDistance" in props: total_dist = props["totalDistance"]
        if "totalTime" in props: total_time = props["totalTime"]
    if total_dist is not None and total_time is not None:
        return float(total_dist), int(total_time)
    raise RuntimeError("Tmap: distance/time not found")

def segment_polyline(start_lon: float, start_lat: float, end_lon: float, end_lat: float, timeout: int = 6) -> List[List[float]]:
    """티맵 A->B 라인 좌표 [lat,lon] 리스트 반환 (LineString)."""
    headers = {"appKey": settings.TMAP_APP_KEY, "Accept": "application/json", "Content-Type": "application/json"}
    body = {
        "reqCoordType": "WGS84GEO", "resCoordType": "WGS84GEO", "sort": "index",
        "startX": start_lon, "startY": start_lat, "endX": end_lon, "endY": end_lat
    }
    r = requests.post(f"{TMAP_BASE}/tmap/tmap/routes?version=1", headers=headers, json=body, timeout=timeout)
    r.raise_for_status()
    data = r.json()
    coords: List[List[float]] = []
    for feat in data.get("features", []):
        geom = feat.get("geometry", {})
        if geom.get("type") == "LineString":
            for x, y in geom.get("coordinates", []):  # [lon,lat]
                coords.append([y, x])
    # 일부 케이스에서 루트 distance/duration만 오는 버전도 있으니, 그땐 빈 리스트 반환
    return coords