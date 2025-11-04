# -*- coding: utf-8 -*-
import os, json, time, math, argparse, requests
from sqlalchemy.orm import Session
from app.db.session import SessionLocal
from app.db import models
from app.utils.geo import haversine_m

KAKAO_KEY = os.getenv("KAKAO_REST_API_KEY")

def dedupe(points, tol_m=1.5):
    out = []; prev=None
    for lat, lon in points:
        if prev is None or haversine_m(prev[0], prev[1], lat, lon) > tol_m:
            out.append([lat, lon]); prev=[lat, lon]
    return out

def kakao_segment(lat1, lon1, lat2, lon2, timeout=5):
    url = "https://apis-navi.kakaomobility.com/v1/directions"
    headers = {"Authorization": f"KakaoAK {KAKAO_KEY}"}
    params = {
        "origin": f"{lon1},{lat1}",
        "destination": f"{lon2},{lat2}",
        "priority": "RECOMMEND", "alternatives":"false", "road_details":"true",
    }
    r = requests.get(url, headers=headers, params=params, timeout=timeout)
    r.raise_for_status()
    data = r.json()
    # routes[0].sections[].roads[].vertexes => [lon1,lat1,lon2,lat2,...]
    coords=[]
    routes = data.get("routes") or []
    if not routes: raise RuntimeError("no route")
    for sec in routes[0].get("sections", []):
        for road in sec.get("roads", []):
            v = road.get("vertexes", [])
            for i in range(0, len(v), 2):
                lon, lat = v[i], v[i+1]
                coords.append([lat, lon])
    if not coords: raise RuntimeError("empty vertexes")
    return coords

def build_route(route_id: str, is_loop=True, sleep_ms=150):
    if not KAKAO_KEY: raise RuntimeError("KAKAO_REST_API_KEY not set")
    db: Session = SessionLocal()
    try:
        route = db.get(models.Route, route_id)
        assert route, f"no route {route_id}"
        stops = (db.query(models.Stop)
                 .filter(models.Stop.route_id==route_id)
                 .order_by(models.Stop.seq.asc()).all())
        assert len(stops) >= 2, "need >=2 stops"

        points=[]
        pairs = list(zip(stops, stops[1:]))
        if is_loop: pairs.append((stops[-1], stops[0]))
        for a,b in pairs:
            seg = kakao_segment(a.lat, a.lon, b.lat, b.lon)
            if points and seg and haversine_m(points[-1][0], points[-1][1], seg[0][0], seg[0][1]) < 1.0:
                seg=seg[1:]
            points.extend(seg)
            time.sleep(sleep_ms/1000.0)

        points = dedupe(points, 1.5)
        if is_loop and points and haversine_m(points[0][0], points[0][1], points[-1][0], points[-1][1]) > 2.0:
            points.append(points[0])

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