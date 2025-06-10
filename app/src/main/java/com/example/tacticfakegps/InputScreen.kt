package com.example.tacticfakegps

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.example.tacticfakegps.ui.theme.*
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp

object MapRef {
    var mapView: MapView? = null
}

@Composable
@Suppress("UNUSED_PARAMETER")
fun InputScreen(
    logText: String,
    viewModel: LocationViewModel,
    onMgrsEntered: (String) -> Unit
) {
    val context: Context = LocalContext.current
    val view = LocalView.current

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            PreferenceManager.getDefaultSharedPreferences(context)
        )
    }

    val input by viewModel.mgrsCoordinates.collectAsState()
    val isInputValid = viewModel.isValidMgrs(input)
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Karte", "Vēsture")
    var selectedPin by remember { mutableStateOf<GeoPoint?>(null) }
    var toggleState by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.mockEnabled.collect { enabled ->
            toggleState = enabled
        }
    }

    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex == 0) {
            while (MapRef.mapView == null) {
                delay(100)
            }
            if (isInputValid) {
                viewModel.centerMapOnMgrsPoint(input)
            }
        }
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        Text(
            "Pilota lokācijas vietas simulācija",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = input,
            onValueChange = { newValue ->
                val filtered = newValue.uppercase()
                    .filter { it.isDigit() || it in 'A'..'Z' }
                    .take(15)
                viewModel.updateMgrsCoordinatesManually(filtered)
            //    hideKeyboard(context, view.windowToken)
            },
            label = { Text("Ievadi MGRS koordinātes") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = when {
                    !isInputValid -> "Nepilnīgas koordinātes"
                    toggleState -> "Lokācijas simulācija ieslēgta"
                    else -> "Lokācijas simulācija izslēgta"
                },
                modifier = Modifier.weight(1f)
            )

            Switch(
                checked = toggleState,
                onCheckedChange = { newState ->
                    hideKeyboard(context, view.windowToken)
                    toggleState = newState
                    if (newState) {
                        viewModel.startMockLocationLoop()
                    } else {
                        viewModel.disableMockLocation()
                    }
                },
                enabled = isInputValid,
                colors = mySwitchColors(),
                modifier = Modifier
                    .scale(1.5f)
                    .padding(end = 16.dp),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color.Transparent,
            indicator = {},
            divider = {}
        ) {
            tabTitles.forEachIndexed { index, title ->
                val isSelected = selectedTabIndex == index
                Tab(
                    selected = isSelected,
                    onClick = {
                        hideKeyboard(context, view.windowToken)
                        selectedTabIndex = index
                    },
                    text = {
                        Text(
                            text = title,
                            color = if (isSelected) Color.White else ToggleGreen
                        )
                    },
                    modifier = Modifier.background(
                        if (isSelected) ToggleGreen else ToggleGreenLight
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTabIndex) {
            0 -> Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            createMapViewWithPinListener(ctx) { geoPoint ->
                                selectedPin = geoPoint
                                viewModel.updateCoordinatesFromPin(
                                    geoPoint.longitude,
                                    geoPoint.latitude
                                )
                            }
                        },
                        update = { mapView ->
                            mapView.overlays.clear()
                            selectedPin?.let { geoPoint ->
                                val marker = createPilotMarker(mapView, geoPoint)
                                mapView.overlays.add(marker)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            1 -> Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
            ) {
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(logText, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

fun createMapViewWithPinListener(
    context: Context,
    onPinSelected: (GeoPoint) -> Unit
): MapView {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val mapView:MyCustomMapView = MyCustomMapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)

        val zoom = prefs.getFloat(PrefKeys.PREF_ZOOM_LEVEL, AppSettings.DEFAULT_ZOOM).toDouble()
        controller.setZoom(zoom)
        controller.setCenter(GeoPoint(AppSettings.DEFAULT_LATITUDE, AppSettings.DEFAULT_LONGITUDE))

        MapRef.mapView = this
    }

    val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                val geoPoint = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
                onPinSelected(geoPoint)
                mapView.performClick()
                return true
            }
        }
    )

    mapView.setOnTouchListener { v, event ->
        val handled = gestureDetector.onTouchEvent(event)
        if (handled) {
            v.performClick()
            true
        } else {
            false
        }
    }

    return mapView
}

fun createPilotMarker(mapView: MapView, position: GeoPoint): Marker {
    return Marker(mapView).apply {
        this.position = position
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        infoWindow = null
        icon = ContextCompat.getDrawable(mapView.context, R.drawable.ic_pin_pilot)
    }
}

fun hideKeyboard(context: Context, windowToken: android.os.IBinder) {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}
