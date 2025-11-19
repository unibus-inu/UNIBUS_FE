from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from fastapi.responses import HTMLResponse
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.api.deps import get_current_user, get_db
from app.core.config import settings
from app.db import models
from app.schemas.auth import LoginIn, SignupIn, TokenOut, UserOut
from app.schemas.common import OK
from app.utils.security import generate_token, hash_password, verify_password

router = APIRouter(prefix="/v1/auth", tags=["auth"])
UTC = timezone.utc
_logout_bearer = HTTPBearer(auto_error=False)


def _normalize_email(value: str) -> str:
    return value.strip().lower()


@router.post("/signup", response_model=UserOut, status_code=status.HTTP_201_CREATED)
def signup(payload: SignupIn, db: Session = Depends(get_db)):
    email = _normalize_email(payload.email)
    exists = db.query(models.User).filter(models.User.email == email).first()
    if exists:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="email already registered")
    new_user = models.User(
        email=email,
        full_name=payload.full_name,
        password_hash=hash_password(payload.password),
    )
    db.add(new_user)
    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="email already registered")
    db.refresh(new_user)
    return UserOut.model_validate(new_user)


@router.post("/login", response_model=TokenOut)
def login(payload: LoginIn, db: Session = Depends(get_db)):
    email = _normalize_email(payload.email)
    user = db.query(models.User).filter(models.User.email == email).first()
    if not user or not verify_password(payload.password, user.password_hash):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="invalid credentials")
    if not user.is_active:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="user disabled")

    now = datetime.now(UTC)
    ttl = getattr(settings, "AUTH_TOKEN_TTL_MINUTES", 1440) or 1440
    expires_at = now + timedelta(minutes=int(ttl))
    token_value = generate_token()
    session = models.AuthToken(user_id=user.id, token=token_value, expires_at=expires_at)
    db.add(session)
    db.commit()

    return TokenOut(access_token=token_value, expires_at=expires_at, user=UserOut.model_validate(user))


@router.get("/me", response_model=UserOut)
def me(current_user: models.User = Depends(get_current_user)):
    return UserOut.model_validate(current_user)


@router.post("/logout", response_model=OK)
def logout(
    current_user: models.User = Depends(get_current_user),
    credentials: HTTPAuthorizationCredentials = Depends(_logout_bearer),
    db: Session = Depends(get_db),
):
    if not credentials or not credentials.credentials:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="not authenticated")
    token_value = credentials.credentials.strip()
    (
        db.query(models.AuthToken)
        .filter(models.AuthToken.token == token_value, models.AuthToken.user_id == current_user.id)
        .delete()
    )
    db.commit()
    return OK()


