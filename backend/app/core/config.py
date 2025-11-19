# backend/app/core/config.py
from __future__ import annotations

# Prefer pydantic-settings v2; gracefully fall back to pydantic v1 BaseSettings if needed.
try:
    from pydantic_settings import BaseSettings, SettingsConfigDict  # pydantic-settings >=2
    SETTINGS_V2 = True
except Exception:  # pragma: no cover
    from pydantic import BaseSettings  # type: ignore
    SETTINGS_V2 = False
    SettingsConfigDict = None  # type: ignore

from typing import List
from pathlib import Path

# Resolve env file locations independent of current working directory
BACKEND_DIR = Path(__file__).resolve().parents[1]   # .../backend
PROJECT_DIR = BACKEND_DIR.parent                    # project root
ENV_FILES = (str(BACKEND_DIR / ".env"), str(PROJECT_DIR / ".env"))

class Settings(BaseSettings):
    # ==== general/app info ====
    APP_NAME: str = "INUBUS"
    APP_VERSION: str = "0.1.0"
    BACKEND_CORS_ORIGINS: List[str] = ["*"]

    # ==== ETA provider ====
    # "internal" | "kakao" | "tmap" | "hybrid" | "hybrid_tmap"
    ETA_PROVIDER: str = "internal"

    # ==== external API keys ====
    KAKAO_REST_API_KEY: str | None = None
    TMAP_APP_KEY: str | None = None

    # ==== auth ====
    AUTH_TOKEN_TTL_MINUTES: int = 60 * 24  # default: 1 day
    PASSWORD_HASH_ITERATIONS: int = 320_000

    # ==== vehicle monitoring ====
    MONITOR_NO_SIGNAL_SEC: int = 120
    MONITOR_LOOKBACK_SEC: int = 600
    MONITOR_STALL_SEC: int = 180
    MONITOR_STALL_RADIUS_M: float = 20.0
    MONITOR_CONGESTION_SPEED_MPS: float = 1.5

    # ==== database ====
    # default to a local sqlite DB under backend/data/
    DB_URL: str = "sqlite:///./data/app.db"
    DB_ECHO: bool = False

    # ==== ingest security ====
    INGEST_SECRET: str | None = None
    INGEST_ALLOW_UNSIGNED: bool = False

    # Device-side HMAC toggle & header names (for ingest endpoints)
    DEVICE_HMAC_REQUIRED: bool = False
    DEVICE_HMAC_TS_HEADER: str = "X-Ingest-Ts"
    DEVICE_HMAC_SIG_HEADERS: List[str] = ["X-Ingest-Sign", "X-Ingest-Signature", "X-Device-Signature"]

    # ==== settings loader ====
    if SETTINGS_V2:
        # pydantic-settings v2 style
        model_config = SettingsConfigDict(
            env_file=ENV_FILES,
            env_file_encoding="utf-8",
            extra="ignore",
            case_sensitive=False,
            env_prefix="",  # use var names as-is (e.g., INGEST_SECRET)
        )

# Instantiate a singleton settings object for import as `from app.core.config import settings`
settings = Settings()

__all__ = ["Settings", "settings"]
