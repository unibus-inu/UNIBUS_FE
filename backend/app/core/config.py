from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    APP_NAME: str = "INU Shuttle ETA"
    ENV: str = "dev"
    DB_URL: str = "sqlite:///./data/app.db"
    DEVICE_HMAC_REQUIRED: bool = True
    DEFAULT_DEVICE_SECRET: str = "CHANGE_ME"

    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

settings = Settings()