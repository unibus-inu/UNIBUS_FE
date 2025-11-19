"""
Generate a polyline for the dorm → 사범대 → 정보기술대 → dorm loop
using Kakao Mobility Directions.

Usage:
    export KAKAO_REST_API_KEY=your_key
    python scripts/gen_sabom_polyline.py > sabom_polyline.json

The script calls Kakao's /v1/directions for each leg and concatenates
the roads.vertexes coordinates into a single [[lat, lon], ...] polyline.
If Kakao API fails, a simple fallback polyline (straight connections)
is returned.
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parents[1]
if str(BASE_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_DIR))

from app.integrations.kakao import segment_polyline

SABOM_LOOP_POINTS = [
    ("dorm", 37.374114, 126.630295),      # 생활원 (기존 좌표)
    ("sabom", 37.38114, 126.6536),        # 사범대 정류장
    ("it_college", 37.37418, 126.6344),   # 정보기술대 (공과대 좌표 재사용)
    ("dorm", 37.374114, 126.630295),      # 생활원으로 회차
]


def _fallback_polyline():
    """Simple straight-line polyline when Kakao API is not available."""
    return [[lat, lon] for _, lat, lon in SABOM_LOOP_POINTS]


def build_polyline():
    coords = []
    for idx in range(len(SABOM_LOOP_POINTS) - 1):
        _, lat_a, lon_a = SABOM_LOOP_POINTS[idx]
        _, lat_b, lon_b = SABOM_LOOP_POINTS[idx + 1]
        try:
            segment = segment_polyline(lon_a, lat_a, lon_b, lat_b)
        except Exception as exc:
            print(f"[warn] Kakao API failed on leg {idx}: {exc}", file=sys.stderr)
            return _fallback_polyline()
        if not segment:
            continue
        if coords:
            segment = segment[1:]
        coords.extend(segment)

    return coords or _fallback_polyline()


def main():
    polyline = build_polyline()
    json.dump(polyline, sys.stdout, ensure_ascii=False, indent=2)
    sys.stdout.write("\n")


if __name__ == "__main__":
    main()
