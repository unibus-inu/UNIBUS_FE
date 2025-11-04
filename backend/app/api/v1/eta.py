from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from app.api.deps import get_db
from app.repositories import route_repo, position_repo
from app.services.route_service import remaining_distance_mapmatched
from app.services.eta_service import effective_speed_mps, eta_seconds

router = APIRouter()

@router.get("/eta/baseline")
def eta_baseline(
    vehicle_id: str = Query(...),
    stop_id: str = Query(...),
    db: Session = Depends(get_db),
):
    pos = position_repo.get_latest_position(db, vehicle_id)
    if not pos:
        raise HTTPException(404, "no position")

    from app.db import models
    veh = db.get(models.Vehicle, vehicle_id)
    if not veh:
        raise HTTPException(404, "vehicle not found")
    r = db.get(models.Route, veh.route_id)
    if not r:
        raise HTTPException(404, "route not found")

    rem_m, lateral_m = remaining_distance_mapmatched(db, r.id, r.polyline, pos.lat, pos.lon, stop_id)
    v = effective_speed_mps(db, vehicle_id, pos.ts)
    sec = eta_seconds(rem_m, v)
    conf = "low" if v in (1.5, 15.0) or lateral_m > 30 else ("mid" if rem_m > 500 else "high")
    return {
        "vehicle_id": vehicle_id,
        "route_id": veh.route_id,
        "stop_id": stop_id,
        "eta_seconds": sec,
        "confidence": conf,
        "debug": {"remaining_m": round(rem_m, 1), "eff_speed_mps": round(v, 2), "lateral_m": round(lateral_m, 1)},
    }