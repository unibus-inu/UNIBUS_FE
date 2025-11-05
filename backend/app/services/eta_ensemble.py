# app/services/eta_ensemble.py
from __future__ import annotations
from typing import Dict, Any, Tuple, List
from statistics import mean
import math, time, inspect
import json
from collections import deque

from sqlalchemy.orm import Session
from app.db.session import SessionLocal
from app.db import models
from app.integrations.tmap import car_route_distance_time as tmap_dt
from app.integrations.kakao import car_route_distance_time as kakao_dt
from app.services.eta_service import effective_speed_mps

# ── type coercion helpers (ensure all coords are floats) ─────────────
def _as_pair(obj) -> Tuple[float, float]:
    # Accept (lat, lon) in dicts or 2-seq; coerce to floats
    if isinstance(obj, dict):
        return (float(obj["lat"]), float(obj["lon"]))
    if isinstance(obj, (list, tuple)) and len(obj) == 2:
        return (float(obj[0]), float(obj[1]))
    raise ValueError(f"unsupported lat/lon pair: {obj!r}")

def _coerce_polyline(poly) -> List[Tuple[float, float]]:
    """Return a list of (lat, lon) float tuples.
    Accepts a native list/tuple of pairs, or a JSON-encoded string of that.
    """
    # Some DB backends may return JSON columns as TEXT. If so, parse it.
    if isinstance(poly, str):
        try:
            poly = json.loads(poly)
        except Exception as e:
            raise ValueError(f"route.polyline is a string but not valid JSON: {e}")
    return [_as_pair(p) for p in poly]

def _resolve_effective_speed(db, vehicle_id: str, route_id: str) -> float:
    """
    Call effective_speed_mps with whatever signature it supports.
    Falls back to a sane default if unavailable.
    """
    try:
        sig = inspect.signature(effective_speed_mps)
        kwargs = {}
        if "db" in sig.parameters: kwargs["db"] = db
        if "vehicle_id" in sig.parameters: kwargs["vehicle_id"] = vehicle_id
        if "route_id" in sig.parameters: kwargs["route_id"] = route_id
        v = effective_speed_mps(**kwargs)
        return float(v)
    except Exception:
        # default cruise speed (m/s) if model not available
        return 16.0

# ── cache: include latest vehicle ts in the key ────────────────────────
# key = (vehicle_id, stop_id, latest_ts)
_CACHE: Dict[Tuple[str, str, int], Tuple[float, Dict[str, Any]]] = {}
_TTL = 3.0  # seconds (짧게; 디버깅용)

# ── tuning knobs ─────────────────────────────────────────────────────
ARRIVE_NEAR_M = 40.0          # within this distance = treat as arriving
PROVIDER_FACTOR = 2.0          # provider ETA must be within ±x of baseline
PROVIDER_ABS_SEC = 90          # or within this many seconds
SMOOTH_WINDOW = 5              # median over last N ETAs

# smoothing memory: key = (vehicle_id, stop_id) -> deque of recent ETAs
_SMOOTH_HISTORY: Dict[Tuple[str, str], deque] = {}

def _cache_get(key):
    now = time.monotonic()
    hit = _CACHE.get(key)
    if hit and hit[0] > now:
        return hit[1]
    return None

def _cache_set(key, payload: Dict[str, Any]):
    _CACHE[key] = (time.monotonic() + _TTL, payload)

def _confidence_from(providers: Dict[str, Any]) -> str:
    ok = [k for k,v in providers.items() if v.get("ok")]
    if "baseline" in ok and any(p in ok for p in ("kakao","tmap")):
        # if provider ETA close to baseline, call it high
        try:
            base = float(providers["baseline"].get("eta_s", 0))
            cands = []
            for p in ("kakao","tmap"):
                if p in providers and providers[p].get("ok"):
                    val = providers[p].get("eta_from_distance_s")
                    if isinstance(val, (int,float)):
                        cands.append(float(val))
            if cands and all(abs(x-base) <= 45 for x in cands):
                return "high"
        except Exception:
            pass
        return "mid"
    return "low"

