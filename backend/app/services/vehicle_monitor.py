import time
from typing import Dict, List

from sqlalchemy.orm import Session

from app.core.config import settings
from app.db import models
from app.utils.geo import haversine_m


def _setting(name: str, default):
    return getattr(settings, name, default)


def _gather_recent_positions(db: Session, vehicle_id: str, since_ts: int, limit: int = 200):
    return (
        db.query(models.Position)
        .filter(
            models.Position.vehicle_id == vehicle_id,
            models.Position.ts >= since_ts,
        )
        .order_by(models.Position.ts.desc())
        .limit(limit)
        .all()
    )


def classify_vehicle_status(db: Session, vehicle_id: str, now_ts: int | None = None) -> Dict[str, object]:
    now_ts = now_ts or int(time.time())
    latest = (
        db.query(models.Position)
        .filter(models.Position.vehicle_id == vehicle_id)
        .order_by(models.Position.ts.desc())
        .first()
    )
    no_signal_sec = _setting("MONITOR_NO_SIGNAL_SEC", 120)
    if not latest:
        return {
            "vehicle_id": vehicle_id,
            "state": "no_signal",
            "reason": "no telemetry recorded",
            "latest_ts": None,
            "seconds_since_update": None,
            "metrics": {},
        }
    seconds_since = max(0, now_ts - int(latest.ts))
    if seconds_since >= no_signal_sec:
        return {
            "vehicle_id": vehicle_id,
            "state": "no_signal",
            "reason": f"last update {seconds_since}s ago",
            "latest_ts": int(latest.ts),
            "seconds_since_update": seconds_since,
            "metrics": {},
        }

    lookback_sec = _setting("MONITOR_LOOKBACK_SEC", 600)
    rows_desc = _gather_recent_positions(db, vehicle_id, latest.ts - lookback_sec)
    if len(rows_desc) < 2:
        return {
            "vehicle_id": vehicle_id,
            "state": "moving",
            "reason": "insufficient history, assuming moving",
            "latest_ts": int(latest.ts),
            "seconds_since_update": seconds_since,
            "metrics": {"samples": len(rows_desc)},
        }

    rows = list(reversed(rows_desc))
    total_distance = 0.0
    for a, b in zip(rows, rows[1:]):
        total_distance += haversine_m(a.lat, a.lon, b.lat, b.lon)
    duration = max(1, rows[-1].ts - rows[0].ts)
    avg_speed = total_distance / duration

    stall_radius = _setting("MONITOR_STALL_RADIUS_M", 20.0)
    congestion_speed = _setting("MONITOR_CONGESTION_SPEED_MPS", 1.5)
    last_move_ts = int(latest.ts)
    for sample in rows_desc:  # descending order, latest first
        dist = haversine_m(latest.lat, latest.lon, sample.lat, sample.lon)
        spd = float(sample.speed_mps or 0.0)
        if dist > stall_radius or spd > congestion_speed:
            last_move_ts = int(sample.ts)
            break
    stopped_duration = int(latest.ts) - last_move_ts
    stall_sec = _setting("MONITOR_STALL_SEC", 180)

    metrics = {
        "samples": len(rows_desc),
        "lookback_sec": lookback_sec,
        "avg_speed_mps": round(avg_speed, 3),
        "total_distance_m": round(total_distance, 1),
        "stopped_duration_sec": stopped_duration,
        "stall_radius_m": stall_radius,
    }

    if stopped_duration >= stall_sec:
        if last_move_ts != int(latest.ts):
            return {
                "vehicle_id": vehicle_id,
                "state": "congestion_stop",
                "reason": f"no movement >{stall_radius}m for {stopped_duration}s (recent motion existed)",
                "latest_ts": int(latest.ts),
                "seconds_since_update": seconds_since,
                "metrics": metrics,
            }
        return {
            "vehicle_id": vehicle_id,
            "state": "stalled_sensor",
            "reason": f"telemetry updating but location unchanged for {stopped_duration}s",
            "latest_ts": int(latest.ts),
            "seconds_since_update": seconds_since,
            "metrics": metrics,
        }

    return {
        "vehicle_id": vehicle_id,
        "state": "moving",
        "reason": "location updates look healthy",
        "latest_ts": int(latest.ts),
        "seconds_since_update": seconds_since,
        "metrics": metrics,
    }


def classify_all_vehicle_statuses(db: Session, now_ts: int | None = None) -> List[Dict[str, object]]:
    ids = [row.id for row in db.query(models.Vehicle.id).all()]
    results: List[Dict[str, object]] = []
    for vid in ids:
        results.append(classify_vehicle_status(db, vid, now_ts=now_ts))
    return results
