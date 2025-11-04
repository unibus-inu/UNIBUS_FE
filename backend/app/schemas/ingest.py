from pydantic import BaseModel, Field
from typing import Optional

class IngestIn(BaseModel):
    vehicle_id: str
    ts: int
    lat: float
    lon: float
    speed_mps: Optional[float] = Field(default=None)
    heading: Optional[float] = Field(default=None)
    hdop: Optional[float] = Field(default=None)