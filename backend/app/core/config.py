from pydantic_settings import BaseSettings, SettingsConfigDict
from typing import Optional


class Settings(BaseSettings):
    APP_NAME: str = "INU Shuttle ETA"
    ENV: str = "dev"
    DB_URL: str = "sqlite:///./data/app.db"
    DEVICE_HMAC_REQUIRED: bool = True
    DEFAULT_DEVICE_SECRET: str = "CHANGE_ME"
    ETA_PROVIDER: str = "internal"  # "internal" | "tmap" | "kakao"
    TMAP_APP_KEY: Optional[str] = None  # T map 경로/맵매칭용
    KAKAO_REST_API_KEY: Optional[str] = None  # Kakao Directions용
    ETA_PROVIDER: str = "internal"
    TMAP_APP_KEY: Optional[str] = None
    KAKAO_REST_API_KEY: Optional[str] = None

    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

settings = Settings()
