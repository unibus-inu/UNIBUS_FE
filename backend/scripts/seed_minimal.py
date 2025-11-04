from sqlalchemy.orm import Session
from app.db.session import engine, SessionLocal
from app.db.base import Base
from app.db import models

def seed():
    Base.metadata.create_all(bind=engine)
    db: Session = SessionLocal()
    if not db.get(models.Route, "inu-a"):
        db.add(models.Route(id="inu-a", name="INU 셔틀 A", polyline="[]"))
    if not db.get(models.Stop, "stop-01"):
        db.add(models.Stop(id="stop-01", route_id="inu-a", name="정문", lat=37.3752, lon=126.6333, seq=1))
    if not db.get(models.Stop, "stop-02"):
        db.add(models.Stop(id="stop-02", route_id="inu-a", name="7호관", lat=37.3766, lon=126.6358, seq=2))
    if not db.get(models.Stop, "stop-03"):
        db.add(models.Stop(id="stop-03", route_id="inu-a", name="기숙사", lat=37.3773, lon=126.6365, seq=3))
    if not db.get(models.Vehicle, "bus-01"):
        db.add(models.Vehicle(id="bus-01", route_id="inu-a",
                              plate=None, device_secret="DEV_DEFAULT_SECRET", last_seen_ts=None))
    db.commit(); db.close()

if __name__ == "__main__":
    seed()