package com.purrguy.offlinemaptracker.ui

import android.content.*
import android.os.Bundle
import android.os.SystemClock
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.purrguy.offlinemaptracker.R
import com.purrguy.offlinemaptracker.tracking.TrackService

class SpeedFragment : Fragment() {
    private lateinit var tvSpeed: TextView
    private lateinit var tvMph: TextView
    private lateinit var tvAvg: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvMax: TextView

    private var timerRunning = false
    private var timerBase = 0L
    private var timerAccum = 0L
    private var distAccum = 0.0
    private var lastDist = 0.0
    private var maxKmh = 0.0

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            val speedMps = i?.getFloatExtra(TrackService.EXTRA_SPEED, 0f) ?: 0f
            val dist = i?.getDoubleExtra(TrackService.EXTRA_DIST, 0.0) ?: 0.0
            val kmh = speedMps * 3.6
            tvSpeed.text = "%.1f km/h".format(kmh)
            tvMph.text = "%.1f mph".format(kmh * 0.621371)
            if (kmh > maxKmh) { maxKmh = kmh; tvMax.text = "Max: %.1f km/h".format(maxKmh) }
            if (timerRunning) {
                distAccum += (dist - lastDist).coerceAtLeast(0.0)
                val elapsedMs = timerAccum + (SystemClock.elapsedRealtime() - timerBase)
                tvTimer.text = formatHms(elapsedMs)
                val hours = elapsedMs / 3600000.0
                val avg = if (hours > 0.0001) (distAccum / 1000.0) / hours else 0.0
                tvAvg.text = "Avg: %.1f km/h".format(avg)
            }
            lastDist = dist
        }
    }

    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, b: Bundle?): View {
        val v = inf.inflate(R.layout.fragment_speed, c, false)
        tvSpeed = v.findViewById(R.id.tv_speed)
        tvMph = v.findViewById(R.id.tv_speed_mph)
        tvAvg = v.findViewById(R.id.tv_avg)
        tvTimer = v.findViewById(R.id.tv_timer)
        tvMax = v.findViewById(R.id.tv_max)
        v.findViewById<Button>(R.id.btn_start_timer).setOnClickListener {
            if (!timerRunning) { timerRunning = true; timerBase = SystemClock.elapsedRealtime(); distAccum = 0.0 }
            // ensure GPS running
            requireContext().startForegroundService(Intent(requireContext(), TrackService::class.java))
        }
        v.findViewById<Button>(R.id.btn_stop_timer).setOnClickListener {
            if (timerRunning) { timerAccum += SystemClock.elapsedRealtime() - timerBase; timerRunning = false }
        }
        v.findViewById<Button>(R.id.btn_reset_timer).setOnClickListener {
            timerRunning = false; timerAccum = 0; distAccum = 0.0; maxKmh = 0.0
            tvTimer.text = "00:00:00"; tvAvg.text = "Avg: -- km/h"; tvMax.text = "Max: 0.0 km/h"
        }
        return v
    }

    override fun onResume() {
        super.onResume()
        requireContext().registerReceiver(receiver, IntentFilter(TrackService.BROADCAST))
        requireContext().startForegroundService(Intent(requireContext(), TrackService::class.java))
    }

    override fun onPause() {
        super.onPause()
        try { requireContext().unregisterReceiver(receiver) } catch (_: Exception) {}
        if (timerRunning) timerAccum += SystemClock.elapsedRealtime() - timerBase.also { timerBase = SystemClock.elapsedRealtime() }
    }

    private fun formatHms(ms: Long): String {
        val s = (ms/1000).toInt()
        return "%02d:%02d:%02d".format(s/3600, (s%3600)/60, s%60)
    }
}
