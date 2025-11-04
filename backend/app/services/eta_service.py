import math
from typing import Tuple, Optional
from sqlalchemy.orm import Session

from app.core.config import settings
from app.db import models
from app.utils.geo import haversine_m

# ------------------------------------------------------------------------------
# Configuration
# ------------------------------------------------------------------------------
# internal | tmap | kakao | hybrid_tmap | hybrid_kakao
PROVIDER = getattr(settings, "ETA_PROVIDER", "internal").lower()

ARRIVAL_RADIUS_M   = 25      # within this radius => arrived
MIN_ETA_SEC        = 12      # minimum ETA for non-arrived cases
FIXED_BUFFER_SEC   = 0       # add fixed seconds to ETA if you want padding

DEFAULT_MPS        = 5.5     # ~19.8 km/h (campus default)
MIN_MPS            = 2.0     # clamp lower bound (heavy congestion)
MAX_MPS            = 16.0    # clamp upper bound (campus speed limit)
LOOKBACK_SEC       = 120     # recent window to estimate effective speed

# ------------------------------------------------------------------------------
# Small utils
# ------------------------------------------------------------------------------
def _clamp(v: float, lo: float, hi: float) -> float:
    return lo if v < lo else hi if v > hi else v

# ------------------------------------------------------------------------------
# ETA helpers
# ------------------------------------------------------------------------------
def eta_seconds(remaining_m: float, eff_speed_mps: float) -> int:
    """
    Convert remaining distance (m) and effective speed (m/s) to ETA seconds
    with arrival radius, min ETA, and optional fixed buffer.
    """
    if remaining_m <= ARRIVAL_RADIUS_M:
        return 0
    est = math.ceil(remaining_m / max(eff_speed_mps, 0.1))
    return max(est + FIXED_BUFFER_SEC, MIN_ETA_SEC)


def effective_speed_mps(
    db: Session,
    vehicle_id: str,
    now_ts: int,
    lookback_sec: int = LOOKBACK_SEC,
    default_mps: float = DEFAULT_MPS,
    min_mps: float = MIN_MPS,
    max_mps: float = MAX_MPS,
) -> float:
    """
    Estimate effective speed using recent position logs.
    Falls back to default when data is insufficient.
    """
    rows = (
        db.query(models.Position)
          .filter(
              models.Position.vehicle_id == vehicle_id,
              models.Position.ts <= now_ts,
              models.Position.ts >= now_ts - lookback_sec,
          )
          .order_by(models.Position.ts.desc())
          .limit(200)
          .all()
    )
    if len(rows) < 2:
        return default_mps

    dist_sum, dt_sum = 0.0, 0
    prev = rows[0]
    for r in rows[1:]:
        dist_sum += haversine_m(prev.lat, prev.lon, r.lat, r.lon)
        dt = max(1, prev.ts - r.ts)   # rows are in descending ts
        dt_sum += dt
        prev = r

    if dt_sum <= 0:
        return default_mps

    v = dist_sum / dt_sum
    return float(_clamp(v, min_mps, max_mps))

# ------------------------------------------------------------------------------
# External providers (optional)
# ------------------------------------------------------------------------------
def external_eta_tmap(curr_lat: float, curr_lon: float, stop_lat: float, stop_lon: float) -> Tuple[float, int]:
    """
    Returns (distance_m, eta_sec) from T map Directions. Raises on failure.
    """
    from app.integrations.tmap import car_route_distance_time
    dist_m, sec = car_route_distance_time(curr_lon, curr_lat, stop_lon, stop_lat)
    return dist_m, sec


def external_eta_kakao(curr_lat: float, curr_lon: float, stop_lat: float, stop_lon: float) -> Tuple[float, int]:
    """
    Returns (distance_m, eta_sec) from Kakao Directions. Raises on failure.
    """
    from app.integrations.kakao import directions_distance_time
    dist_m, sec = directions_distance_time(curr_lon, curr_lat, stop_lon, stop_lat)
    return dist_m, sec

