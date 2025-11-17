# tools/replay_dummy.py
"""
Simple helper that replays vehicle telemetry along the stored route polyline.
ENV knobs:
    URL           ingest endpoint (default: http://localhost:8000/v1/ingest)
    SEC           shared secret for HMAC
    VID           vehicle_id to emit
    API_BASE      base URL used to fetch /v1/route/{route_id}
    ROUTE_ID      which route's polyline to follow (default: inu-a)
    STEP_METERS   target spacing between generated points (default: 40m)
    LOOP_COUNT    number of laps (0 = infinite)
    SLEEP_SEC     delay between points (default: 1.2s)
"""
import time, json, hmac, hashlib, requests, os, math, itertools, sys
from typing import Iterable, List, Tuple

URL = os.getenv("URL", "http://localhost:8000/v1/ingest")
SEC = os.getenv("SEC", "your-shared-secret")
VID = os.getenv("VID", "bus-001")
API_BASE = os.getenv("API_BASE", "http://localhost:8000")
ROUTE_ID = os.getenv("ROUTE_ID", "inu-a")
STEP_METERS = float(os.getenv("STEP_METERS", "40"))
LOOP_COUNT = int(os.getenv("LOOP_COUNT", "1"))
SLEEP_SEC = float(os.getenv("SLEEP_SEC", "1.2"))

def hmac_hex(sec: str, body: bytes) -> str:
    return hmac.new(sec.encode(), body, hashlib.sha256).hexdigest()

def heading(a: Tuple[float,float], b: Tuple[float,float]) -> float:
    (lat1,lon1),(lat2,lon2)=a,b
    y=math.sin(math.radians(lon2-lon1))*math.cos(math.radians(lat2))
    x=math.cos(math.radians(lat1))*math.sin(math.radians(lat2)) - \
      math.sin(math.radians(lat1))*math.cos(math.radians(lat2))*math.cos(math.radians(lon2-lon1))
    return (math.degrees(math.atan2(y,x))+360)%360

def haversine_m(lat1, lon1, lat2, lon2):
    R = 6371000.0
    dlat = math.radians(lat2 - lat1)
    dlon = math.radians(lon2 - lon1)
    a = math.sin(dlat/2)**2 + math.cos(math.radians(lat1))*math.cos(math.radians(lat2))*math.sin(dlon/2)**2
    return 2 * R * math.asin(min(1.0, math.sqrt(a)))

def fetch_polyline() -> List[Tuple[float,float]]:
    route_url = f"{API_BASE.rstrip('/')}/v1/route/{ROUTE_ID}"
    try:
        r = requests.get(route_url, timeout=5)
        r.raise_for_status()
        data = r.json()
        coords = data.get("polyline") or []
        if not isinstance(coords, list):
            raise ValueError("polyline format invalid")
        return [(float(lat), float(lon)) for lat, lon in coords]
    except Exception as e:
        print(f"[WARN] failed to fetch route polyline ({e}); falling back to demo path", file=sys.stderr)
        return [
            (37.375120, 126.632910),
            (37.376200, 126.634000),
            (37.377450, 126.635300),
            (37.378900, 126.636700),
            (37.380200, 126.637900),
        ]

def densify(polyline: List[Tuple[float,float]], step_m: float) -> Iterable[Tuple[float,float]]:
    """Yield interpolated points along the polyline roughly every step_m."""
    if not polyline:
        return []
    points: List[Tuple[float,float]] = [tuple(polyline[0])]
    for i in range(len(polyline) - 1):
        a = tuple(polyline[i])
        b = tuple(polyline[i + 1])
        seg_len = max(1.0, haversine_m(a[0], a[1], b[0], b[1]))
        steps = max(1, int(math.ceil(seg_len / max(step_m, 1.0))))
        for s in range(1, steps + 1):
            t = min(1.0, s / steps)
            lat = a[0] + (b[0] - a[0]) * t
            lon = a[1] + (b[1] - a[1]) * t
            points.append((lat, lon))
    return points

def iter_loops(points: List[Tuple[float,float]]):
    if LOOP_COUNT <= 0:
        loops = itertools.count()
    else:
        loops = range(LOOP_COUNT)
    for lap in loops:
        for idx, pt in enumerate(points):
            yield (lap, idx, pt)

def main():
    path = list(densify(fetch_polyline(), STEP_METERS))
    if len(path) < 2:
        print("[ERR] insufficient points to replay", file=sys.stderr)
        sys.exit(1)
    print(f"[info] Loaded {len(path)} points from route '{ROUTE_ID}', replaying with STEP={STEP_METERS}m")
    prev = path[0]
    for lap, idx, (lat, lon) in iter_loops(path):
        hdg = heading(prev, (lat, lon)) if prev else 0.0
        payload = {
            "vehicle_id": VID,
            "ts": int(time.time()),
            "lat": lat,
            "lon": lon,
            "speed_mps": 8.0,
            "heading": hdg
        }
        body = json.dumps(payload, separators=(',',':')).encode()
        sig  = hmac_hex(SEC, body)
        try:
            r = requests.post(URL, data=body, headers={
                "Content-Type":"application/json",
                "X-Device-Signature": sig
            }, timeout=5)
            print(f"[lap {lap}] #{idx}: {r.status_code} {r.text}")
        except Exception as e:
            print(f"[ERR] ingest failed at lap {lap} idx {idx}: {e}", file=sys.stderr)
        prev = (lat, lon)
        time.sleep(SLEEP_SEC)

if __name__ == "__main__":
    main()
