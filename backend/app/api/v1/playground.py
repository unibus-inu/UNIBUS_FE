"""Simple HTML playground for visualizing stops on a Leaflet map."""
from fastapi import APIRouter
from fastapi.responses import HTMLResponse

router = APIRouter()

HTML_TEMPLATE = """<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8" />
  <title>INU Shuttle Map Playground</title>
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <link rel="stylesheet" href="/static/leaflet/leaflet.css" />
  <style>
    body,html {margin:0; padding:0; height:100%;}
    #map {width:100%; height:100%;}
    #status {
      position:absolute; top:10px; left:10px;
      background:rgba(255,255,255,0.9); padding:6px 10px;
      border-radius:4px; font-family:sans-serif; font-size:14px;
      box-shadow:0 2px 6px rgba(0,0,0,0.15);
      z-index:1000;
    }
  </style>
</head>
<body>
  <div id="status">불러오는 중…</div>
  <div id="map"></div>
  <script src="/static/leaflet/leaflet.js"></script>
  <script>
    const statusEl = document.getElementById("status");
    function failEarly(message) {
      statusEl.textContent = message;
      throw new Error(message);
    }

    function ensureLeaflet() {
      if (!window.L) {
        failEarly("Leaflet 스크립트를 불러오지 못했습니다. (사내망/인터넷 확인)");
        return false;
      }
      return true;
    }

    if (!ensureLeaflet()) {
      // Leaflet이 없으면 이후 코드를 실행하지 않음
      // failEarly가 throw 해서 아래 코드는 도달하지 않지만 안전망 유지
    }

    const params = new URLSearchParams(window.location.search);
    const routeId = params.get("route_id") || "inu-a"; // ?route_id=노선ID 로 교체 가능
    const map = L.map("map");
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 19,
      attribution: "&copy; OpenStreetMap"
    }).addTo(map);

    async function loadRoute() {
      const res = await fetch(`/v1/route/${encodeURIComponent(routeId)}`);
      if (!res.ok) throw new Error("route fetch failed");
      return res.json();
    }

    async function loadStops() {
      const res = await fetch(`/api/stops?route_id=${routeId}`);
      if (!res.ok) throw new Error("stops fetch failed");
      return res.json();
    }

    function drawPolyline(polylineCoords) {
      if (!Array.isArray(polylineCoords) || polylineCoords.length < 2) return null;
      const latlngs = polylineCoords.map(([lat, lon]) => [lat, lon]);
      return L.polyline(latlngs, {color: "#0D6EFD", weight: 4}).addTo(map);
    }

    function drawStops(stops) {
      return stops.map((s) =>
        L.circleMarker([s.lat, s.lon], {
          radius: 6,
          color: "#DC3545",
          weight: 2,
          fillColor: "#fff",
          fillOpacity: 0.9,
        })
          .addTo(map)
          .bindPopup(`<strong>${s.name}</strong><br/>#${s.seq} (${s.id})`)
      );
    }

    async function bootstrap() {
      try {
        const [route, stops] = await Promise.all([loadRoute(), loadStops()]);
        const line = drawPolyline(route.polyline);
        drawStops(stops);
        if (line) {
          map.fitBounds(line.getBounds().pad(0.1));
        } else if (stops.length) {
          const latlngs = stops.map((s) => [s.lat, s.lon]);
          map.fitBounds(latlngs, {padding: [20, 20]});
        } else {
          map.setView([37.3868, 126.6675], 15); // fallback center
        }
        statusEl.textContent = "✅ 정류장/노선 로딩 완료";
      } catch (err) {
        console.error(err);
        statusEl.textContent = "불러오기 실패: " + err.message;
      }
    }

    bootstrap();
  </script>
</body>
</html>
"""

@router.get("/playground/map", response_class=HTMLResponse)
def playground_map():
    """Serve a quick Leaflet-based playground page."""
    return HTML_TEMPLATE

__all__ = ["router"]