# ------------------------------------------------------------------------------
# Hybrid combiner
# ------------------------------------------------------------------------------
def _hybrid_speed(
    rem_m_internal: float,
    v_internal: float,
    lateral_m: float,
    ext_dist_m: Optional[float],
    ext_eta_sec: Optional[int],
) -> float:
    """
    Blend internal speed with external (traffic-informed) speed.
    - Favor internal near the stop and when route-lengths disagree.
    - Favor external when far away and lengths are similar.
    """
    # If external unavailable, use internal
    if not ext_dist_m or not ext_eta_sec or ext_eta_sec <= 0:
        return v_internal

    # External effective speed from provider
    v_external = _clamp(ext_dist_m / max(ext_eta_sec, 1), MIN_MPS, MAX_MPS)

    # Heuristics for blending weight [0..1] toward external
    # (1) Distance gate: farther => trust external more
    w_dist = _clamp(rem_m_internal / 1500.0, 0.0, 1.0)  # >1.5 km => 1.0
    # (2) Lateral offset: off-route reduces external trust (we trust our route more)
    w_lat = 1.0 - _clamp(lateral_m / 50.0, 0.0, 1.0)    # 0m:1.0 → 50m:0.0
    # Combine
    w_ext = _clamp(0.6 * w_dist + 0.4 * w_lat, 0.0, 1.0)

    v_blend = w_ext * v_external + (1.0 - w_ext) * v_internal
    return float(_clamp(v_blend, MIN_MPS, MAX_MPS))

# ------------------------------------------------------------------------------
# Main estimator (chooses provider, falls back to internal or blends)
# ------------------------------------------------------------------------------
def estimate_eta_for(vehicle_pos: models.Position, stop: models.Stop, db: Session) -> Tuple[int, dict]:
    """
    Compute ETA to stop for the given vehicle position.
    Returns (eta_sec, debug_dict).
    """
    # Internal polyline-based first (we always compute; used in pure internal and as hybrid baseline)
    from app.services.route_service import remaining_distance_mapmatched
    route = db.get(models.Route, stop.route_id)
    rem_m, lateral_m = remaining_distance_mapmatched(
        db, route.id, route.polyline, vehicle_pos.lat, vehicle_pos.lon, stop.id
    )
    v_int = effective_speed_mps(db, vehicle_pos.vehicle_id, vehicle_pos.ts)
    eta_int = eta_seconds(rem_m, v_int)

    # External (optional)
    ext_dist_m: Optional[float] = None
    ext_eta_sec: Optional[int] = None

    try:
        if PROVIDER in ("tmap", "hybrid_tmap"):
            ext_dist_m, ext_eta_sec = external_eta_tmap(vehicle_pos.lat, vehicle_pos.lon, stop.lat, stop.lon)
        elif PROVIDER in ("kakao", "hybrid_kakao"):
            ext_dist_m, ext_eta_sec = external_eta_kakao(vehicle_pos.lat, vehicle_pos.lon, stop.lat, stop.lon)
    except Exception:
        # Ignore external errors; we fall back to internal
        pass

    # Provider modes
    if PROVIDER == "tmap" and ext_eta_sec is not None:
        return max(ext_eta_sec, 0), {"provider": "tmap", "remaining_m": ext_dist_m}
    if PROVIDER == "kakao" and ext_eta_sec is not None:
        return max(ext_eta_sec, 0), {"provider": "kakao", "remaining_m": ext_dist_m}

    # Hybrid or Internal
    if PROVIDER in ("hybrid_tmap", "hybrid_kakao") and ext_eta_sec is not None and ext_dist_m is not None:
        v_blend = _hybrid_speed(rem_m, v_int, lateral_m, ext_dist_m, ext_eta_sec)
        eta = eta_seconds(rem_m, v_blend)
        return eta, {
            "provider": PROVIDER,
            "remaining_m": rem_m,
            "lateral_m": lateral_m,
            "eff_speed_mps_internal": v_int,
            "eff_speed_mps_external": _clamp(ext_dist_m / max(ext_eta_sec, 1), MIN_MPS, MAX_MPS),
            "eff_speed_mps_blend": v_blend,
            "ext_dist_m": ext_dist_m,
            "ext_eta_sec": ext_eta_sec,
        }

    # Default: internal
    return eta_int, {
        "provider": "internal",
        "remaining_m": rem_m,
        "lateral_m": lateral_m,
        "eff_speed_mps": v_int,
    }