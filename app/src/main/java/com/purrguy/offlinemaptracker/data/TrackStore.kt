package com.purrguy.offlinemaptracker.data

import android.content.Context
import android.location.Location
import com.google.gson.Gson
import org.osmdroid.util.GeoPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class TrackPoint(val lat: Double, val lon: Double, val time: Long, val speedMps: Float = 0f)
data class TrackMeta(
    val id: String,
    val startTime: Long,
    val endTime: Long,
    val distanceM: Double,
    val points: Int,
    val avgKmh: Double
)

object TrackStore {
    private val gson = Gson()
    private fun dir(c: Context) = File(c.getExternalFilesDir(null), "tracks").apply { mkdirs() }

    fun save(c: Context, points: List<TrackPoint>): File {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val id = "track_" + sdf.format(Date())
        val f = File(dir(c), "$id.json")
        f.writeText(gson.toJson(points))
        // also export GPX (view freely in any GIS app)
        val gpx = File(dir(c), "$id.gpx")
        gpx.writeText(toGpx(points))
        return f
    }

    fun list(c: Context): List<Pair<File, TrackMeta?>> {
        val files = dir(c).listFiles { f -> f.extension == "json" }?.sortedByDescending { it.name } ?: emptyList()
        return files.map { f ->
            try {
                val arr = gson.fromJson(f.readText(), Array<TrackPoint>::class.java).toList()
                f to metaFromPoints(f.nameWithoutExtension, arr)
            } catch (e: Exception) { f to null }
        }
    }

    fun loadPoints(c: Context, file: File): List<TrackPoint> {
        return try {
            gson.fromJson(file.readText(), Array<TrackPoint>::class.java).toList()
        } catch (e: Exception) { emptyList() }
    }

    private fun metaFromPoints(id: String, pts: List<TrackPoint>): TrackMeta? {
        if (pts.size < 2) return TrackMeta(id, pts.firstOrNull()?.time ?: 0, pts.lastOrNull()?.time ?: 0, 0.0, pts.size, 0.0)
        var d = 0.0
        for (i in 1 until pts.size) d += haversine(pts[i-1], pts[i])
        val dtH = ((pts.last().time - pts.first().time) / 3600000.0).coerceAtLeast(1e-6)
        return TrackMeta(id, pts.first().time, pts.last().time, d, pts.size, (d/1000.0)/dtH)
    }

    fun haversine(a: TrackPoint, b: TrackPoint): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val s = Math.sin(dLat/2)*Math.sin(dLat/2) +
                Math.cos(Math.toRadians(a.lat))*Math.cos(Math.toRadians(b.lat))*
                Math.sin(dLon/2)*Math.sin(dLon/2)
        return 2 * R * Math.asin(Math.sqrt(s))
    }

    fun haversineLatLon(aLat: Double, aLon: Double, bLat: Double, bLon: Double): Double =
        haversine(TrackPoint(aLat, aLon, 0), TrackPoint(bLat, bLon, 0))

    private fun toGpx(pts: List<TrackPoint>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\"?><gpx version=\"1.1\" creator=\"OfflineMapTracker\"><trk><trkseg>")
        for (p in pts) sb.append("<trkpt lat=\"${p.lat}\" lon=\"${p.lon}\"><time>${p.time}</time></trkpt>")
        sb.append("</trkseg></trk></gpx>")
        return sb.toString()
    }

    fun formatDuration(ms: Long): String {
        val s = (ms / 1000).toInt()
        return "%02d:%02d:%02d".format(s/3600, (s%3600)/60, s%60)
    }

    fun toGeoPoints(pts: List<TrackPoint>): List<GeoPoint> = pts.map { GeoPoint(it.lat, it.lon) }
}
