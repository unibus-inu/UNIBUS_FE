# app/integrations/kakao.py
from __future__ import annotations
import requests
from typing import List, Tuple
from app.core.config import settings

KAKAO_BASE = "https://apis-navi.kakaomobility.com"

def _headers():
    if not settings.KAKAO_REST_API_KEY:
        raise RuntimeError("KAKAO_REST_API_KEY not set")
    return {"Authorization": f"KakaoAK {settings.KAKAO_REST_API_KEY}"}

def car_route_distance_time(start_lon: float, start_lat: float, end_lon: float, end_lat: float, timeout: int = 6) -> Tuple[float, int]:
    """카카오 자동차 길찾기 요약값(distance m, duration s) 반환 (교통 반영)."""
    params = {
        "origin": f"{start_lon},{start_lat}",
        "destination": f"{end_lon},{end_lat}",
        "priority": "RECOMMEND",
        "summary": "true",  # summary=true면 요약(distance,duration) 포함
    }
    r = requests.get(f"{KAKAO_BASE}/v1/directions", headers=_headers(), params=params, timeout=timeout)
    r.raise_for_status()
    data = r.json()
    # routes[0].summary.distance / duration
    route0 = (data.get("routes") or [None])[0] or {}
    summ = route0.get("summary") or {}
    dist = float(summ.get("distance"))
    dur = int(summ.get("duration"))
    return dist, dur

def segment_polyline(start_lon: float, start_lat: float, end_lon: float, end_lat: float, timeout: int = 6) -> List[List[float]]:
    """카카오 상세 응답(roads.vertexes)에서 [lat,lon] 리스트 반환."""
    params = {
        "origin": f"{start_lon},{start_lat}",
        "destination": f"{end_lon},{end_lat}",
        "priority": "RECOMMEND",
        "summary": "false",  # 상세 섹션/roads 포함
    }
    r = requests.get(f"{KAKAO_BASE}/v1/directions", headers=_headers(), params=params, timeout=timeout)
    r.raise_for_status()
    data = r.json()
    coords: List[List[float]] = []
    routes = data.get("routes") or []
    if not routes:
        return coords
    for sec in routes[0].get("sections") or []:
        for road in sec.get("roads") or []:
            v = road.get("vertexes") or []  # [x1,y1,x2,y2,...]
            for i in range(0, len(v) - 1, 2):
                x, y = float(v[i]), float(v[i+1])  # x=lon, y=lat
                coords.append([y, x])
    return coords