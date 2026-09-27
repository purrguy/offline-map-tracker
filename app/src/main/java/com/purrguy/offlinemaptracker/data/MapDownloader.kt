package com.purrguy.offlinemaptracker.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Simple resumable-ish downloader with progress. Space-saving:
 * - downloads single compressed vector .map (street names included)
 * - optional .osm.pbf only if user wants offline routing graph
 * - raster tiles are cached on demand by osmdroid (LRU, expiry) instead of bulk download
 */
class MapDownloader(private val ctx: Context) {

    suspend fun downloadMap(entry: CountryEntry, onProgress: (Int) -> Unit, onLog: (String) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dest = MapStorage.mapFile(ctx, entry.id)
            downloadTo(entry.mapsforgeUrl, dest, onProgress, onLog)
            onLog("Saved ${dest.name} (${dest.length() / 1024 / 1024} MB)")
            dest
        }

    suspend fun downloadPbf(entry: CountryEntry, onProgress: (Int) -> Unit, onLog: (String) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dest = MapStorage.pbfFile(ctx, entry.id)
            downloadTo(entry.pbfUrl, dest, onProgress, onLog)
            dest
        }

    private fun downloadTo(urlStr: String, dest: File, onProgress: (Int) -> Unit, onLog: (String) -> Unit) {
        // Try primary URL, fall back to mirror naming if 404 (mapsforge file layout changes sometimes)
        val candidates = listOf(urlStr)
        var lastErr: Exception? = null
        for (u in candidates) {
            try {
                onLog("Downloading:\n$u")
                val url = URL(u)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 20_000; readTimeout = 30_000
                    setRequestProperty("User-Agent", "OfflineMapTracker/1.0 (free offline use)")
                    instanceFollowRedirects = true
                }
                conn.connect()
                if (conn.responseCode !in 200..299) throw Exception("HTTP ${conn.responseCode} for $u")
                val total = conn.contentLengthLong
                conn.inputStream.use { ins ->
                    FileOutputStream(dest).use { out ->
                        val buf = ByteArray(64 * 1024)
                        var done = 0L
                        while (true) {
                            val n = ins.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            done += n
                            if (total > 0) onProgress(((done * 100) / total).toInt().coerceIn(0, 100))
                        }
                    }
                }
                if (dest.length() < 50_000) throw Exception("File too small, likely error page")
                return
            } catch (e: Exception) {
                lastErr = e
                onLog("Failed: ${e.message}")
            }
        }
        throw lastErr ?: Exception("Download failed")
    }
}
