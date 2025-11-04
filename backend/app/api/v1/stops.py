from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from app.api.deps import get_db
from app.repositories import route_repo, position_repo
from app.services.route_service import remaining_distance_simple
from app.services.eta_service import effective_speed_mps, eta_seconds
from app.schemas.eta import EtaOut

router = APIRouter()

@router.get("/stops/{stop_id}/eta", response_model=EtaOut)
def get_eta(stop_id: str,
            vehicle_id: str = Query(default="bus-01"),
            db: Session = Depends(get_db)):
    stop = route_repo.get_stop(db, stop_id)
    if not stop: raise HTTPException(404, "stop not found")
    pos = position_repo.get_latest_position(db, vehicle_id)
    if not pos: raise HTTPException(404, "no position")

    v = effective_speed_mps(db, vehicle_id, pos.ts)
    rem = remaining_distance_simple(db, pos.lat, pos.lon, stop_id)
    sec = eta_seconds(rem, v)
    conf = "low" if v in (1.5, 15.0) else ("mid" if rem>500 else "high")

    return EtaOut(stop_id=stop_id, route_id=stop.route_id, eta_seconds=sec, eta_confidence=conf)