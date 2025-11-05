# -*- coding: utf-8 -*-
# File: backend/scripts/playback_polyline.py
import time
import math
import requests
import argparse
import os
import hmac
import hashlib
import json

def bearing_deg(a, b):
    """
    Compute forward bearing in degrees from point a -> b.
    a, b: [lat, lon]
    """
    y1, x1 = map(math.radians, a)
    y2, x2 = map(math.radians, b)
    dlon = x2 - x1
    x = math.sin(dlon) * math.cos(y2)
    y = math.cos(y1) * math.sin(y2) - math.sin(y1) * math.cos(y2) * math.cos(dlon)
    deg = (math.degrees(math.atan2(x, y)) + 360.0) % 360.0
    return deg

def _sign(ts: int, body_str: str) -> dict:
    """
    Generate HMAC-SHA256 signature headers for ingest.
    Message = f"{ts}.{body_str}"
    Key     = INGEST_SECRET (env)
    If no secret is set, returns empty headers (unsigned mode).
    """
    sec = os.environ.get("INGEST_SECRET")
    if not sec:
        return {}
    sig = hmac.new(sec.encode(), f"{ts}.{body_str}".encode(), hashlib.sha256).hexdigest()
    return {"X-Ingest-Ts": str(ts), "X-Ingest-Sign": sig}

def main(base, vehicle_id, route_id, speed_mps, interval_s, steps):
    # 1) Fetch polyline from backend
    r = requests.get(f"{base}/v1/route/{route_id}", timeout=5)
    r.raise_for_status()
    poly = r.json().get("polyline") or []
    if len(poly) < 2:
        raise SystemExit(f"polyline empty for route_id={route_id}")

    print(f"[playback] route={route_id} vertices={len(poly)} interval={interval_s}s speed={speed_mps}m/s")

    # 2) Walk along polyline and ingest positions
    i = 0
    while steps < 0 or i < steps:
        a = poly[i % len(poly)]
        b = poly[(i + 1) % len(poly)]
        heading = bearing_deg(a, b)

        payload = {
            "vehicle_id": vehicle_id,
            "ts": int(time.time()),
            "lat": float(a[0]),
            "lon": float(a[1]),
            "speed_mps": float(speed_mps),
            "heading": float(heading),
        }

        # IMPORTANT: Build exact JSON bytes for signing and send via data= with explicit headers
        body = json.dumps(payload, separators=(',', ':'))
        headers = {"Content-Type": "application/json"}
        headers.update(_sign(payload["ts"], body))

        try:
            rr = requests.post(f"{base}/v1/ingest", data=body, headers=headers, timeout=5)
            if rr.status_code >= 300:
                print(f"[ingest] HTTP {rr.status_code} -> {rr.text[:200]}")
            else:
                print(f"[ingest] {i} OK")
        except requests.RequestException as e:
            print(f"[ingest] error: {e}")

        i += 1
        time.sleep(interval_s)

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", default="http://127.0.0.1:8000", help="backend base URL")
    ap.add_argument("--vehicle-id", default="bus-01")
    ap.add_argument("--route-id", default="inu-a")
    ap.add_argument("--speed", type=float, default=6.0, help="m/s")
    ap.add_argument("--interval", type=float, default=1.0, help="seconds between samples")
    ap.add_argument("--steps", type=int, default=120, help="-1 = infinite")
    args = ap.parse_args()
    main(args.base, args.vehicle_id, args.route_id, args.speed, args.interval, args.steps)
