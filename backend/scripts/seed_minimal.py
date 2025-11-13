# scripts/seed_minimal.py
# --- add project root (backend/) to sys.path ---
import sys
from pathlib import Path
BASE_DIR = Path(__file__).resolve().parents[1]  # .../backend
if str(BASE_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_DIR))
# -----------------------------------------------
from sqlalchemy.orm import Session
from app.db.session import engine, SessionLocal
from app.db.base import Base
from app.db import models
import json

ROUTE_ID = "inu-a"
ROUTE_NAME = "INU 셔틀 A"

# 정류장 좌표 (lat, lon) — 너가 준 값
STOPS = [
    # seq 1..N (노선 진행 순서대로)
    {"id": "stop-incheon-univ-stn", "name": "인천대입구역", "lat": 37.385524, "lon": 126.638650, "seq": 1},
    {"id": "stop-main-gate",        "name": "인천대정문",   "lat": 37.377730, "lon": 126.635094, "seq": 2},
    {"id": "stop-eng",              "name": "인천대공과대", "lat": 37.37418, "lon": 126.6344, "seq": 3},
    {"id": "stop-dorm",             "name": "인천대생활원", "lat": 37.374114, "lon": 126.630295, "seq": 4},
]

# 노선 폴리라인은 MVP에선 정류장들을 순서대로 연결 (필요시 더 촘촘한 경로로 교체)
POLYLINE = [[s["lat"], s["lon"]] for s in STOPS]

def upsert_route(db: Session):
    r = db.get(models.Route, ROUTE_ID)
    if not r:
        r = models.Route(id=ROUTE_ID, name=ROUTE_NAME, polyline=json.dumps(POLYLINE))
        db.add(r)
    else:
        r.name = ROUTE_NAME
        r.polyline = json.dumps(POLYLINE)
        db.add(r)

def upsert_stops(db: Session):
    for s in STOPS:
        cur = db.get(models.Stop, s["id"])
        if not cur:
            db.add(models.Stop(id=s["id"], route_id=ROUTE_ID, name=s["name"],
                               lat=s["lat"], lon=s["lon"], seq=s["seq"]))
        else:
            cur.route_id = ROUTE_ID
            cur.name = s["name"]
            cur.lat = s["lat"]
            cur.lon = s["lon"]
            cur.seq = s["seq"]
            db.add(cur)

def upsert_vehicle(db: Session):
    v = db.get(models.Vehicle, "bus-01")
    if not v:
        db.add(models.Vehicle(id="bus-01", route_id=ROUTE_ID,
                              plate=None, device_secret="DEV_DEFAULT_SECRET", last_seen_ts=None))
    else:
        v.route_id = ROUTE_ID
        db.add(v)

def seed():
    Base.metadata.create_all(bind=engine)
    db: Session = SessionLocal()
    upsert_route(db)
    upsert_stops(db)
    upsert_vehicle(db)
    db.commit(); db.close()
    print("✓ Seeded route/stops/vehicle")

if __name__ == "__main__":
    seed()
