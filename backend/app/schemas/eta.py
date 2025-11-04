from pydantic import BaseModel

class LatestOut(BaseModel):
    vehicle_id: str
    ts: int
    lat: float
    lon: float
    speed_mps: float | None = None
    heading: float | None = None

class EtaOut(BaseModel):
    stop_id: str
    route_id: str
    eta_seconds: int
    eta_confidence: str