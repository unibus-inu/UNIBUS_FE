from fastapi import FastAPI
from app.core.logging import setup_logging
from app.core.config import settings
from app.db.session import engine
from app.db.base import Base

from app.api.v1.health import router as health_router
from app.api.v1.ingest import router as ingest_router
from app.api.v1.vehicles import router as vehicles_router
from app.api.v1.stops import router as stops_router
from app.api.compat import router as compat_router


setup_logging()
app = FastAPI(title=settings.APP_NAME)

@app.on_event("startup")
def on_startup():
    Base.metadata.create_all(bind=engine)  # MVP: 자동 생성 (운영 전에는 Alembic 추천)

app.include_router(health_router, prefix="/v1")
app.include_router(ingest_router, prefix="/v1")
app.include_router(vehicles_router, prefix="/v1")
app.include_router(stops_router, prefix="/v1")
app.include_router(ingest_router)
app.include_router(compat_router)