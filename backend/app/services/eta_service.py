from sqlalchemy.orm import Session
from app.repositories.position_repo import recent_speeds
from app.utils.smoothing import ema

def effective_speed_mps(db: Session, vehicle_id: str, now_ts: int) -> float:
    speeds = [s for s in recent_speeds(db, vehicle_id, now_ts-60) if s and s>0.3]
    v = ema(speeds, 0.4) if speeds else None
    if v is None: v = 5.0            # fallback 18km/h
    return max(1.5, min(v, 15.0))     # clamp

def eta_seconds(remaining_m: float, eff_speed_mps: float) -> int:
    dwell = 12  # 평균 정차 보정
    return int(remaining_m / eff_speed_mps + dwell)