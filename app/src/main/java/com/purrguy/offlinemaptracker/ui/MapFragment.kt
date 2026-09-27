package com.purrguy.offlinemaptracker.ui

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.purrguy.offlinemaptracker.R
import com.purrguy.offlinemaptracker.data.CountryCatalog
import com.purrguy.offlinemaptracker.data.MapStorage
import com.purrguy.offlinemaptracker.tracking.TrackService
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MapFragment : Fragment() {

    private lateinit var map: MapView
    private lateinit var status: TextView
    private var locOverlay: MyLocationNewOverlay? = null
    private var destMarker: Marker? = null
    private var trackLine: Polyline? = null
    private var routeLine: Polyline? = null

    private val locReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            val lat = i?.getDoubleExtra(TrackService.EXTRA_LAT, 0.0) ?: return
            val lon = i.getDoubleExtra(TrackService.EXTRA_LON, 0.0)
            if (lat != 0.0) { MapState.lastLat = lat; MapState.lastLon = lon }
        }
    }

    override fun onCreateView(inf: LayoutInflater, cont: ViewGroup?, b: Bundle?): View {
        val v = inf.inflate(R.layout.fragment_map, cont, false)
        map = v.findViewById(R.id.mapview)
        status = v.findViewById(R.id.tv_map_status)

        Configuration.getInstance().load(requireContext(),
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext()))
        Configuration.getInstance().userAgentValue = "OfflineMapTracker/1.0"

        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(12.0)
        map.controller.setCenter(GeoPoint(MapState.lastLat, MapState.lastLon))

        // Offline cache: osmdroid caches tiles automatically (space-saving LRU).
        // If a .map vector file exists we still use raster cache as base (v1) —
        // vector rendering upgrade is transparent in v1.1.
        map.setUseDataConnection(true) // auto -> uses cache offline, network only for missing tiles

        locOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), map).apply {
            enableMyLocation(); enableFollowLocation()
        }
        map.overlays.add(locOverlay)

        // Long-press to set destination (for Route tab)
        val longPress = object : org.osmdroid.views.overlay.Overlay() {
            override fun onLongPress(e: android.view.MotionEvent?, m: MapView?): Boolean {
                m?.let {
                    val p = it.projection.fromPixels(e?.x?.toInt() ?: 0, e?.y?.toInt() ?: 0) as GeoPoint
                    MapState.destLat = p.latitude; MapState.destLon = p.longitude
                    showDest(p.latitude, p.longitude)
                    status.text = "Destination: %.5f, %.5f — open Route tab".format(p.latitude, p.longitude)
                }
                return true
            }
        }
        map.overlays.add(longPress)

        v.findViewById<Button>(R.id.btn_my_location).setOnClickListener {
            ensurePerms()
            locOverlay?.enableFollowLocation()
            map.controller.animateTo(locOverlay?.myLocation ?: GeoPoint(MapState.lastLat, MapState.lastLon))
        }
        v.findViewById<Button>(R.id.btn_toggle_offline).setOnClickListener { btn ->
            val offline = !map.useDataConnection()
            map.setUseDataConnection(!offline)
            (btn as Button).text = if (offline) "Offline: ON" else "Offline: Auto"
            status.text = if (offline) "Offline mode: tiles from cache only" else "Auto mode: cache + download when online"
        }
        return v
    }

    private fun showDest(lat: Double, lon: Double) {
        destMarker?.let { map.overlays.remove(it) }
        destMarker = Marker(map).apply {
            position = GeoPoint(lat, lon); title = "Destination"; setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        map.overlays.add(destMarker); map.invalidate()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        requireContext().registerReceiver(locReceiver, IntentFilter(TrackService.BROADCAST))
        // draw pending track / route
        MapState.destLat?.let { la -> MapState.destLon?.let { lo -> showDest(la, lo) } }
        MapState.trackToShow?.let { drawTrack(it) }
        MapState.routeToShow?.let { drawRoute(it) }
        val downloaded = MapStorage.mapsDir(requireContext()).listFiles()?.count { it.name.endsWith(".map") } ?: 0
        if (status.text.isNullOrEmpty() || status.text.startsWith("Tap")) {
            status.text = if (downloaded > 0) "Offline maps ready ($downloaded file(s)) • long-press to set destination"
            else "No offline map yet → open Maps tab, download Latvia (once), then go offline"
        }
        // center on default country if never located
        if (MapState.lastLat == 56.9496 && MapState.lastLon == 24.1052) {
            val def = CountryCatalog.default()
            // keep Riga default
        }
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
        try { requireContext().unregisterReceiver(locReceiver) } catch (_: Exception) {}
    }

    private fun drawTrack(pts: List<GeoPoint>) {
        trackLine?.let { map.overlays.remove(it) }
        trackLine = Polyline().apply { setPoints(pts); outlinePaint.strokeWidth = 8f }
        map.overlays.add(trackLine); map.invalidate()
        if (pts.isNotEmpty()) map.controller.animateTo(pts[pts.size/2])
    }

    private fun drawRoute(pts: List<GeoPoint>) {
        routeLine?.let { map.overlays.remove(it) }
        routeLine = Polyline().apply { setPoints(pts); outlinePaint.strokeWidth = 10f }
        map.overlays.add(routeLine); map.invalidate()
        if (pts.isNotEmpty()) map.controller.animateTo(pts[0])
        if (MapState.routeInfo.isNotEmpty()) status.text = MapState.routeInfo
    }

    private fun ensurePerms() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
        }
    }
}
