"""Build route polylines using Kakao Mobility Directions between consecutive stops."""
import sys
import json
import time
import argparse
from typing import List

from pathlib import Path
BASE_DIR = Path(__file__).resolve().parents[1]
if str(BASE_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_DIR))

import requests
from sqlalchemy.orm import Session

from app.core.config import settings
from app.db.session import SessionLocal
from app.db import models
from app.utils.geo import haversine_m

KAKAO_DIRECTIONS_URL = "https://apis-navi.kakaomobility.com/v1/directions"
KAKAO_REST_API_KEY = settings.KAKAO_REST_API_KEY


def _assert_key():
    if not KAKAO_REST_API_KEY:
        raise RuntimeError("KAKAO_REST_API_KEY not set (환경변수나 .env 확인)")


def _headers() -> dict:
    return {"Authorization": f"KakaoAK {KAKAO_REST_API_KEY}"}


def _dedupe(points: List[List[float]], tol_m: float = 1.5) -> List[List[float]]:
    out: List[List[float]] = []
    prev = None
    for lat, lon in points:
        if prev is None or haversine_m(prev[0], prev[1], lat, lon) > tol_m:
            out.append([lat, lon])
            prev = [lat, lon]
    return out


def kakao_segment(lat1: float, lon1: float, lat2: float, lon2: float, priority: str = "TIME") -> List[List[float]]:
    """
    Fetch a single segment polyline between two coordinates via Kakao Mobility Directions.
    priority: TIME, DISTANCE, FEE, etc. (default TIME)
    Returns [[lat, lon], ...]
    """
    params = {
        "origin": f"{lon1},{lat1}",
        "destination": f"{lon2},{lat2}",
        "priority": priority,
        "car_fuel": "GASOLINE",
        "car_hipass": "false",
        "alternatives": "false",
        "road_details": "true",
    }
    resp = requests.get(KAKAO_DIRECTIONS_URL, headers=_headers(), params=params, timeout=8)
    try:
        resp.raise_for_status()
    except requests.HTTPError as e:
        body = ""
        try:
            body = resp.text[:500]
        except Exception:
            pass
        raise requests.HTTPError(f"{e} / body={body}") from None

    data = resp.json()
    routes = data.get("routes", [])
    if not routes:
        return []

    # Kakao vertexes: [lon, lat, lon, lat, ...]
    coords: List[List[float]] = []
    first = routes[0]
    for section in first.get("sections", []):
        for road in section.get("roads", []):
            vertexes = road.get("vertexes") or []
            for i in range(0, len(vertexes), 2):
                lon = vertexes[i]
                lat = vertexes[i + 1]
                coords.append([lat, lon])
    return coords


def build_route(route_id: str, is_loop: bool = True, sleep_ms: int = 150, priority: str = "TIME"):
    _assert_key()
    db: Session = SessionLocal()
    try:
        route = db.get(models.Route, route_id)
        if not route:
            raise RuntimeError(f"route {route_id} not found")

        stops = (
            db.query(models.Stop)
            .filter(models.Stop.route_id == route_id)
            .order_by(models.Stop.seq.asc())
            .all()
        )
        if len(stops) < 2:
            raise RuntimeError("need >= 2 stops")

        pairs = list(zip(stops, stops[1:]))
        if is_loop:
            pairs.append((stops[-1], stops[0]))

        points: List[List[float]] = []
        for a, b in pairs:
            seg = kakao_segment(a.lat, a.lon, b.lat, b.lon, priority=priority)
            if points and seg:
                if haversine_m(points[-1][0], points[-1][1], seg[0][0], seg[0][1]) < 1.0:
                    seg = seg[1:]
            points.extend(seg)
            time.sleep(sleep_ms / 1000.0)

        points = _dedupe(points, 1.5)
        if is_loop and points:
            if haversine_m(points[0][0], points[0][1], points[-1][0], points[-1][1]) > 2.0:
                points.append(points[0])

        if not points:
            raise RuntimeError("No coordinates collected from Kakao Directions.")

        route.polyline = json.dumps(points, ensure_ascii=False)
        db.add(route)
        db.commit()
        print(f"saved route {route_id}: {len(points)} vertices (kakao priority={priority})")
    finally:
        db.close()


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--route-id", required=True)
    ap.add_argument("--is-loop", action="store_true", default=True)
    ap.add_argument("--priority", default="TIME", help="TIME, DISTANCE, FEE 등 Kakao Directions priority")
    ap.add_argument("--sleep-ms", type=int, default=150, help="API 사이 대기(ms)")
    args = ap.parse_args()
    build_route(args.route_id, is_loop=args.is_loop, sleep_ms=args.sleep_ms, priority=args.priority)
