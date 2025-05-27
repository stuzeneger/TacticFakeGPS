package com.example.tacticfakegps

import android.app.Application
import android.content.pm.ApplicationInfo

object AppSettings {

    // Sākotnējais mock intervāls (milisekundēs)
    var mockIntervalMs: Long = 5000L

    // Debug režīma pārbaude
    fun isDebuggable(application: Application): Boolean {
        return (application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
}
