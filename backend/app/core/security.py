import hmac, hashlib

def hmac_hex(secret: str, body: bytes) -> str:
    return hmac.new(secret.encode(), body, hashlib.sha256).hexdigest()

def verify_hmac(secret: str, body: bytes, sig_hex: str) -> bool:
    return hmac.compare_digest(hmac_hex(secret, body), sig_hex or "")