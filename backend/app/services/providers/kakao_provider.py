# app/services/providers/kakao_provider.py
from __future__ import annotations
import requests
from typing import Optional, Dict
from app.core.config import settings

_KAKAO_DIR_URL = "https://apis-navi.kakaomobility.com/v1/directions"

def directions_distance_m(origin: Dict[str,float],
                          dest: Dict[str,float],
                          waypoint: Optional[Dict[str,float]]=None,
                          timeout: float = 3.5) -> int:
    """
    Kakao Directions 호출해서 'summary.distance'(m)만 반환.
    ETA(duration)는 무시.
    origin/dest/waypoint는 {'lat':..,'lon':..} 형태.
    """
    headers = {"Authorization": f"KakaoAK {settings.KAKAO_REST_API_KEY}"}
    params = {
        "origin":      f"{origin['lon']},{origin['lat']}",
        "destination": f"{dest['lon']},{dest['lat']}",
    }
    # (선택) 앞으로 진행 고정용 웨이포인트가 있다면 추가
    if waypoint:
        params["waypoints"] = f"{waypoint['lon']},{waypoint['lat']}"

    r = requests.get(_KAKAO_DIR_URL, headers=headers, params=params, timeout=timeout)
    r.raise_for_status()
    data = r.json()
    try:
        return int(data["routes"][0]["summary"]["distance"])
    except Exception as e:
        raise RuntimeError(f"kakao: no distance in response: {data}") from e