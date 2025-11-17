from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, Field
from sqlalchemy.orm import Session
from datetime import datetime, timezone
import math
from app.db.session import get_db
from app.db import models

router = APIRouter()
UTC = timezone.utc

class RideSurveyIn(BaseModel):
    user_id: str | None = None
    vehicle_id: str = Field(min_length=1, max_length=64)
    board_stop: str = Field(min_length=1, max_length=128)
    board_time: datetime              # 클라가 ISO8601로 보냄 (예: 2025-11-17T09:05:00+09:00)
    class_building: str = Field(min_length=1, max_length=32)
    class_room: str = Field(min_length=1, max_length=32)
    class_start_time: datetime
    arrival_time: datetime

@router.post("/v1/survey/ride")
def submit_survey(p: RideSurveyIn, db: Session = Depends(get_db)):
    # UTC 정규화
    bt = p.board_time.astimezone(UTC)
    cs = p.class_start_time.astimezone(UTC)
    ar = p.arrival_time.astimezone(UTC)

    # 파생치 계산
    travel_min = max(0, round((ar - bt).total_seconds() / 60))
    delta_sec  = (ar - cs).total_seconds()
    late_min   = max(0, math.ceil(delta_sec / 60.0))
    early_min  = max(0, math.floor(-delta_sec / 60.0))

    row = models.RideSurvey(
        user_id=p.user_id, vehicle_id=p.vehicle_id, board_stop=p.board_stop,
        board_time=bt, class_building=p.class_building, class_room=p.class_room,
        class_start_time=cs, arrival_time=ar,
        travel_time_min=travel_min, early_min=early_min, late_min=late_min
    )
    try:
        db.add(row); db.commit()
    except Exception:
        db.rollback()
        raise HTTPException(500, "db commit failed")

    return {
        "ok": True,
        "early_min": early_min,
        "late_min": late_min,
        "travel_time_min": travel_min
    }

@router.post("/ride")
def submit_survey(p: RideSurveyIn, db: Session = Depends(get_db)):
    # UTC 정규화
    bt = p.board_time.astimezone(UTC)
    cs = p.class_start_time.astimezone(UTC)
    ar = p.arrival_time.astimezone(UTC)

    # 파생치 계산
    travel_min = max(0, round((ar - bt).total_seconds() / 60))
    delta_sec  = (ar - cs).total_seconds()
    late_min   = max(0, math.ceil(delta_sec / 60.0))
    early_min  = max(0, math.floor(-delta_sec / 60.0))

    row = models.RideSurvey(
        user_id=p.user_id, vehicle_id=p.vehicle_id, board_stop=p.board_stop,
        board_time=bt, class_building=p.class_building, class_room=p.class_room,
        class_start_time=cs, arrival_time=ar,
        travel_time_min=travel_min, early_min=early_min, late_min=late_min
    )
    try:
        db.add(row); db.commit()
    except Exception:
        db.rollback()
        raise HTTPException(500, "db commit failed")

    return {"ok": True, "early_min": early_min, "late_min": late_min, "travel_time_min": travel_min}
