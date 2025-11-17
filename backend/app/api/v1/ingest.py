from fastapi import APIRouter, Request, HTTPException, Depends
import json, hmac, hashlib
from app.core.config import settings
from sqlalchemy.orm import Session
from app.db.session import get_db
from app.db import models

router = APIRouter()


@router.post("/ingest")
async def ingest(request: Request, db: Session = Depends(get_db)):
    # 1) 원본 바이트 확보
    raw: bytes = await request.body()

    # 2) 서명 검증은 'raw'로만!
    secret = (getattr(settings, "INGEST_SECRET", "") or "").strip()
    allow_unsigned = bool(getattr(settings, "INGEST_ALLOW_UNSIGNED", False))

    sig_ok = False
    if allow_unsigned or not secret:
        sig_ok = True
    else:
        dev_sig = request.headers.get("X-Device-Signature") or ""
        if dev_sig:
            expect = hmac.new(secret.encode(), raw, hashlib.sha256).hexdigest()
            if hmac.compare_digest(dev_sig, expect):
                sig_ok = True

        if not sig_ok:
            ts = request.headers.get("X-Ingest-Ts")
            ing_sig = request.headers.get("X-Ingest-Sign") or request.headers.get("X-Ingest-Signature")
            if ts and ing_sig:
                combo = f"{ts}.{raw.decode()}"
                expect2 = hmac.new(secret.encode(), combo.encode(), hashlib.sha256).hexdigest()
                if hmac.compare_digest(ing_sig, expect2):
                    sig_ok = True

    if not sig_ok:
        raise HTTPException(status_code=401, detail="bad signature")

    # 3) 검증 후에 JSON 파싱
    try:
        payload = json.loads(raw.decode())
    except Exception:
        raise HTTPException(status_code=400, detail="invalid json")

    # 4) 필수 필드 검증 + 동기 저장 (commit)
    try:
        vehicle_id = str(payload["vehicle_id"])  # required
        ts = int(payload["ts"])                  # required (epoch seconds)
        lat = float(payload["lat"])              # required
        lon = float(payload["lon"])              # required
        speed_mps = float(payload.get("speed_mps") or 0.0)
        heading = float(payload.get("heading") or 0.0)
    except (KeyError, ValueError, TypeError):
        raise HTTPException(status_code=400, detail="missing or invalid fields")

    vp = models.VehiclePosition(
        vehicle_id=vehicle_id,
        ts=ts,
        lat=lat,
        lon=lon,
        speed_mps=speed_mps,
        heading=heading,
    )
    try:
        db.add(vp)
        db.commit()
    except Exception:
        db.rollback()
        raise HTTPException(status_code=500, detail="db commit failed")

    # ... 이후 DB 저장/처리 로직 ...
    return {"ok": True}
