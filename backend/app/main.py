from fastapi import FastAPI
from app.core.logging import setup_logging
from app.core.config import settings
from app.db.session import engine
from app.db.base import Base

from app.api.v1.health import router as health_router
from app.api.v1.ingest import router as ingest_router
from app.api.v1.vehicles import router as vehicles_router
from app.api.v1.stops import router as stops_router
from app.api.v1.route import router as route_router
from app.api.v1.eta import router as eta_router
from app.api.compat import router as compat_router
from app.api.v1.debug import router as debug_router


setup_logging()
app = FastAPI(title=settings.APP_NAME)

@app.on_event("startup")
def on_startup():
    # MVP 개발 단계: 자동 테이블 생성 (운영 전에는 Alembic 마이그레이션 권장)
    Base.metadata.create_all(bind=engine)

@app.get("/")
def root():
    return {
        "message": "INU Shuttle ETA API",
        "docs": "/docs",
        "v1_health": "/v1/healthz",
    }

@app.get("/health")
def health_root():
    return {"ok": True}

# v1 APIs
app.include_router(health_router,   prefix="/v1")
app.include_router(ingest_router,   prefix="/v1")
app.include_router(vehicles_router, prefix="/v1")
app.include_router(stops_router,    prefix="/v1")
app.include_router(route_router,    prefix="/v1")
app.include_router(eta_router,      prefix="/v1")

# Compatibility & aliases
app.include_router(ingest_router)   # alias: /ingest
app.include_router(compat_router)   # /api/*, /ws/*
app.include_router(debug_router, prefix="/v1")