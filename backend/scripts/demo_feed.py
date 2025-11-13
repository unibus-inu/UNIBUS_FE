# scripts/demo_feed.py
import os, time, json, hmac, hashlib, requests

URL = os.getenv("SERVER_URL", "http://127.0.0.1:8000/v1/ingest")
VEHICLE_ID = os.getenv("VEHICLE_ID", "bus-01")
SECRET = os.getenv("DEVICE_SECRET", "DEV_DEFAULT_SECRET")

# backend/scripts/demo_feed.py 중 path 수정
path = [
    (37.385524,126.638650),  # 인천대입구역
    (37.377730,126.635094),  # 인천대정문
    (37.37418, 126.6344),  # 인천대공과대
    (37.374114,126.630295),  # 인천대생활원
]

def sig(body: bytes) -> str:
    return hmac.new(SECRET.encode(), body, hashlib.sha256).hexdigest()

for i, (lat, lon) in enumerate(path * 3):  # 15포인트
    payload = {
        "vehicle_id": VEHICLE_ID,
        "ts": int(time.time()),
        "lat": lat,
        "lon": lon,
        "speed_mps": 5.5,
        "heading": 90.0
    }
    body = json.dumps(payload, separators=(",", ":")).encode()
    r = requests.post(URL, data=body, headers={
        "Content-Type": "application/json",
        "X-Device-Signature": sig(body)
    })
    print(i, r.status_code, r.text)
    time.sleep(1)