def _accept_provider_eta(eta_s: float, baseline_s: float, dist_m: float, rem_route_m: float) -> bool:
    # reject zeros when route-remaining is large
    if eta_s is None or eta_s <= 0:
        return False
    if rem_route_m > 150 and eta_s < 10:
        return False
    if baseline_s <= 0:
        return True
    if abs(eta_s - baseline_s) <= PROVIDER_ABS_SEC:
        return True
    if (baseline_s/PROVIDER_FACTOR) <= eta_s <= (baseline_s*PROVIDER_FACTOR):
        return True
    return False

# ── geom helpers ───────────────────────────────────────────────────────
def _hav(a: Tuple[float,float], b: Tuple[float,float]) -> float:
    # (lat,lon) meters
    R=6371000.0
    la1,lo1 = math.radians(a[0]), math.radians(a[1])
    la2,lo2 = math.radians(b[0]), math.radians(b[1])
    dlat, dlon = la2-la1, lo2-lo1
    h = math.sin(dlat/2)**2 + math.cos(la1)*math.cos(la2)*math.sin(dlon/2)**2
    return 2*R*math.asin(min(1.0, math.sqrt(h)))

def _poly_len(poly: List[Tuple[float,float]]) -> float:
    return sum(_hav(poly[i], poly[i+1]) for i in range(len(poly)-1))

# ── local map-matching (no external dependency on route_service) ───────
def _meters_scale(lat_deg: float) -> Tuple[float, float]:
    # approximate meters per degree at given latitude
    lat_rad = math.radians(lat_deg)
    m_per_deg_lat = 111_320.0
    m_per_deg_lon = 111_320.0 * math.cos(lat_rad)
    return m_per_deg_lat, m_per_deg_lon

def _to_xy(lat: float, lon: float, ref_lat: float, ref_lon: float) -> Tuple[float,float]:
    mlat, mlon = _meters_scale(ref_lat)
    # x east-west, y north-south
    x = (lon - ref_lon) * mlon
    y = (lat - ref_lat) * mlat
    return (x, y)

def _project_point_on_segment(a: Tuple[float,float], b: Tuple[float,float], p: Tuple[float,float]) -> Tuple[float, float, float]:
    """
    Project geographic point p onto segment a->b using a local tangent plane.
    Returns (t_clamped in [0,1], lateral_distance_m, seg_len_meters).
    """
    ref_lat = (a[0] + b[0]) * 0.5
    ref_lon = (a[1] + b[1]) * 0.5
    ax, ay = _to_xy(a[0], a[1], ref_lat, ref_lon)
    bx, by = _to_xy(b[0], b[1], ref_lat, ref_lon)
    px, py = _to_xy(p[0], p[1], ref_lat, ref_lon)

    ex, ey = (bx - ax), (by - ay)
    vx, vy = (px - ax), (py - ay)
    denom = ex*ex + ey*ey
    t = 0.0 if denom == 0 else (vx*ex + vy*ey) / denom
    t_clamped = 0.0 if t < 0.0 else (1.0 if t > 1.0 else t)

    projx, projy = (ax + t_clamped*ex, ay + t_clamped*ey)
    dx, dy = (px - projx), (py - projy)
    lateral_m = (dx*dx + dy*dy) ** 0.5

    seg_len_m = _hav(a, b)  # use haversine for segment length
    return t_clamped, lateral_m, seg_len_m

