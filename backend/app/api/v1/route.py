from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.api.deps import get_db
from app.repositories.route_repo import list_stops
from app.db import models
import json

router = APIRouter()

@router.get("/route/{route_id}")
def get_route(route_id: str, db: Session = Depends(get_db)):
    r = db.get(models.Route, route_id)
    if not r:
        raise HTTPException(404, "route not found")
    stops = list_stops(db, route_id)
    return {
        "id": r.id,
        "name": r.name,
        "polyline": json.loads(r.polyline),
        "stops": [{"id": s.id, "name": s.name, "lat": s.lat, "lon": s.lon, "seq": s.seq} for s in stops],
    }