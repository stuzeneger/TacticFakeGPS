package com.example.tacticfakegps

import android.content.Context
import androidx.preference.PreferenceManager
import org.osmdroid.views.MapView
import org.osmdroid.events.MapAdapter
import org.osmdroid.events.ZoomEvent

class MyCustomMapView(context: Context) : MapView(context) {

    init {
        setupZoomListener(context)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun setupZoomListener(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        this.addMapListener(object : MapAdapter() {
            override fun onZoom(event: ZoomEvent?): Boolean {
                val zoomLevel = this@MyCustomMapView.zoomLevelDouble
                prefs.edit().putFloat(PrefKeys.PREF_ZOOM_LEVEL, zoomLevel.toFloat()).apply()
                return true
            }
        })
    }
}

