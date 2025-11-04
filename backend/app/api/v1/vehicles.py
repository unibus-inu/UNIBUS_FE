# app/api/v1/vehicles.py
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.api.deps import get_db
from app.repositories.position_repo import get_latest_position
from app.schemas.eta import LatestOut

router = APIRouter()  # <- 반드시 전역에서 인스턴스 생성

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

__all__ = ["router"]  # (선택) 내보낼 심볼 명시