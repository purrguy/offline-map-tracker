# Offline Map Tracker — Android, 100% free, offline-first

Phone app (Android) that runs **offline after a one-time map download**:

- **Maps tab**: select your country (**Latvia is default**). Big countries (Russia, USA…) show **regions** so you download only what you need. Downloads the whole country's vector map with **street names** in one file (~55 MB for Latvia). Space-saving: single compressed `.map` + LRU tile cache, no params/API keys.
- **Map tab**: view freely, ◎ Location, offline toggle, long-press to set destination.
- **Speed tab**: **Track speed** (current GPS km/h + mph), **timer for medium/average speed**, max speed. Works offline.
- **Tracks tab**: **track tracking even offline** — Start track → records GPS in background → Stop+Save → makes a **from-where-to-where map**, view freely with time / distance / avg speed. Exports GPX + JSON.
- **Route tab**: like Google Maps — enter destination or use map tap → **optimal way to get there**. Offline straight-line + online OSRM road route when internet available, on-device GraphHopper hook ready.

No accounts, no keys, no payments. Map data © OpenStreetMap contributors.

## Download (APK)

1. Open **Actions** tab → latest run → download `offline-map-tracker-apk`
2. Or open **Releases** (tagged `v*`) → download `.apk`
3. On phone: allow *Install unknown apps* → install APK.

Build locally:
```bash
gradle :app:assembleDebug
# APK at app/build/outputs/apk/debug/
```

## One-time offline setup (do this on Wi-Fi)

1. Open app → grant Location → go to **Maps** tab
2. Download **Latvia ⭐** (or your region)
3. Go to **Map** tab → toggle *Offline: ON* → kill internet → map still works.

Storage: `Android/data/com.purrguy.offlinemaptracker/files/maps/` (`latvia.map`, tracks in `.../tracks/*.gpx`).

## Sources (all free)

- Render: `download.mapsforge.org` vector `.map` (street names included)
- Routing data: `download.geofabrik.de` `.osm.pbf`
- Tiles fallback: OSMDroid cache (Mapnik, ODbL attribution on map)
- Routing fallback: OSRM public demo server (online only, optional)

## Stack

- Kotlin, osmdroid 6.1.20, mapsforge 0.19.0, graphhopper-core 7.0, play-services-location
- minSdk 26, targetSdk 34, AGP 8.5.2, Java 17 (CI builds APK, no local SDK needed)

## License

MIT — free for self use. See LICENSE.
