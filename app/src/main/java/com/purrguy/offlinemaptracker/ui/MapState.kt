package com.purrguy.offlinemaptracker.ui

import org.osmdroid.util.GeoPoint

object MapState {
    var destLat: Double? = null
    var destLon: Double? = null
    var destLabel: String = ""
    var trackToShow: List<GeoPoint>? = null
    var routeToShow: List<GeoPoint>? = null
    var routeInfo: String = ""
    var lastLat: Double = 56.9496  // Riga default (Latvia)
    var lastLon: Double = 24.1052
}
