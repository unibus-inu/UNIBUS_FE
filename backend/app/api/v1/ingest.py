from fastapi import APIRouter, Depends, Header, HTTPException, Request
from sqlalchemy.orm import Session
from app.schemas.ingest import IngestIn
from app.core.config import settings
from app.core.security import verify_hmac
from app.repositories import vehicle_repo, position_repo
from app.api.deps import get_db
from app.core.events import broker

router = APIRouter()

@router.post("/ingest")
async def ingest(req: Request, payload: IngestIn,
                 x_device_signature: str | None = Header(default=None),
                 db: Session = Depends(get_db)):
    body = await req.body()

    # 시크릿 조회
    secret = vehicle_repo.get_vehicle_secret(db, payload.vehicle_id) \
             or (settings.DEFAULT_DEVICE_SECRET)

    if settings.DEVICE_HMAC_REQUIRED and not verify_hmac(secret, body, x_device_signature or ""):
        raise HTTPException(401, "bad signature")

    position_repo.insert_position(
        db,
        vehicle_id=payload.vehicle_id,
        ts=payload.ts,
        lat=payload.lat,
        lon=payload.lon,
        speed_mps=payload.speed_mps,
        heading=payload.heading,
        hdop=payload.hdop,
        src="gps",
    )

    # ...
    await broker.publish(f"vehicle:{payload.vehicle_id}", {
        "vehicle_id": payload.vehicle_id,
        "ts": payload.ts,
        "lat": payload.lat,
        "lon": payload.lon,
        "speed_mps": payload.speed_mps,
        "heading": payload.heading,
    })
    vehicle_repo.touch_vehicle(db, payload.vehicle_id, payload.ts)
    return {"ok": True}