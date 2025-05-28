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
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import mil.nga.mgrs.MGRS

class LocationViewModel(private val application: Application) : AndroidViewModel(application) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
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
        appendLog("loadMgrsFromPrefs()")
        val savedMgrs = prefs.getString(PrefKeys.PREF_MGRS_COORDINATES, null)
        appendLog("Saglabātās koordinātas!!!!: $savedMgrs")
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
    private fun sendMockLocationViaFusedClient(lat: Double, lon: Double) {
        val mockLoc = Location("gps").apply {
            latitude = lat
            longitude = lon
            accuracy = 1f
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = System.nanoTime()
        }
        fusedLocationClient.setMockLocation(mockLoc)
        appendLog("Nosūtīts mock ar FusedLocationClient: LAT=$lat, LON=$lon")
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
            appendLog("Nosūtīts mock ar LocationManager: LAT=$lat, LON=$lon")
        } catch (e: Exception) {
            appendLog("Neizdevās nosūtīt mock ar LocationManager: ${e.message}")
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
        }

        val lat: Double
        val lon: Double
        try {
            val point = MGRS.parse(mgrs).toPoint()
            lat = point.y
            lon = point.x
        } catch (e: Exception) {
            appendLog("MGRS parse kļūda: ${e.message}")
            return
        }

        lastMgrs = mgrs

        try {
            fusedLocationClient.setMockMode(true)
            isMockRunning = true
            appendLog("Mock režīms ieslēgts.")
        } catch (e: SecurityException) {
            appendLog("Mock nav atļauts šai lietotnei! Iespējams, tā nav norādīta kā 'Mock location app'.")
            openDeveloperSettings()
            return
        } catch (e: Exception) {
            appendLog("Neizdevās ieslēgt mock režīmu: ${e.message}")
            return
        }

        mockJob?.cancel()
        mockJob = viewModelScope.launch {
            while (isActive) {
                try {
                    sendMockLocationViaFusedClient(lat, lon)
                    startMockLocationViaLocationManager(lat, lon)
                } catch (e: SecurityException) {
                    appendLog("Neatļauts nosūtīt mock lokāciju: ${e.message}")
                    cancel()
                } catch (e: Exception) {
                    appendLog("Mock kļūda: ${e.message}")
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
                fusedLocationClient.setMockMode(false)
                isMockRunning = false
                appendLog("Mock režīms izslēgts.")
            } catch (e: Exception) {
                appendLog("Neizdevās izslēgt mock režīmu: ${e.message}")
            }
        }
    }

    private fun openDeveloperSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        application.startActivity(intent)
    }

    fun bootDeviceLocation() {
        prefs.edit().putBoolean(PrefKeys.PREF_BOOT_LOCATION_ENABLED, true).apply()
        appendLog("Starts pie sāknēšanas iestatīts")
    }

    fun isBootEnabled(): Boolean {
        return prefs.getBoolean(PrefKeys.PREF_BOOT_LOCATION_ENABLED, false)
    }

    fun setBootLocationEnabled(enabled: Boolean) {
        val currentMgrs = mgrsCoordinates.value
        appendLog("Ievadītās koordinātes: $currentMgrs")

        prefs.edit().putBoolean(PrefKeys.PREF_BOOT_LOCATION_ENABLED, enabled).apply()
        appendLog("boot_location_enabled tika iestatīts uz $enabled")
        if (!enabled) {
            stopMockLocationLoop()
            prefs.edit().remove(PrefKeys.PREF_MGRS_COORDINATES).apply()
            viewModelScope.launch {
                //_mgrsCoordinates.emit("")
                appendLog("MGRS koordinātes tika izdzēstas, jo boot_location_enabled ir izslēgts")
            }
        }
        else {
            saveMgrsToPrefs(currentMgrs)
            appendLog("MGRS koordinātes tika saglabātas, jo boot_location_enabled ir ieslēgts")
        }
    }


    fun toggleBootLocation() {
        val current = isBootEnabled()
        setBootLocationEnabled(!current)
    }

    fun disableMockLocation() {
        appendLog("Izslēgt lokācijas simulāciju")
        setBootLocationEnabled(false)
        stopMockLocationLoop()
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
    }
}
