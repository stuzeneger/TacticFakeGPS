package com.example.tacticfakegps

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import mil.nga.mgrs.MGRS

class LocationViewModel(private val application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("tactic_prefs", Context.MODE_PRIVATE)
    private val _mgrsCoordinates = MutableStateFlow("")
    val mgrsCoordinates: StateFlow<String> = _mgrsCoordinates

    private val _logText = MutableStateFlow("")
    val logText: StateFlow<String> = _logText

    private var mockJob: Job? = null
    private var lastMgrs: String? = null
    private var isMockRunning = false

    private val isDebuggable: Boolean =
        (application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    fun saveMgrsToPrefs(mgrs: String) {
        prefs.edit().putString(PrefKeys.PREF_MGRS_COORDINATES, mgrs).apply()
        appendLog("Saglabātas koordinātas: $mgrs")
    }

    fun loadMgrsFromPrefs() {
        appendLog("Ielādē aplikācijas iestatījumus")
        val savedMgrs = prefs.getString(PrefKeys.PREF_MGRS_COORDINATES, null)
        appendLog("Saglabātās koordinātas: $savedMgrs")
        if (savedMgrs != null) {
            viewModelScope.launch {
                _mgrsCoordinates.emit(savedMgrs)
                lastMgrs = savedMgrs
                appendLog("Ielādētas saglabātās koordinātas: $savedMgrs")
            }
        }
    }

    fun appendLog(line: String) {
        viewModelScope.launch {
            val updated = if (_logText.value.isBlank()) line else "${_logText.value}\n$line"
            _logText.emit(updated)
        }
    }

    @SuppressLint("MissingPermission")
    fun startMockLocationViaLocationManager(lat: Double, lon: Double) {
        val locationManager = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providerName = LocationManager.GPS_PROVIDER

        try {
            try {
                locationManager.removeTestProvider(providerName)
            } catch (_: Exception) {}

            locationManager.addTestProvider(
                providerName,
                false, false, false, false,
                true, true, true,
                android.location.Criteria.POWER_LOW,
                android.location.Criteria.ACCURACY_FINE
            )
            locationManager.setTestProviderEnabled(providerName, true)

            val mockLocation = Location(providerName).apply {
                latitude = lat
                longitude = lon
                accuracy = 1f
                time = System.currentTimeMillis()
                elapsedRealtimeNanos = System.nanoTime()
            }
            locationManager.setTestProviderLocation(providerName, mockLocation)
        } catch (e: Exception) {
            appendLog("Neizdevās nosūtīt lokācijas simulāciju ar LocationManager: ${e.message}")
            if (e.message?.contains("mock", ignoreCase = true) == true) {
                appendLog("Atver Developer Settings, jo nav izvēlēta mock aplikācija.")
                openDeveloperSettings()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startMockLocationLoop(mgrsInput: String) {
        val mgrs = mgrsInput.trim()
        if (mgrs.isEmpty()) {
            appendLog("MGRS vērtība ir tukša — pārbaudi ievadi.")
            return
        } else {
            saveMgrsToPrefs(mgrs)
        }

        val lat: Double
        val lon: Double
        try {
            val point = MGRS.parse(mgrs).toPoint()
            lat = point.y
            lon = point.x
        } catch (e: Exception) {
            appendLog("MGRS formāta kļūda: ${e.message}")
            return
        }

        lastMgrs = mgrs

        try {
            isMockRunning = true
            appendLog("Lokācijas simulācija ieslēgta")
        } catch (e: SecurityException) {
            appendLog("Lokācijas simulācija nav atļautq šai lietotnei! Iespējams, tā nav norādīta kā 'Mock location app'.")
            openDeveloperSettings()
            return
        } catch (e: Exception) {
            appendLog("Neizdevās ieslēgt lokācijas simulāciju: ${e.message}")
            return
        }

        mockJob?.cancel()
        mockJob = viewModelScope.launch {
            while (isActive) {
                try {
                    startMockLocationViaLocationManager(lat, lon)
                } catch (e: SecurityException) {
                    appendLog("Neatļauts nosūtīt lokācijas simulācija: ${e.message}")
                    cancel()
                } catch (e: Exception) {
                    appendLog("Lokācijas simulācijas kļūda: ${e.message}")
                    cancel()
                }
                delay(AppSettings.mockIntervalMs)
            }
        }
    }

    fun stopMockLocationLoop() {
        mockJob?.cancel()
        mockJob = null

        if (isDebuggable) {
            try {
                isMockRunning = false
                appendLog("Lokācijas simulācija izsleģta")
            } catch (e: Exception) {
                appendLog("Neizdevās izslēgt lokacijas simulācijas režīmu: ${e.message}")
            }
        }
    }

    private fun openDeveloperSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        application.startActivity(intent)
    }

    fun isBootEnabled(): Boolean {
        return prefs.getBoolean(PrefKeys.PREF_BOOT_LOCATION_ENABLED, false)
    }

    fun setBootLocationEnabled(enabled: Boolean) {
        val currentMgrs = mgrsCoordinates.value
        appendLog("Ievadītās koordinātes: $currentMgrs")

        prefs.edit().putBoolean(PrefKeys.PREF_BOOT_LOCATION_ENABLED, enabled).apply()
        appendLog("Lokācijas simulācija tika iestatīta uz $enabled")
        if (!enabled) {
            stopMockLocationLoop()
            prefs.edit().remove(PrefKeys.PREF_MGRS_COORDINATES).apply()
            viewModelScope.launch {
                appendLog("MGRS koordinātes tika izdzēstas, jo lokācijas simulācija ir izslēgta")
            }
        }
        else {
            saveMgrsToPrefs(currentMgrs)
            appendLog("MGRS koordinātes tika saglabātas, jo lokācijas simulācija ir ieslēgts")
        }
    }

    fun updateMgrsCoordinatesIfEnabled(newCoords: String) {
        if (isBootEnabled()) {
            viewModelScope.launch {
                _mgrsCoordinates.emit(newCoords)
                appendLog("MGRS koordinātes atjaunotas: $newCoords")
            }
        } else {
            appendLog("MGRS koordinātes netika atjaunotas, jo boot_location ir izslēgts")
        }
    }

    fun updateMgrsCoordinatesManually(input: String) {
        viewModelScope.launch {
            _mgrsCoordinates.emit(input)
        }

    fun saveMgrsToPrefs(mgrs: String) {
            prefs.edit().putString(PrefKeys.PREF_MGRS_COORDINATES, mgrs).apply()
            appendLog("Saglabātas koordinātas: $mgrs")
        }
    }

    fun disableMockLocation() {
        appendLog("Izslēgt lokācijas simulāciju")
        setBootLocationEnabled(false)
        stopMockLocationLoop()
    }
}
