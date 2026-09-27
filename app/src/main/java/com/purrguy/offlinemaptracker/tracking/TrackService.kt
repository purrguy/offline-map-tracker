package com.purrguy.offlinemaptracker.tracking

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.location.Location
import android.os.*
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.purrguy.offlinemaptracker.R
import com.purrguy.offlinemaptracker.data.TrackPoint
import com.purrguy.offlinemaptracker.data.TrackStore

/**
 * Foreground GPS service: works offline, keeps recording even with screen off.
 * Broadcasts updates so Speed + Tracks + Map fragments stay live.
 */
class TrackService : Service() {

    companion object {
        const val ACTION_START = "start_track"
        const val ACTION_STOP = "stop_track"
        const val BROADCAST = "com.purrguy.offlinemaptracker.LOC"
        const val EXTRA_LAT = "lat"; const val EXTRA_LON = "lon"
        const val EXTRA_SPEED = "speed"; const val EXTRA_DIST = "dist"
        const val EXTRA_COUNT = "count"
        var recording = false
        var points: MutableList<TrackPoint> = mutableListOf()
        var distanceM: Double = 0.0
    }

    private lateinit var fused: FusedLocationProviderClient
    private var last: TrackPoint? = null

    private val callback = object : LocationCallback() {
        override fun onLocationResult(r: LocationResult) {
            for (loc in r.locations) onLoc(loc)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        fused = LocationServices.getFusedLocationProviderClient(this)
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                recording = true; points = mutableListOf(); distanceM = 0.0; last = null
                startFg()
                fused.requestLocationUpdates(req(), callback, Looper.getMainLooper())
            }
            ACTION_STOP -> {
                recording = false
                try { fused.removeLocationUpdates(callback) } catch (_: Exception) {}
                if (points.size >= 2) TrackStore.save(this, points.toList())
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                // passive speed updates (no save) - still need FG for background
                startFg()
                fused.requestLocationUpdates(req(), callback, Looper.getMainLooper())
            }
        }
        return START_STICKY
    }

    private fun req(): LocationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
        .setMinUpdateDistanceMeters(3f).build()

    private fun startFg() {
        val ch = "track"
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(ch, "Tracking", NotificationManager.IMPORTANCE_LOW))
        val n = NotificationCompat.Builder(this, ch)
            .setContentTitle("Offline Map Tracker — GPS active")
            .setContentText(if (recording) "Recording track (offline)" else "GPS speed (offline)")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
        startForeground(1, n)
    }

    private fun onLoc(loc: Location) {
        val tp = TrackPoint(loc.latitude, loc.longitude, System.currentTimeMillis(),
            if (loc.hasSpeed()) loc.speed else 0f)
        last?.let { distanceM += TrackStore.haversine(it, tp) }
        last = tp
        if (recording) points.add(tp)
        val i = Intent(BROADCAST).apply {
            putExtra(EXTRA_LAT, tp.lat); putExtra(EXTRA_LON, tp.lon)
            putExtra(EXTRA_SPEED, tp.speedMps); putExtra(EXTRA_DIST, distanceM)
            putExtra(EXTRA_COUNT, points.size)
        }
        sendBroadcast(i)
    }
}
