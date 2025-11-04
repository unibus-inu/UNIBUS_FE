from sqlalchemy import select
from sqlalchemy.orm import Session
from app.db import models

def insert_position(db: Session, **kwargs):
    p = models.Position(**kwargs)
    db.add(p); db.commit()
    return p

def get_latest_position(db: Session, vehicle_id: str):
    stmt = select(models.Position).where(models.Position.vehicle_id==vehicle_id)\
            .order_by(models.Position.ts.desc()).limit(1)
    return db.execute(stmt).scalars().first()

def recent_speeds(db: Session, vehicle_id: str, since_ts: int, limit:int=10):
    stmt = select(models.Position.speed_mps)\
        .where(models.Position.vehicle_id==vehicle_id, models.Position.ts>since_ts)\
        .order_by(models.Position.ts.desc()).limit(limit)
    return [r[0] for r in db.execute(stmt).all() if r[0]]