package com.example.tacticfakegps

import android.app.Application
import android.content.pm.ApplicationInfo

object AppSettings {

    var mockIntervalMs: Long = 5000L
    val randomShiftProbability: Double = 0.25
    val shiftLat: Double = 0.0000089
    val shiftLon: Double = 0.0000113
    val settingsName: String = "tactic_fakegps_prefs"


    fun isDebuggable(application: Application): Boolean {
        return (application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
}
