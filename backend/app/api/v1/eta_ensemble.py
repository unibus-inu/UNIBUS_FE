from fastapi import APIRouter, HTTPException, Query
from app.services.eta_ensemble import eta_ensemble_seconds

router = APIRouter(prefix="/v1/eta", tags=["eta"])

@router.get("/ensemble")
def eta_ensemble(
    vehicle_id: str = Query(...),
    stop_id: str = Query(...),
    use_tmap: bool = Query(True),   # tmap 테스트용 스위치
    use_kakao: bool = Query(True),  # kakao 테스트용 스위치
):
    try:
        return eta_ensemble_seconds(
            vehicle_id,
            stop_id,
            use_tmap=use_tmap,
            use_kakao=use_kakao,
        )
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))