HTML_TEMPLATE = """<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8" />
  <title>INUBUS Auth Playground</title>
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <style>
    body {font-family: system-ui, sans-serif; margin: 0; padding: 20px; background: #f5f6fa;}
    h1 {margin-top: 0;}
    .panel {background: #fff; border-radius: 8px; padding: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); margin-bottom: 18px;}
    label {display: block; margin-bottom: 6px; font-weight: 600;}
    input[type="text"], input[type="email"], input[type="password"] {
      width: 100%; padding: 8px 10px; border-radius: 4px; border: 1px solid #d0d7de; margin-bottom: 12px;
    }
    button {cursor: pointer; border: none; border-radius: 4px; padding: 8px 16px; font-weight: 600;}
    button.primary {background: #0d6efd; color: #fff;}
    button.secondary {background: #e9ecef; color: #333;}
    pre {background: #0b1724; color: #a5d6ff; padding: 12px; border-radius: 6px; overflow-x: auto;}
    .token-box {word-break: break-all;}
    .flex {display: flex; gap: 20px; flex-wrap: wrap;}
    .flex .panel {flex: 1; min-width: 260px;}
  </style>
</head>
<body>
  <h1>Auth Playground</h1>
  <p>간단한 데모 UI로 /v1/auth 엔드포인트를 실험해보세요.</p>

  <div class="flex">
    <div class="panel">
      <h2>회원가입</h2>
      <label>이메일</label>
      <input id="signup-email" type="email" placeholder="you@example.com" />
      <label>이름(선택)</label>
      <input id="signup-name" type="text" placeholder="홍길동" />
      <label>비밀번호</label>
      <input id="signup-pass" type="password" placeholder="****" />
      <button class="primary" onclick="submitSignup()">회원가입</button>
      <div id="signup-status"></div>
    </div>
    <div class="panel">
      <h2>로그인</h2>
      <label>이메일</label>
      <input id="login-email" type="email" placeholder="you@example.com" />
      <label>비밀번호</label>
      <input id="login-pass" type="password" placeholder="****" />
      <button class="primary" onclick="submitLogin()">로그인</button>
      <div id="login-status"></div>
    </div>
  </div>

  <div class="panel">
    <h2>토큰 & 내 정보</h2>
    <p class="token-box"><strong>Access Token:</strong> <span id="token-display">없음</span></p>
    <button class="secondary" onclick="callMe()">/v1/auth/me 호출</button>
    <button class="secondary" onclick="logout()">로그아웃</button>
    <h3>응답</h3>
    <pre id="me-output">-</pre>
  </div>

  <script>
    let token = null;

    async function submitSignup() {
      const payload = {
        email: document.getElementById("signup-email").value,
        full_name: document.getElementById("signup-name").value || null,
        password: document.getElementById("signup-pass").value,
      };
      const statusEl = document.getElementById("signup-status");
      statusEl.textContent = "요청 중...";
      try {
        const res = await fetch("/v1/auth/signup", {
          method: "POST",
          headers: {"Content-Type": "application/json"},
          body: JSON.stringify(payload),
        });
        const data = await res.json();
        statusEl.textContent = res.ok ? "✅ " + JSON.stringify(data) : "❌ " + (data.detail || res.statusText);
      } catch (err) {
        statusEl.textContent = "❌ " + err.message;
      }
    }

    async function submitLogin() {
      const payload = {
        email: document.getElementById("login-email").value,
        password: document.getElementById("login-pass").value,
      };
      const statusEl = document.getElementById("login-status");
      statusEl.textContent = "요청 중...";
      try {
        const res = await fetch("/v1/auth/login", {
          method: "POST",
          headers: {"Content-Type": "application/json"},
          body: JSON.stringify(payload),
        });
        const data = await res.json();
        if (res.ok) {
          token = data.access_token;
          document.getElementById("token-display").textContent = token;
          statusEl.textContent = "✅ 로그인 성공";
        } else {
          statusEl.textContent = "❌ " + (data.detail || res.statusText);
        }
      } catch (err) {
        statusEl.textContent = "❌ " + err.message;
      }
    }

    async function callMe() {
      const out = document.getElementById("me-output");
      if (!token) {
        out.textContent = "토큰이 없습니다. 먼저 로그인하세요.";
        return;
      }
      out.textContent = "요청 중...";
      try {
        const res = await fetch("/v1/auth/me", {
          headers: {"Authorization": "Bearer " + token},
        });
        const data = await res.json();
        out.textContent = JSON.stringify(data, null, 2);
      } catch (err) {
        out.textContent = err.message;
      }
    }

    async function logout() {
      const out = document.getElementById("me-output");
      if (!token) {
        out.textContent = "이미 토큰이 없습니다.";
        return;
      }
      try {
        const res = await fetch("/v1/auth/logout", {
          method: "POST",
          headers: {"Authorization": "Bearer " + token},
        });
        const data = await res.json();
        out.textContent = JSON.stringify(data, null, 2);
        if (res.ok) {
          token = null;
          document.getElementById("token-display").textContent = "없음";
        }
      } catch (err) {
        out.textContent = err.message;
      }
    }
  </script>
</body>
</html>
"""


@router.get("/demo", response_class=HTMLResponse)
def auth_demo_page():
    """Simple HTML playground for trying the auth APIs."""
    return HTML_TEMPLATE


__all__ = ["router"]
