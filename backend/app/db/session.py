from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from app.core.config import settings
import os

# SQLite 파일 디렉토리 준비
if settings.DB_URL.startswith("sqlite:///"):
    os.makedirs("./data", exist_ok=True)

engine = create_engine(
    settings.DB_URL,
    connect_args={"check_same_thread": False} if settings.DB_URL.startswith("sqlite") else {},
)
SessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False)
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()