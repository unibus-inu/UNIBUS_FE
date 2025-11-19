from datetime import datetime, timezone
from typing import Generator

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.orm import Session

from app.db import models
from app.db.session import SessionLocal

_bearer_scheme = HTTPBearer(auto_error=False)


def get_db() -> Generator:
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(_bearer_scheme),
    db: Session = Depends(get_db),
) -> models.User:
    if not credentials or not credentials.credentials:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="not authenticated")
    token_value = credentials.credentials.strip()
    if not token_value:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="not authenticated")

    token = (
        db.query(models.AuthToken)
        .filter(models.AuthToken.token == token_value)
        .first()
    )
    now = datetime.now(timezone.utc)
    if not token:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="invalid or expired token")
    expires_at = token.expires_at
    if expires_at.tzinfo is None:
        expires_at = expires_at.replace(tzinfo=timezone.utc)
    if expires_at <= now:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="invalid or expired token")
    if not token.user or not token.user.is_active:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="user disabled")
    return token.user