def _mapmatch_remaining(curr: Tuple[float,float], poly: List[Tuple[float,float]]) -> Tuple[float, float]:
    """
    Return (remaining_to_end_m, lateral_offset_m) by projecting current point
    to the closest polyline segment and measuring progress along the polyline.
    """
    if not poly or len(poly) < 2:
        return 0.0, 0.0

    total_len_m = 0.0
    seg_lens: List[float] = []
    for i in range(len(poly)-1):
        L = _hav(poly[i], poly[i+1])
        seg_lens.append(L)
        total_len_m += L

    best_lat = float("inf")
    best_s_along = 0.0
    cum_before = 0.0
    for i in range(len(poly)-1):
        a, b = poly[i], poly[i+1]
        t, lateral_m, seg_len_m = _project_point_on_segment(a, b, curr)
        s_here = cum_before + (t * seg_len_m)
        if lateral_m < best_lat:
            best_lat = lateral_m
            best_s_along = s_here
        cum_before += seg_len_m

    remaining_m = max(0.0, total_len_m - best_s_along)
    return remaining_m, best_lat

# ── data helpers ───────────────────────────────────────────────────────
def _get_vehicle_and_stop(db: Session, vehicle_id: str, stop_id: str):
    vpos = (db.query(models.VehiclePosition)
              .filter(models.VehiclePosition.vehicle_id == vehicle_id)
              .order_by(models.VehiclePosition.ts.desc())
              .first())
    if not vpos:
        raise ValueError(f"no latest position for {vehicle_id}")
    stop = db.get(models.Stop, stop_id)
    if not stop:
        raise ValueError(f"no stop {stop_id}")
    route = db.get(models.Route, stop.route_id)
    if not route or not getattr(route, "polyline", None):
        raise RuntimeError(f"route {getattr(stop, 'route_id', None)} has no polyline")
    return vpos, stop, route

