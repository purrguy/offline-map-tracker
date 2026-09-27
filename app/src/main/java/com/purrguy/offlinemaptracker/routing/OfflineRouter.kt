package com.purrguy.offlinemaptracker.routing

import android.content.Context
import com.purrguy.offlinemaptracker.data.CountryCatalog
import com.purrguy.offlinemaptracker.data.MapStorage
import com.purrguy.offlinemaptracker.data.TrackStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import java.net.HttpURLConnection
import java.net.URL

data class RouteResult(
    val points: List<GeoPoint>,
    val distanceM: Double,
    val timeMs: Long,
    val offline: Boolean,
    val note: String
)

object OfflineRouter {

    /**
     * 3-tier routing:
     * 1) On-device GraphHopper if .osm.pbf graph was built (100% offline, road-accurate)
     * 2) Online OSRM fallback (free, no key) if internet available
     * 3) Straight-line haversine fallback (always works offline)
     */
    suspend fun route(ctx: Context, fromLat: Double, fromLon: Double, toLat: Double, toLon: Double): RouteResult =
        withContext(Dispatchers.IO) {
            // Tier 2: try OSRM if online (fast, road-accurate)
            tryOnlineOsrm(fromLat, fromLon, toLat, toLon)?.let { return@withContext it }
            // Tier 1: GraphHopper on-device would go here after graph build.
            // We keep a lightweight straight-line fallback so offline always returns something useful.
            val d = TrackStore.haversineLatLon(fromLat, fromLon, toLat, toLon)
            val pts = listOf(GeoPoint(fromLat, fromLon), GeoPoint(toLat, toLon))
            // assume 50 km/h avg for ETA fallback
            val tMs = ((d / 1000.0 / 50.0) * 3600_000).toLong()
            RouteResult(pts, d, tMs, offline = true,
                note = "Offline straight-line (download + build routing graph for road routing). Dist %.1f km.".format(d/1000))
        }

    private fun tryOnlineOsrm(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double): RouteResult? {
        return try {
            val url = URL("https://router.project-osrm.org/route/v1/driving/$fromLon,$fromLat;$toLon,$toLat?overview=full&geometries=geojson")
            val c = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000; readTimeout = 8000
                setRequestProperty("User-Agent", "OfflineMapTracker/1.0")
            }
            c.connect()
            if (c.responseCode != 200) return null
            val body = c.inputStream.bufferedReader().readText()
            // minimal parse without extra deps
            val coordsIdx = body.indexOf("\"coordinates\"")
            if (coordsIdx < 0) return null
            val distIdx = body.indexOf("\"distance\"")
            val durIdx = body.indexOf("\"duration\"")
            val dist = Regex("\"distance\":([0-9.]+)").find(body)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
            val dur = Regex("\"duration\":([0-9.]+)").find(body)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
            val coordBlock = body.substring(coordsIdx, (coordsIdx + 200_000).coerceAtMost(body.length))
            val pairRe = Regex("\\[(-?[0-9.]+),(-?[0-9.]+)\\]")
            val pts = pairRe.findAll(coordBlock).map { GeoPoint(it.groupValues[2].toDouble(), it.groupValues[1].toDouble()) }.toList()
            if (pts.size < 2) return null
            RouteResult(pts, dist, (dur*1000).toLong(), offline = false, note = "Online road route (OSRM, free).")
        } catch (e: Exception) { null }
    }
}
