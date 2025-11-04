# app/api/compat.py
from fastapi import APIRouter, Depends, HTTPException, Query, WebSocket
from fastapi.websockets import WebSocketDisconnect
from sqlalchemy.orm import Session

from app.api.deps import get_db
from app.repositories.position_repo import get_latest_position
from app.repositories.route_repo import list_stops
from app.schemas.eta import LatestOut
from app.services.eta_service import effective_speed_mps, eta_seconds
from app.services.route_service import remaining_distance_simple
from app.utils.geo import haversine_m
try:
    from app.core.events import broker  # optional: real-time streaming broker
except Exception:
    broker = None  # fallback when broker module is absent

router = APIRouter()

# /api/health
@router.get("/api/health")
def api_health():
    return {"ok": True}

# /api/positions/latest?bus_id=B-N
@router.get("/api/positions/latest", response_model=LatestOut)
def api_latest(bus_id: str = Query(..., alias="bus_id"), db: Session = Depends(get_db)):
    pos = get_latest_position(db, bus_id)
    if not pos:
        raise HTTPException(404, "no position")
    return LatestOut(
        vehicle_id=bus_id, ts=pos.ts, lat=pos.lat, lon=pos.lon,
        speed_mps=pos.speed_mps, heading=pos.heading
    )

# /api/stops[?route_id=...]
@router.get("/api/stops")
def api_stops(route_id: str | None = None, db: Session = Depends(get_db)):
    rid = route_id or "inu-a"   # 기본 노선 아이디(필요시 교체)
    stops = list_stops(db, rid)
    return [{"id": s.id, "name": s.name, "lat": s.lat, "lon": s.lon, "seq": s.seq} for s in stops]

# /api/route?route_id=...
@router.get("/api/route")
def api_route(route_id: str = Query(...), db: Session = Depends(get_db)):
    from app.db import models
    r = db.get(models.Route, route_id)
    if not r:
        raise HTTPException(404, "route not found")
    return {"id": r.id, "name": r.name, "polyline": r.polyline}

def _pick_next_stop_id(db: Session, route_id: str, lat: float, lon: float) -> str:
    """stop_id 미지정 시 가장 가까운 정류장을 우선 반환(MVP)."""
    stops = list_stops(db, route_id)
    best = min(stops, key=lambda s: haversine_m(lat, lon, s.lat, s.lon))
    return best.id

# /api/eta?bus_id=B-N[&stop_id=...]
@router.get("/api/eta")
def api_eta(bus_id: str, stop_id: str | None = None, route_id: str | None = None,
            db: Session = Depends(get_db)):
    pos = get_latest_position(db, bus_id)
    if not pos:
        raise HTTPException(404, "no position")
    rid = route_id or "inu-a"
    sid = stop_id or _pick_next_stop_id(db, rid, pos.lat, pos.lon)
    v = effective_speed_mps(db, bus_id, pos.ts)
    rem = remaining_distance_simple(db, pos.lat, pos.lon, sid)
    sec = eta_seconds(rem, v)
    return {"bus_id": bus_id, "route_id": rid, "stop_id": sid,
            "eta_seconds": sec, "confidence": ("low" if v in (1.5, 15.0) else ("mid" if rem > 500 else "high"))}

# /api/eta_mix?bus_id=B-N  (외부 교통 혼합 스텁)
@router.get("/api/eta_mix")
def api_eta_mix(bus_id: str, stop_id: str | None = None, route_id: str | None = None,
                db: Session = Depends(get_db)):
    base = api_eta(bus_id=bus_id, stop_id=stop_id, route_id=route_id, db=db)
    base["source"] = "baseline"   # 후속: T-map/Kakao 혼합값으로 교체
    return base

# /ws/bus/{bus_id}  (WebSocket alias)
@router.websocket("/ws/bus/{bus_id}")
async def ws_bus(websocket: WebSocket, bus_id: str):
    await websocket.accept()
    if broker is None:
        await websocket.send_json({"warning": "stream broker not configured"})
        await websocket.close()
        return
    key = f"vehicle:{bus_id}"
    q = await broker.subscribe(key)
    try:
        while True:
            data = await q.get()
            await websocket.send_json(data)
    except WebSocketDisconnect:
        pass
    finally:
        await broker.unsubscribe(key, q)