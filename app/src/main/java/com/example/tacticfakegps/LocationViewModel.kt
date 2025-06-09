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
import org.osmdroid.util.GeoPoint
import kotlinx.coroutines.flow.MutableSharedFlow

class LocationViewModel(private val application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(AppSettings.settingsName, Context.MODE_PRIVATE)
    private val _mgrsCoordinates = MutableStateFlow("")
    val mgrsCoordinates: StateFlow<String> = _mgrsCoordinates
    private val _logText = MutableStateFlow("")
    val logText: StateFlow<String> = _logText
    private var mockJob: Job? = null
    private var isMockRunning = false
    private val _hideKeyboardEvent = MutableSharedFlow<Unit>()
    private val _mockEnabled = MutableStateFlow(isBootEnabled())
    val mockEnabled: StateFlow<Boolean> = _mockEnabled

    private val isDebuggable: Boolean =
        (application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    fun isValidMgrs(mgrs: String): Boolean {
        val mgrsRegex = Regex("""^(?:[1-9]|[1-5][0-9]|60)[C-HJ-NP-X][A-HJ-NP-Z]{2}[0-9]{10}$""")
        return mgrs.matches(mgrsRegex)
    }

    fun loadMgrsFromPrefs() {
        val mgrs = prefs.getString(PrefKeys.PREF_MGRS_COORDINATES, "") ?: ""
        appendLog("Saglabātās koordinātas: $mgrs")

        if (isValidMgrs(mgrs)) {
            viewModelScope.launch {
                _mgrsCoordinates.emit(mgrs)
                centerMapOnMgrsPoint(mgrs)
                appendLog("Ielādētas saglabātās koordinātas: $mgrs")
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
    fun startMockLocationViaLocationManager() {
        val locationManager = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providerName = LocationManager.GPS_PROVIDER

        try {
            try {
                locationManager.removeTestProvider(providerName)
            } catch (_: Exception) {}

            locationManager.addTestProvider(
                providerName,
                false, false, false, false,
                true, true,
                true,
                android.location.Criteria.POWER_LOW,
                android.location.Criteria.ACCURACY_FINE
            )

            locationManager.setTestProviderEnabled(providerName, true)

            val mgrs = _mgrsCoordinates.value
            val point = try {
                MGRS.parse(mgrs).toPoint()
            } catch (e: Exception) {
                appendLog("MGRS formāta kļūda: ${e.message}")
                return
            }

            val randomShiftProbability = AppSettings.randomShiftProbability
            val shiftLat = AppSettings.shiftLat
            val shiftLon = AppSettings.shiftLon

            var fakeLatitude = point.y
            var fakeLongitude = point.x

            if (Math.random() < randomShiftProbability) {
                when ((1..4).random()) {
                    1 -> fakeLatitude += shiftLat
                    2 -> fakeLatitude -= shiftLat
                    3 -> fakeLongitude += shiftLon
                    4 -> fakeLongitude -= shiftLon
                }
            }

            val mockLocation = Location(providerName).apply {
                latitude = fakeLatitude
                longitude = fakeLongitude
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
    fun startMockLocationLoop() {
        mockJob?.cancel()
        mockJob = viewModelScope.launch {
            isMockRunning = true
            appendLog("Lokācijas simulācija ieslēgta")
            while (isActive) {
                startMockLocationViaLocationManager()
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
                appendLog("Lokācijas simulācija izslēgta")
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

    private fun setBootLocationEnabled(enabled: Boolean) {
        val mgrs = mgrsCoordinates.value
        viewModelScope.launch {
            _mockEnabled.emit(enabled)
        }
        prefs.edit().putBoolean(PrefKeys.PREF_BOOT_LOCATION_ENABLED, enabled).apply()
        appendLog("Lokācijas simulācija tika iestatīta uz $enabled")
        if (!enabled) {
            stopMockLocationLoop()
        }
        else {
            saveMgrsToPrefs(mgrs)
            appendLog("MGRS koordinātes tika saglabātas, jo lokācijas simulācija ir ieslēgta")
        }
    }

    fun disableMockLocation() {
        appendLog("Izslēgt lokācijas simulāciju")
        setBootLocationEnabled(false)
        stopMockLocationLoop()
    }

    fun updateCoordinatesFromPin(latitude: Double, longitude: Double) {
            val mgrs = MGRS.from(latitude, longitude).toString()
            viewModelScope.launch {
                _mgrsCoordinates.emit(mgrs)
                saveMgrsToPrefs(mgrs)
            }
        }

    fun updateMgrsCoordinatesManually(mgrs: String) {
        viewModelScope.launch {
            _mgrsCoordinates.emit(mgrs)
            if (isValidMgrs(mgrs)) {
                centerMapOnMgrsPoint(mgrs)
                saveMgrsToPrefs(mgrs)
            }
        }
    }


    private fun saveMgrsToPrefs(mgrs: String) {
        prefs.edit().putString(PrefKeys.PREF_MGRS_COORDINATES, mgrs).apply()
        centerMapOnMgrsPoint(mgrs)
    }

    fun centerMapOnMgrsPoint(mgrs: String) {
        val map = MapRef.mapView
        val geoPoint: GeoPoint = try {
            val point = MGRS.parse(mgrs).toPoint()
            GeoPoint(point.y, point.x)
        } catch (e: Exception) {
            return
        }
        if (map == null) {
            viewModelScope.launch {
                delay(500)
                centerMapOnMgrsPoint(mgrs)
            }
            return
        }
        val marker = createPilotMarker(map, geoPoint)
        map.overlays.clear()
        map.controller.setCenter(geoPoint)
        map.overlays.add(marker)
        map.invalidate()
        viewModelScope.launch {
            _hideKeyboardEvent.emit(Unit)
        }
    }
}
