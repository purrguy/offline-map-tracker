package com.purrguy.offlinemaptracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.purrguy.offlinemaptracker.ui.*

class MainActivity : AppCompatActivity() {

    private val mapFrag = MapFragment()
    private val speedFrag = SpeedFragment()
    private val tracksFrag = TracksFragment()
    private val routeFrag = RouteFragment()
    private val dlFrag = DownloadFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tracksFrag.onViewOnMap = { show(mapFrag, R.id.nav_map) }
        routeFrag.onRouted = { show(mapFrag, R.id.nav_map) }

        show(mapFrag, R.id.nav_map)
        findViewById<BottomNavigationView>(R.id.bottom_nav).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_map -> show(mapFrag, R.id.nav_map)
                R.id.nav_speed -> show(speedFrag, R.id.nav_speed)
                R.id.nav_tracks -> show(tracksFrag, R.id.nav_tracks)
                R.id.nav_route -> show(routeFrag, R.id.nav_route)
                R.id.nav_download -> show(dlFrag, R.id.nav_download)
                else -> false
            }
        }
        askPerms()
    }

    private fun show(f: Fragment, id: Int): Boolean {
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, f).commit()
        return true
    }

    private fun askPerms() {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        val missing = perms.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) ActivityCompat.requestPermissions(this, missing.toTypedArray(), 99)
    }
}
