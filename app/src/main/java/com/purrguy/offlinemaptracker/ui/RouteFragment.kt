package com.purrguy.offlinemaptracker.ui

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.purrguy.offlinemaptracker.R
import com.purrguy.offlinemaptracker.data.TrackStore
import com.purrguy.offlinemaptracker.routing.OfflineRouter
import kotlinx.coroutines.launch

class RouteFragment : Fragment() {
    var onRouted: (() -> Unit)? = null

    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, b: Bundle?): View {
        val v = inf.inflate(R.layout.fragment_route, c, false)
        val etLat: EditText = v.findViewById(R.id.et_dest_lat)
        val etLon: EditText = v.findViewById(R.id.et_dest_lon)
        val etName: EditText = v.findViewById(R.id.et_dest_name)
        val tvInfo: TextView = v.findViewById(R.id.tv_route_info)

        MapState.destLat?.let { etLat.setText(it.toString()) }
        MapState.destLon?.let { etLon.setText(it.toString()) }
        if (MapState.routeInfo.isNotEmpty()) tvInfo.text = MapState.routeInfo

        v.findViewById<Button>(R.id.btn_use_map_center).setOnClickListener {
            MapState.destLat?.let { etLat.setText(it.toString()) }
            MapState.destLon?.let { etLon.setText(it.toString()) }
            Toast.makeText(requireContext(), "Long-press Map tab to set destination", Toast.LENGTH_LONG).show()
        }
        v.findViewById<Button>(R.id.btn_route).setOnClickListener {
            val dLat = etLat.text.toString().toDoubleOrNull()
            val dLon = etLon.text.toString().toDoubleOrNull()
            if (dLat == null || dLon == null) { Toast.makeText(requireContext(), "Enter valid lat/lon", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            MapState.destLat = dLat; MapState.destLon = dLon; MapState.destLabel = etName.text.toString()
            tvInfo.text = "Routing…"
            lifecycleScope.launch {
                try {
                    val res = OfflineRouter.route(requireContext(), MapState.lastLat, MapState.lastLon, dLat, dLon)
                    MapState.routeToShow = res.points
                    MapState.trackToShow = null
                    val info = "%s\nDistance: %.2f km • ETA: %s • %d pts".format(
                        res.note, res.distanceM/1000, TrackStore.formatDuration(res.timeMs), res.points.size)
                    MapState.routeInfo = info
                    tvInfo.text = info
                    Toast.makeText(requireContext(), "Route ready — open Map tab", Toast.LENGTH_SHORT).show()
                    onRouted?.invoke()
                } catch (e: Exception) {
                    tvInfo.text = "Route failed: ${e.message}"
                }
            }
        }
        v.findViewById<Button>(R.id.btn_clear_route).setOnClickListener {
            MapState.routeToShow = null; MapState.routeInfo = ""; MapState.destLat = null; MapState.destLon = null
            tvInfo.text = "Cleared."
        }
        return v
    }
}
