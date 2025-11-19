import hashlib
import hmac
import secrets

from app.core.config import settings

DEFAULT_ALGO = "pbkdf2_sha256"


def _iterations() -> int:
    val = getattr(settings, "PASSWORD_HASH_ITERATIONS", 320_000)
    try:
        return max(100_000, int(val))
    except Exception:
        return 320_000


def hash_password(password: str) -> str:
    if not isinstance(password, str) or not password:
        raise ValueError("password must be a non-empty string")
    salt = secrets.token_bytes(16)
    iterations = _iterations()
    dk = hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), salt, iterations)
    return f"{DEFAULT_ALGO}${iterations}${salt.hex()}${dk.hex()}"


def verify_password(password: str, hashed: str) -> bool:
    try:
        algo, iter_str, salt_hex, hash_hex = hashed.split("$", 3)
        iterations = int(iter_str)
    except (ValueError, AttributeError):
        return False
    if algo != DEFAULT_ALGO:
        return False
    try:
        salt = bytes.fromhex(salt_hex)
        expected = bytes.fromhex(hash_hex)
    except ValueError:
        return False
    test = hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), salt, iterations)
    return hmac.compare_digest(expected, test)


def generate_token(length: int = 32) -> str:
    return secrets.token_urlsafe(length)
