# -*- coding: utf-8 -*-
"""
Build route polyline via T map Directions between consecutive stops.
- Tries both endpoints: /tmap/routes and /tmap/tmap/routes
- Adds Content-Type header (avoid 403 due to missing JSON header)
- Parses LineString features; dedupes vertices; optionally closes loop
"""
import os, json, time, argparse
from typing import List, Tuple
import requests
from sqlalchemy.orm import Session

from app.db.session import SessionLocal
from app.db import models
from app.utils.geo import haversine_m

TMAP_BASE = "https://apis.openapi.sk.com"
TMAP_APP_KEY = os.getenv("TMAP_APP_KEY")

def _assert_key():
    if not TMAP_APP_KEY:
        raise RuntimeError("TMAP_APP_KEY not set (.env 또는 환경변수 확인)")

def _headers():
    return {
        "appKey": TMAP_APP_KEY,
        "Accept": "application/json",
        "Content-Type": "application/json",
    }

def _dedupe(points: List[List[float]], tol_m: float = 1.5) -> List[List[float]]:
    out: List[List[float]] = []
    prev = None
    for lat, lon in points:
        if prev is None or haversine_m(prev[0], prev[1], lat, lon) > tol_m:
            out.append([lat, lon]); prev = [lat, lon]
    return out

def _tmap_segment_try(ep: str, lat1: float, lon1: float, lat2: float, lon2: float, timeout=6) -> List[List[float]]:
    """Try one endpoint; return [ [lat,lon], ... ] from LineString."""
    url = f"{TMAP_BASE}{ep}"
    body = {
        "reqCoordType": "WGS84GEO",
        "resCoordType": "WGS84GEO",
        "sort": "index",
        "startX": lon1, "startY": lat1,
        "endX":   lon2, "endY":   lat2,
    }
    r = requests.post(url, headers=_headers(), json=body, timeout=timeout)
    # 403/에러일 때 본문을 그대로 보여주면 원인파악 쉬움
    try:
        r.raise_for_status()
    except requests.HTTPError as e:
        msg = ""
        try:
            msg = r.text[:500]
        except Exception:
            pass
        raise requests.HTTPError(f"{e} / endpoint={ep} / body={msg}") from None

    data = r.json()
    coords: List[List[float]] = []
    for feat in data.get("features", []):
        geom = feat.get("geometry", {})
        if geom.get("type") == "LineString":
            for x, y in geom.get("coordinates", []):  # [lon,lat]
                coords.append([y, x])
    return coords

def tmap_segment(lat1: float, lon1: float, lat2: float, lon2: float, timeout=6) -> List[List[float]]:
    """
    Try both endpoints, prefer the one that returns LineString coords.
    """
    # 1) 권장: /tmap/routes?version=1
    endpoints = ["/tmap/routes?version=1", "/tmap/tmap/routes?version=1"]
    last_err = None
    for ep in endpoints:
        try:
            coords = _tmap_segment_try(ep, lat1, lon1, lat2, lon2, timeout=timeout)
            if coords:
                return coords
        except Exception as e:
            last_err = e
            continue
    # 둘 다 실패면 에러
    raise RuntimeError(f"Tmap segment failed: {last_err}")

def build_route(route_id: str, is_loop=True, sleep_ms=150):
    _assert_key()
    db: Session = SessionLocal()
    try:
        route = db.get(models.Route, route_id)
        assert route, f"no route {route_id}"

        stops = (db.query(models.Stop)
                 .filter(models.Stop.route_id == route_id)
                 .order_by(models.Stop.seq.asc())
                 .all())
        assert len(stops) >= 2, "need >= 2 stops"

        points: List[List[float]] = []
        pairs = list(zip(stops, stops[1:]))
        if is_loop:
            pairs.append((stops[-1], stops[0]))

        for a, b in pairs:
            seg = tmap_segment(a.lat, a.lon, b.lat, b.lon)
            if points and seg:
                # 접합부 중복 제거
                if haversine_m(points[-1][0], points[-1][1], seg[0][0], seg[0][1]) < 1.0:
                    seg = seg[1:]
            points.extend(seg)
            time.sleep(sleep_ms / 1000.0)

        points = _dedupe(points, 1.5)
        if is_loop and points and haversine_m(points[0][0], points[0][1], points[-1][0], points[-1][1]) > 2.0:
            points.append(points[0])  # 루프 닫기

        if not points:
            raise RuntimeError("No coordinates collected from T map. (상품 권한/엔드포인트/키 확인)")

        route.polyline = json.dumps(points, ensure_ascii=False)
        db.add(route); db.commit()
        print(f"saved route {route_id}: {len(points)} vertices (tmap)")
    finally:
        db.close()

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--route-id", required=True)
    ap.add_argument("--is-loop", action="store_true", default=True)
    args = ap.parse_args()
    build_route(args.route_id, is_loop=args.is_loop)