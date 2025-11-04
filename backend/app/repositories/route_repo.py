from sqlalchemy import select
from sqlalchemy.orm import Session
from app.db import models

def get_stop(db: Session, stop_id: str):
    return db.get(models.Stop, stop_id)

def list_stops(db: Session, route_id: str):
    stmt = select(models.Stop).where(models.Stop.route_id==route_id).order_by(models.Stop.seq.asc())
    return db.execute(stmt).scalars().all()