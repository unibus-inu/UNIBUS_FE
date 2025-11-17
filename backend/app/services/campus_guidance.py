"""Helpers that surface campus boarding + drop-off guidance."""
from __future__ import annotations
from typing import Dict, List, Optional
from sqlalchemy.orm import Session

from app.data.campus_buildings import BUILDINGS, BuildingDef, DEFAULT_BOARD_STOP_ID
from app.repositories.route_repo import get_stop
from app.utils.geo import haversine_m

AVG_WALK_MPS = 1.35  # ~4.9km/h

_BUILDING_BY_ID: Dict[str, BuildingDef] = {b.id: b for b in BUILDINGS}
_ALIAS_TO_ID: Dict[str, str] = {}

def _normalize(text: str) -> str:
    return text.strip().lower()

for b in BUILDINGS:
    for alias in (b.id, b.name, *b.aliases):
        key = _normalize(alias)
        if key:
            _ALIAS_TO_ID.setdefault(key, b.id)

def _stop_payload(stop) -> Dict[str, object]:
    return {
        "id": stop.id,
        "name": stop.name,
        "lat": stop.lat,
        "lon": stop.lon,
        "seq": stop.seq,
        "route_id": stop.route_id,
    }

def _build_payload(db: Session, building: BuildingDef) -> Optional[Dict[str, object]]:
    stop = get_stop(db, building.stop_id)
    if not stop:
        return None
    walk_m = haversine_m(building.lat, building.lon, stop.lat, stop.lon)
    walk_min = walk_m / max(AVG_WALK_MPS * 60.0, 1.0)
    return {
        "building_id": building.id,
        "building_name": building.name,
        "aliases": sorted(set(a for a in (building.name, *building.aliases) if a)),
        "lat": building.lat,
        "lon": building.lon,
        "recommended_stop": _stop_payload(stop),
        "walk_distance_m": round(walk_m, 1),
        "estimated_walk_minutes": round(walk_min, 1),
        "notes": building.notes,
    }

def get_default_board_stop(db: Session) -> Dict[str, object]:
    """Return the canonical boarding stop (인천대입구역)."""
    stop = get_stop(db, DEFAULT_BOARD_STOP_ID)
    if not stop:
        raise ValueError(f"default board stop {DEFAULT_BOARD_STOP_ID} missing in DB")
    return _stop_payload(stop)

def list_dropoff_guides(db: Session, query: Optional[str] = None) -> List[Dict[str, object]]:
    """Return all building drop-off suggestions, optionally filtered by query."""
    q = _normalize(query) if query else None
    rows: List[Dict[str, object]] = []
    for b in BUILDINGS:
        if q:
            haystack = [_normalize(b.id), _normalize(b.name)]
            haystack.extend(_normalize(alias) for alias in b.aliases)
            if not any(q in token for token in haystack):
                continue
        payload = _build_payload(db, b)
        if payload:
            rows.append(payload)
    rows.sort(key=lambda x: x["building_name"])
    return rows

def get_dropoff_for_building(db: Session, building_id_or_alias: str) -> Optional[Dict[str, object]]:
    """Lookup a single building by id or alias."""
    key = _normalize(building_id_or_alias)
    if not key:
        return None
    b_id = _ALIAS_TO_ID.get(key)
    if not b_id:
        return None
    building = _BUILDING_BY_ID.get(b_id)
    if not building:
        return None
    return _build_payload(db, building)

__all__ = [
    "get_default_board_stop",
    "list_dropoff_guides",
    "get_dropoff_for_building",
]
