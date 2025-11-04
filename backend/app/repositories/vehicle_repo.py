from sqlalchemy.orm import Session
from app.db import models

def get_vehicle_secret(db: Session, vehicle_id: str) -> str | None:
    v = db.get(models.Vehicle, vehicle_id)
    return v.device_secret if v else None

def touch_vehicle(db: Session, vehicle_id: str, now_ts: int):
    v = db.get(models.Vehicle, vehicle_id)
    if v:
        v.last_seen_ts = now_ts
        db.add(v); db.commit()