# scripts/demo_feed.py
import os, time, json, hmac, hashlib, requests

URL = os.getenv("SERVER_URL", "http://127.0.0.1:8000/v1/ingest")
VEHICLE_ID = os.getenv("VEHICLE_ID", "bus-01")
SECRET = os.getenv("DEVICE_SECRET", "DEV_DEFAULT_SECRET")

path = [
    (37.3752, 126.6333),  # 정문
    (37.3758, 126.6342),
    (37.3766, 126.6358),  # 7호관
    (37.3771, 126.6362),
    (37.3773, 126.6365),  # 기숙사
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