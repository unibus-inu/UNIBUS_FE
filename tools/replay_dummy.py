import time, json, hmac, hashlib, requests, os, math

URL = os.getenv("URL", "http://localhost:8000/v1/ingest")  # 게이트웨이 쓰면 거기로
SEC = os.getenv("SEC", "your-shared-secret")
VID = os.getenv("VID", "bus-001")

path = [
  (37.375120, 126.632910),
  (37.376200, 126.634000),
  (37.377450, 126.635300),
  (37.378900, 126.636700),
  (37.380200, 126.637900),
]

def hmac_hex(sec, body): return hmac.new(sec.encode(), body, hashlib.sha256).hexdigest()
def heading(a,b):
    (lat1,lon1),(lat2,lon2)=a,b
    y=math.sin(math.radians(lon2-lon1))*math.cos(math.radians(lat2))
    x=math.cos(math.radians(lat1))*math.sin(math.radians(lat2)) - \
      math.sin(math.radians(lat1))*math.cos(math.radians(lat2))*math.cos(math.radians(lon2-lon1))
    return (math.degrees(math.atan2(y,x))+360)%360

for i in range(len(path)):
    lat, lon = path[i]
    hdg = heading(path[i-1], path[i]) if i>0 else 0.0
    payload = {
        "vehicle_id": VID,
        "ts": int(time.time()),
        "lat": lat,
        "lon": lon,
        "speed_mps": 8.0,
        "heading": hdg
    }
    body = json.dumps(payload, separators=(',',':')).encode()
    sig  = hmac_hex(SEC, body)
    r = requests.post(URL, data=body, headers={
        "Content-Type":"application/json",
        "X-Device-Signature": sig
    }, timeout=5)
    print(i, r.status_code, r.text)
    time.sleep(1.5)