# ── main ───────────────────────────────────────────────────────────────
def eta_ensemble_seconds(vehicle_id: str, stop_id: str) -> Dict[str, Any]:
    db: Session = SessionLocal()
    try:
        # 최신 위치 읽고, 그 ts로 캐시키 구성
        vpos, stop, route = _get_vehicle_and_stop(db, vehicle_id, stop_id)
        cache_key = (vehicle_id, stop_id, int(vpos.ts))
        cached = _cache_get(cache_key)
        if cached:
            return cached

        # 폴리라인, 좌표 모두 float로 강제
        poly: List[Tuple[float,float]] = _coerce_polyline(route.polyline)  # [(lat,lon), ...] as floats
        total_len_m = _poly_len(poly)
        v_lat, v_lon = float(vpos.lat), float(vpos.lon)
        s_lat, s_lon = float(stop.lat), float(stop.lon)

        # 현재 위치에서 루트 말단까지 남은거리(폴리라인 따라감) + 측면 오프셋
        rem_m, lateral_m = _mapmatch_remaining((v_lat, v_lon), poly)
        s_now = total_len_m - rem_m  # 루트 시작점으로부터 진행량

        # 정거장별 s_stop 계산: s_stop = total_len - (그 정거장에서 끝까지 rem)
        # (루프 노선 기준, 앞으로 갈 거리: (s_stop - s_now + total_len) % total_len)
        stops = (db.query(models.Stop)
                   .filter(models.Stop.route_id == route.id)
                   .order_by(models.Stop.seq.asc())
                   .all())
        stop_ids = [s.id for s in stops]
        rem_from_stop: Dict[str,float] = {}
        s_stop: Dict[str,float] = {}
        for s in stops:
            r_stop, _lat = _mapmatch_remaining((float(s.lat), float(s.lon)), poly)
            rem_from_stop[s.id] = r_stop
            s_stop[s.id] = total_len_m - r_stop

        # 목표 정거장까지 남은거리(경로따라)
        dist_to_target_m = (s_stop[stop_id] - s_now + total_len_m) % total_len_m

        # 우리 속도모델
        v_eff = max(0.1, _resolve_effective_speed(db, vehicle_id, route.id))
        eta_baseline_s = int(math.ceil(dist_to_target_m / v_eff))

        providers: Dict[str, Any] = {
            "baseline": {
                "ok": True,
                "remaining_m": round(dist_to_target_m, 1),
                "eff_speed_mps": round(v_eff, 2),
                "eta_s": eta_baseline_s,
                "lateral_m": round(lateral_m, 1),
            }
        }
        cands = [eta_baseline_s]
        method_parts = ["baseline"]
        baseline_s = float(eta_baseline_s)

        # T map: distance only → convert with v_eff, then gate
        try:
            dist_m_tmap, _ = tmap_dt(v_lon, v_lat, s_lon, s_lat)
            eta_tmap = int(math.ceil(dist_m_tmap / v_eff))
            ok_tmap = _accept_provider_eta(eta_tmap, baseline_s, dist_m_tmap, dist_to_target_m)
            providers["tmap"] = {
                "ok": bool(ok_tmap),
                "distance_m": int(dist_m_tmap),
                "eta_from_distance_s": eta_tmap,
            }
            if ok_tmap:
                cands.append(eta_tmap); method_parts.append("tmap_distance")
        except Exception as e:
            providers["tmap"] = {"ok": False, "error": str(e)}

        # Kakao: distance only → convert with v_eff, then gate
        try:
            dist_m_kakao, _ = kakao_dt(v_lon, v_lat, s_lon, s_lat)
            eta_kakao = int(math.ceil(dist_m_kakao / v_eff))
            ok_kakao = _accept_provider_eta(eta_kakao, baseline_s, dist_m_kakao, dist_to_target_m)
            providers["kakao"] = {
                "ok": bool(ok_kakao),
                "distance_m": int(dist_m_kakao),
                "eta_from_distance_s": eta_kakao,
            }
            if ok_kakao:
                cands.append(eta_kakao); method_parts.append("kakao_distance")
        except Exception as e:
            providers["kakao"] = {"ok": False, "error": str(e)}

        # near-stop gating: clamp very small remaining distances to small ETAs
        eta_raw = int(round(mean(cands))) if cands else 0
        if dist_to_target_m <= ARRIVE_NEAR_M:
            eta_raw = min(10, max(0, int(math.ceil(dist_to_target_m / max(0.1, v_eff)))))

        # smoothing: median over last N
        sm_key = (vehicle_id, stop_id)
        dq = _SMOOTH_HISTORY.get(sm_key)
        if dq is None:
            dq = deque(maxlen=SMOOTH_WINDOW)
            _SMOOTH_HISTORY[sm_key] = dq
        dq.append(eta_raw)
        eta_sorted = sorted(dq)
        eta = int(eta_sorted[len(eta_sorted)//2])  # median

        conf = _confidence_from(providers)

        # 디버그에 모든 중간값 박제
        debug_details = {
            "vehicle": {
                "id": vehicle_id, "ts": int(vpos.ts),
                "lat": vpos.lat, "lon": vpos.lon,
            },
            "route": {"id": route.id, "total_len_m": round(total_len_m, 1)},
            "progress": {
                "s_now": round(s_now, 1),
                "rem_to_end_m": round(rem_m, 1),
                "lateral_m": round(lateral_m, 1),
            },
            "to_stops": {
                sid: {
                    "s_stop": round(s_stop[sid], 1),
                    "dist_forward_m": round(((s_stop[sid] - s_now + total_len_m) % total_len_m), 1),
                }
                for sid in stop_ids
            },
            "speed_model": {
                "v_eff_mps": round(v_eff, 2),
                "note": "effective_speed_mps output; plug components here if you have them",
            },
            "providers_raw": providers,
            "cache_key": {"vehicle_id": vehicle_id, "stop_id": stop_id, "ts": int(vpos.ts)},
            "cache_ttl_s": _TTL,
            "smoothing": {
                "window": SMOOTH_WINDOW,
                "history": list(dq),
                "eta_raw": eta_raw,
                "eta_median": eta
            },
        }

        resp = {
            "vehicle_id": vehicle_id,
            "route_id": route.id,
            "stop_id": stop_id,
            "eta_seconds": eta,
            "confidence": conf,
            "providers": providers,
            "method": "mean(" + ",".join(method_parts) + ")",
            "debug_details": debug_details,  # ← 여기!
        }
        _cache_set(cache_key, resp)
        return resp
    finally:
        db.close()