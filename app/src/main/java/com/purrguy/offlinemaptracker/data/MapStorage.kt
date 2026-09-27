package com.purrguy.offlinemaptracker.data

import android.content.Context
import java.io.File

object MapStorage {
    fun mapsDir(c: Context): File = File(c.getExternalFilesDir(null), "maps").apply { mkdirs() }
    fun mapFile(c: Context, id: String): File = File(mapsDir(c), "$id.map")
    fun pbfFile(c: Context, id: String): File = File(mapsDir(c), "$id.osm.pbf")
    fun graphDir(c: Context, id: String): File = File(mapsDir(c), "$id-gh").apply { mkdirs() }
    fun tilesDir(c: Context): File = File(c.getExternalFilesDir(null), "tiles").apply { mkdirs() }

    fun isMapDownloaded(c: Context, id: String): Boolean {
        val f = mapFile(c, id)
        return f.exists() && f.length() > 1_000_000
    }

    fun storageSummary(c: Context): String {
        val dir = mapsDir(c)
        var total = 0L
        dir.listFiles()?.forEach { total += it.length() }
        val mb = total / 1024.0 / 1024.0
        val count = dir.listFiles()?.count { it.name.endsWith(".map") } ?: 0
        return "Maps on device: $count • %.1f MB".format(mb)
    }
}
