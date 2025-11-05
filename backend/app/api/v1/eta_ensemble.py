from fastapi import APIRouter, HTTPException, Query
from app.services.eta_ensemble import eta_ensemble_seconds

router = APIRouter(prefix="/v1/eta", tags=["eta"])

@router.get("/ensemble")
def eta_ensemble(vehicle_id: str = Query(...), stop_id: str = Query(...)):
    try:
        return eta_ensemble_seconds(vehicle_id, stop_id)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))
