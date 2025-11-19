from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.api.deps import get_db
from app.db import models
from app.repositories.position_repo import get_latest_position
from app.schemas.eta import LatestOut
from app.services import vehicle_monitor

router = APIRouter(prefix="/v1", tags=["vehicles"])


@router.get("/vehicles/{vehicle_id}/last")
def last_position(vehicle_id: str, db: Session = Depends(get_db)):
    vp = (
        db.query(models.VehiclePosition)
        .filter(models.VehiclePosition.vehicle_id == vehicle_id)
        .order_by(models.VehiclePosition.ts.desc())
        .first()
    )
    if not vp:
        raise HTTPException(404, "no data")
    return {
        "vehicle_id": vp.vehicle_id,
        "ts": vp.ts,
        "lat": vp.lat,
        "lon": vp.lon,
        "speed_mps": vp.speed_mps,
        "heading": vp.heading,
    }


@router.get("/vehicles/{vehicle_id}/trail")
def trail(vehicle_id: str, limit: int = 100, db: Session = Depends(get_db)):
    rows = (
        db.query(models.VehiclePosition)
        .filter(models.VehiclePosition.vehicle_id == vehicle_id)
        .order_by(models.VehiclePosition.ts.desc())
        .limit(min(limit, 1000))
        .all()
    )
    rows.reverse()
    return {
        "vehicle_id": vehicle_id,
        "points": [{"ts": r.ts, "lat": r.lat, "lon": r.lon} for r in rows],
    }


@router.get("/vehicles/{vehicle_id}/latest", response_model=LatestOut)
def latest(vehicle_id: str, db: Session = Depends(get_db)):
    pos = get_latest_position(db, vehicle_id)
    if not pos:
        raise HTTPException(status_code=404, detail="no position")
    return LatestOut(
        vehicle_id=vehicle_id,
        ts=pos.ts,
        lat=pos.lat,
        lon=pos.lon,
        speed_mps=pos.speed_mps,
        heading=pos.heading,
    )


@router.get("/vehicles/{vehicle_id}/status")
def vehicle_status(vehicle_id: str, db: Session = Depends(get_db)):
    return vehicle_monitor.classify_vehicle_status(db, vehicle_id)


@router.get("/vehicles/status")
def vehicles_status(db: Session = Depends(get_db)):
    return vehicle_monitor.classify_all_vehicle_statuses(db)


__all__ = ["router"]
