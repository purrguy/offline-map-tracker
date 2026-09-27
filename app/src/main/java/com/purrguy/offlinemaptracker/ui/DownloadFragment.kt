package com.purrguy.offlinemaptracker.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.purrguy.offlinemaptracker.R
import com.purrguy.offlinemaptracker.data.*
import kotlinx.coroutines.*

class DownloadFragment : Fragment() {
    private lateinit var rv: RecyclerView
    private lateinit var tvStorage: TextView
    private var query = ""
    private var jobs = mutableMapOf<String, Job>()

    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, b: Bundle?): View {
        val v = inf.inflate(R.layout.fragment_download, c, false)
        rv = v.findViewById(R.id.rv_countries)
        tvStorage = v.findViewById(R.id.tv_storage)
        rv.layoutManager = LinearLayoutManager(requireContext())
        v.findViewById<EditText>(R.id.et_search).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { query = s.toString(); refresh() }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
        refresh()
        return v
    }

    override fun onResume() {
        super.onResume()
        tvStorage.text = MapStorage.storageSummary(requireContext())
    }

    private fun filtered(): List<CountryEntry> {
        val q = query.trim().lowercase()
        val all = CountryCatalog.entries
        if (q.isEmpty()) return all
        return all.filter { it.displayName.lowercase().contains(q) || it.id.contains(q) }
    }

    private fun refresh() {
        if (!isAdded) return
        tvStorage.text = MapStorage.storageSummary(requireContext())
        val items = filtered()
        rv.adapter = object : RecyclerView.Adapter<VH>() {
            override fun onCreateViewHolder(p: ViewGroup, t: Int): VH {
                val vv = layoutInflater.inflate(R.layout.item_country, p, false)
                return VH(vv)
            }
            override fun getItemCount() = items.size
            override fun onBindViewHolder(h: VH, pos: Int) {
                val e = items[pos]
                val downloaded = MapStorage.isMapDownloaded(requireContext(), e.id)
                h.name.text = e.displayName + if (downloaded) "  ✓ offline" else ""
                h.detail.text = "~${e.approxMapSizeMb} MB vector • street names • bbox %.1f,%.1f → %.1f,%.1f".format(e.south, e.west, e.north, e.east)
                h.status.text = if (downloaded) "Ready offline. Delete to free space." else "Not downloaded. Needs internet once (~${e.approxMapSizeMb} MB)."
                h.dl.isEnabled = jobs[e.id]?.isActive != true
                h.bar.visibility = if (jobs[e.id]?.isActive == true) View.VISIBLE else View.GONE
                h.dl.setOnClickListener {
                    jobs[e.id]?.cancel()
                    jobs[e.id] = lifecycleScope.launch {
                        h.bar.visibility = View.VISIBLE
                        h.status.text = "Starting…"
                        try {
                            val dl = MapDownloader(requireContext())
                            withContext(Dispatchers.Main) { Toast.makeText(requireContext(), "Downloading ${e.displayName}… keep app open", Toast.LENGTH_LONG).show() }
                            dl.downloadMap(e,
                                onProgress = { p -> lifecycleScope.launch(Dispatchers.Main) { h.bar.progress = p; h.status.text = "Downloading… $p%" } },
                                onLog = { msg -> lifecycleScope.launch(Dispatchers.Main) { h.status.text = msg.take(300) } })
                            h.status.text = "✓ Done — works offline now"
                            // center map on this country
                            MapState.lastLat = e.centerLat; MapState.lastLon = e.centerLon
                        } catch (ex: Exception) {
                            if (ex is CancellationException) h.status.text = "Cancelled"
                            else h.status.text = "Failed: ${ex.message}\nTry again on Wi-Fi. Raster cache still works via Map tab."
                        } finally {
                            h.bar.visibility = View.GONE
                            tvStorage.text = MapStorage.storageSummary(requireContext())
                            refresh()
                        }
                    }
                }
                h.del.setOnClickListener {
                    jobs[e.id]?.cancel()
                    MapStorage.mapFile(requireContext(), e.id).delete()
                    MapStorage.pbfFile(requireContext(), e.id).delete()
                    MapStorage.graphDir(requireContext(), e.id).deleteRecursively()
                    refresh()
                    Toast.makeText(requireContext(), "Deleted ${e.displayName}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tv_name)
        val detail: TextView = v.findViewById(R.id.tv_detail)
        val dl: Button = v.findViewById(R.id.btn_download)
        val del: Button = v.findViewById(R.id.btn_delete)
        val bar: ProgressBar = v.findViewById(R.id.progress)
        val status: TextView = v.findViewById(R.id.tv_status)
    }
}
