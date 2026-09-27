package com.purrguy.offlinemaptracker.ui

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.purrguy.offlinemaptracker.R
import com.purrguy.offlinemaptracker.data.TrackStore
import com.purrguy.offlinemaptracker.tracking.TrackService
import java.io.File

class TracksFragment : Fragment() {
    private lateinit var tvStatus: TextView
    private lateinit var tvStats: TextView
    private lateinit var rv: RecyclerView
    var onViewOnMap: (() -> Unit)? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            val d = i?.getDoubleExtra(TrackService.EXTRA_DIST, 0.0) ?: 0.0
            val n = i?.getIntExtra(TrackService.EXTRA_COUNT, 0) ?: 0
            if (TrackService.recording) {
                tvStatus.text = "● Recording… $n pts"
                tvStats.text = "Distance: %.0f m".format(d)
            }
        }
    }

    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, b: Bundle?): View {
        val v = inf.inflate(R.layout.fragment_tracks, c, false)
        tvStatus = v.findViewById(R.id.tv_track_status)
        tvStats = v.findViewById(R.id.tv_track_stats)
        rv = v.findViewById(R.id.rv_tracks)
        rv.layoutManager = LinearLayoutManager(requireContext())

        v.findViewById<Button>(R.id.btn_start_track).setOnClickListener {
            if (!hasPerm()) { reqPerm(); return@setOnClickListener }
            val i = Intent(requireContext(), TrackService::class.java).setAction(TrackService.ACTION_START)
            requireContext().startForegroundService(i)
            tvStatus.text = "● Recording…"
            Toast.makeText(requireContext(), "Recording (works offline)", Toast.LENGTH_SHORT).show()
        }
        v.findViewById<Button>(R.id.btn_stop_track).setOnClickListener {
            requireContext().startService(Intent(requireContext(), TrackService::class.java).setAction(TrackService.ACTION_STOP))
            tvStatus.text = "Not recording"
            refreshList()
            Toast.makeText(requireContext(), "Track saved (GPX + JSON)", Toast.LENGTH_SHORT).show()
        }
        refreshList()
        return v
    }

    override fun onResume() {
        super.onResume()
        requireContext().registerReceiver(receiver, IntentFilter(TrackService.BROADCAST))
        refreshList()
        tvStatus.text = if (TrackService.recording) "● Recording…" else "Not recording"
    }

    override fun onPause() {
        super.onPause()
        try { requireContext().unregisterReceiver(receiver) } catch (_: Exception) {}
    }

    private fun refreshList() {
        if (!isAdded) return
        val items = TrackStore.list(requireContext())
        rv.adapter = object : RecyclerView.Adapter<VH>() {
            override fun onCreateViewHolder(p: ViewGroup, t: Int): VH {
                val vv = layoutInflater.inflate(R.layout.item_track, p, false)
                return VH(vv)
            }
            override fun getItemCount() = items.size
            override fun onBindViewHolder(h: VH, pos: Int) {
                val (file, meta) = items[pos]
                h.name.text = file.nameWithoutExtension
                h.meta.text = if (meta != null)
                    "%.2f km • %s • %d pts • avg %.1f km/h".format(meta.distanceM/1000,
                        TrackStore.formatDuration(meta.endTime - meta.startTime), meta.points, meta.avgKmh)
                else "tap to view"
                h.viewBtn.setOnClickListener {
                    val pts = TrackStore.loadPoints(requireContext(), file)
                    MapState.trackToShow = TrackStore.toGeoPoints(pts)
                    MapState.routeToShow = null
                    Toast.makeText(requireContext(), "Showing ${pts.size} pts on Map tab", Toast.LENGTH_SHORT).show()
                    onViewOnMap?.invoke()
                }
            }
        }
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tv_track_name)
        val meta: TextView = v.findViewById(R.id.tv_track_meta)
        val viewBtn: Button = v.findViewById(R.id.btn_view)
    }

    private fun hasPerm() = ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    private fun reqPerm() { requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 2) }
}
