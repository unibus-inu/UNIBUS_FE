# -*- coding: utf-8 -*-
import os, json, time, math, argparse, requests
from sqlalchemy.orm import Session
from app.db.session import SessionLocal
from app.db import models
from app.utils.geo import haversine_m

TMAP_BASE = "https://apis.openapi.sk.com"
TMAP_APP_KEY = os.getenv("TMAP_APP_KEY")  # .env에도 동일 이름 권장

def dedupe(points, tol_m=1.5):
    out = []
    prev = None
    for lat, lon in points:
        if prev is None or haversine_m(prev[0], prev[1], lat, lon) > tol_m:
            out.append([lat, lon]); prev = [lat, lon]
    return out

def tmap_segment(lat1, lon1, lat2, lon2, timeout=5):
    url = f"{TMAP_BASE}/tmap/tmap/routes?version=1"
    headers = {"appKey": TMAP_APP_KEY, "Accept": "application/json"}
    body = {
        "reqCoordType": "WGS84GEO", "resCoordType": "WGS84GEO", "sort": "index",
        "startX": lon1, "startY": lat1, "endX": lon2, "endY": lat2
    }
    r = requests.post(url, headers=headers, json=body, timeout=timeout)
    r.raise_for_status()
    data = r.json()
    coords = []
    for feat in data.get("features", []):
        geom = feat.get("geometry", {})
        if geom.get("type") == "LineString":
            for x, y in geom.get("coordinates", []):  # [lon, lat]
                coords.append([y, x])  # [lat, lon]
    if not coords:
        raise RuntimeError("Tmap returned empty LineString")
    return coords

def build_route(route_id: str, is_loop=True, sleep_ms=150):
    if not TMAP_APP_KEY:
        raise RuntimeError("TMAP_APP_KEY not set")
    db: Session = SessionLocal()
    try:
        route = db.get(models.Route, route_id)
        assert route, f"no route {route_id}"
        stops = (db.query(models.Stop)
                 .filter(models.Stop.route_id==route_id)
                 .order_by(models.Stop.seq.asc()).all())
        assert len(stops) >= 2, "need >=2 stops"

        points = []
        pairs = list(zip(stops, stops[1:]))
        if is_loop:
            pairs.append((stops[-1], stops[0]))

        for a, b in pairs:
            seg = tmap_segment(a.lat, a.lon, b.lat, b.lon)
            if points and seg:
                # 이어붙일 때 시작 중복 제거
                if haversine_m(points[-1][0], points[-1][1], seg[0][0], seg[0][1]) < 1.0:
                    seg = seg[1:]
            points.extend(seg)
            time.sleep(sleep_ms/1000.0)  # 쿼터 여유

        points = dedupe(points, 1.5)
        if is_loop and points and haversine_m(points[0][0], points[0][1], points[-1][0], points[-1][1]) > 2.0:
            points.append(points[0])  # 루프 닫기

        route.polyline = json.dumps(points, ensure_ascii=False)
        db.add(route); db.commit()
        print(f"saved route {route_id}: {len(points)} vertices")
    finally:
        db.close()

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--route-id", required=True)
    ap.add_argument("--is-loop", action="store_true", default=True)
    args = ap.parse_args()
    build_route(args.route_id, is_loop=args.is_loop)