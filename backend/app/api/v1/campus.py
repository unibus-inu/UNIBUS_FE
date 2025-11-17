from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from app.api.deps import get_db
from app.services import campus_guidance

router = APIRouter(prefix="/v1/campus", tags=["campus"])

@router.get("/boarding-stop")
def boarding_stop(db: Session = Depends(get_db)):
    """Return the canonical boarding location (인천대입구역)."""
    try:
        return campus_guidance.get_default_board_stop(db)
    except ValueError as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/dropoff-guides")
def dropoff_guides(
    q: str | None = Query(default=None, description="building name/id filter"),
    db: Session = Depends(get_db),
):
    """List drop-off suggestions per building along with the default boarding stop."""
    guides = campus_guidance.list_dropoff_guides(db, q)
    try:
        board = campus_guidance.get_default_board_stop(db)
    except ValueError as e:
        raise HTTPException(status_code=500, detail=str(e))
    return {
        "default_board_stop": board,
        "dropoff_guides": guides,
    }

@router.get("/dropoff-guides/{building_id}")
def dropoff_detail(building_id: str, db: Session = Depends(get_db)):
    result = campus_guidance.get_dropoff_for_building(db, building_id)
    if not result:
        raise HTTPException(status_code=404, detail="building not found")
    return result

__all__ = ["router"]
