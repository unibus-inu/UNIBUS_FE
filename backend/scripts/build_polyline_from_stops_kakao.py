# -*- coding: utf-8 -*-
# backend/scripts/build_polyline_from_stops_kakao.py
import os, json, time, argparse
from sqlalchemy.orm import Session
from app.db.session import SessionLocal
from app.db import models
from app.utils.geo import haversine_m
from app.integrations.kakao import segment_polyline

def dedupe(points, tol_m=1.5):
    out=[]; prev=None
    for lat, lon in points:
        if prev is None or haversine_m(prev[0], prev[1], lat, lon) > tol_m:
            out.append([lat,lon]); prev=[lat,lon]
    return out

def build_route(route_id: str, is_loop=True, sleep_ms=150):
    db: Session = SessionLocal()
    try:
        route = db.get(models.Route, route_id); assert route, f"no route {route_id}"
        stops = (db.query(models.Stop)
                 .filter(models.Stop.route_id==route_id)
                 .order_by(models.Stop.seq.asc()).all())
        assert len(stops)>=2, "need >=2 stops"
        pts=[]
        pairs=list(zip(stops, stops[1:]));
        if is_loop: pairs.append((stops[-1], stops[0]))
        for a,b in pairs:
            seg = segment_polyline(a.lon, a.lat, b.lon, b.lat)  # kakao는 (lon,lat) 인자
            if pts and seg:
                # 접합 시 중복 제거
                if haversine_m(pts[-1][0], pts[-1][1], seg[0][0], seg[0][1]) < 1.0:
                    seg = seg[1:]
            pts.extend(seg)
            time.sleep(sleep_ms/1000.0)
        pts = dedupe(pts, 1.5)
        if is_loop and pts and haversine_m(pts[0][0], pts[0][1], pts[-1][0], pts[-1][1]) > 2.0:
            pts.append(pts[0])
        route.polyline = json.dumps(pts, ensure_ascii=False)
        db.add(route); db.commit()
        print(f"saved route {route_id}: {len(pts)} vertices (kakao)")
    finally:
        db.close()

if __name__=="__main__":
    ap=argparse.ArgumentParser()
    ap.add_argument("--route-id", required=True)
    ap.add_argument("--is-loop", action="store_true", default=True)
    args=ap.parse_args()
    build_route(args.route_id, is_loop=args.is_loop)