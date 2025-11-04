from typing import Optional
from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from sqlalchemy import func, text
from app.api.deps import get_db
from app.db import models

router = APIRouter()

@router.get("/debug/positions/summary")
def positions_summary(db: Session = Depends(get_db)):
    total = db.query(func.count(models.Position.id)).scalar() or 0
    latest_ts = db.query(func.max(models.Position.ts)).scalar()
    per_vehicle = (
        db.query(models.Position.vehicle_id, func.count(models.Position.id))
          .group_by(models.Position.vehicle_id)
          .all()
    )
    return {
        "total": total,
        "latest_ts": latest_ts,
        "per_vehicle": [{"vehicle_id": v, "count": c} for (v, c) in per_vehicle],
    }

@router.get("/debug/positions/recent")
def positions_recent(
    vehicle_id: Optional[str] = Query(None),
    limit: int = Query(50, ge=1, le=500),
    db: Session = Depends(get_db),
):
    q = db.query(models.Position).order_by(models.Position.ts.desc())
    if vehicle_id:
        q = q.filter(models.Position.vehicle_id == vehicle_id)
    rows = q.limit(limit).all()
    return [
        {
            "vehicle_id": r.vehicle_id,
            "ts": r.ts,
            "lat": r.lat,
            "lon": r.lon,
            "speed_mps": r.speed_mps,
            "heading": r.heading,
        }
        for r in rows
    ]

# 초간단 핑(문제 분리용)
@router.get("/debug/pingdb")
def pingdb(db: Session = Depends(get_db)):
    db.execute(text("SELECT 1"))
    return {"ok": True}