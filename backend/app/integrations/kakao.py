import requests
from typing import Tuple, List
from app.core.config import settings

class KakaoError(RuntimeError): ...

def directions_distance_time(start_lon: float, start_lat: float, end_lon: float, end_lat: float) -> Tuple[float, int]:
    """
    Kakao Navi Directions. (m, sec) 반환. 실패 시 예외.
    """
    if not settings.KAKAO_REST_API_KEY:
        raise KakaoError("KAKAO_REST_API_KEY not set")

    url = "https://apis-navi.kakaomobility.com/v1/directions"
    headers = {"Authorization": f"KakaoAK {settings.KAKAO_REST_API_KEY}"}
    params = {
        "origin": f"{start_lon},{start_lat}",
        "destination": f"{end_lon},{end_lat}",
        "priority": "RECOMMEND", "alternatives": "false", "road_details": "false",
    }
    r = requests.get(url, headers=headers, params=params, timeout=4)
    r.raise_for_status()
    data = r.json()
    routes = data.get("routes") or []
    if not routes:
        raise KakaoError("No routes in response")
    summ = routes[0].get("summary", {})
    dist = summ.get("distance")
    dur  = summ.get("duration")
    if dist is None or dur is None:
        raise KakaoError("No distance/duration in summary")
    return float(dist), int(dur)

def directions_polyline(start_lon: float, start_lat: float, end_lon: float, end_lat: float) -> List[List[float]]:
    url = "https://apis-navi.kakaomobility.com/v1/directions"
    headers = {"Authorization": f"KakaoAK {settings.KAKAO_REST_API_KEY}"}
    params = {
        "origin": f"{start_lon},{start_lat}",
        "destination": f"{end_lon},{end_lat}",
        "priority": "RECOMMEND", "alternatives":"false", "road_details":"true",
    }
    r = requests.get(url, headers=headers, params=params, timeout=4)
    r.raise_for_status()
    coords: List[List[float]] = []
    routes = r.json().get("routes") or []
    if routes:
        for sec in routes[0].get("sections", []):
            for road in sec.get("roads", []):
                v = road.get("vertexes", [])
                for i in range(0, len(v), 2):
                    lon, lat = v[i], v[i+1]
                    coords.append([lat, lon])
    if not coords:
        raise KakaoError("Empty polyline from Kakao")
    return coords