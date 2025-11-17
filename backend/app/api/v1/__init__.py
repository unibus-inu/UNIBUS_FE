from fastapi import APIRouter
from .vehicles import router as vehicles_router
from .survey import router as survey_router
from .ingest import router as ingest_router  # 이미 있을 것

api_router = APIRouter()
api_router.include_router(ingest_router)
api_router.include_router(vehicles_router)
api_router.include_router(survey_router)