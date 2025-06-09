import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ScaleBarOverlay
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalContext
import com.example.tacticfakegps.LocationViewModel
import java.io.File
import org.osmdroid.tileprovider.tilesource.XYTileSource
import kotlinx.coroutines.*
import androidx.compose.ui.Alignment
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp

object MapRef {
    @Volatile
    var mapView: MapView? = null
}

@Composable
fun OsmMapView(context: Context) {
    AndroidView(
        factory = { ctx ->
            val osmdroidBasePath = File(ctx.getExternalFilesDir(null), "osmdroid")
            val osmdroidTileCache = File(osmdroidBasePath, "tiles")

            Configuration.getInstance().apply {
                this.osmdroidBasePath = osmdroidBasePath
                this.osmdroidTileCache = osmdroidTileCache
                load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
                Log.d("OsmMapView", "Konfigurācija ielādēta: $osmdroidBasePath")
            }

            val offlineTileSource = XYTileSource(
                "LatviaOffline",
                0,
                18,
                256,
                ".png",
                arrayOf()
            )

            MapView(ctx).apply {
                setTileSource(offlineTileSource)
                setMultiTouchControls(true)
                controller.setZoom(15.0)
                controller.setCenter(GeoPoint(56.9496, 24.1052)) // Rīga
                overlays.add(ScaleBarOverlay(this))
                MapRef.mapView = this
                Log.d("OsmMapView", "MapView inicializēts ar offline avotu.")
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun OsmMapScreen(viewModel: LocationViewModel) {
    val context = LocalContext.current
    var isCacheReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            Log.d("OsmMapScreen", "Sākam pārbaudīt Latvia keša esamību...")
            if (!isLatviaCached(context)) {
                Log.d("OsmMapScreen", "Kešs NAV atrasts. Sākam lejupielādi.")
                preloadLatviaTiles(context)
                Log.d("OsmMapScreen", "Lejupielāde pabeigta.")
            } else {
                Log.d("OsmMapScreen", "Kešs jau IR pieejams. Lejupielāde nav nepieciešama.")
            }
            isCacheReady = true
            Log.d("OsmMapScreen", "isCacheReady = true, kartes parādīšana tiks sākta.")
        }
    }

    if (isCacheReady) {
        OsmMapView(context)
        SideEffect {
            MapRef.mapView?.let {
                Log.d("OsmMapScreen", "MapView pieejams. Ielādējam MGRS no preferences.")
                viewModel.loadMgrsFromPrefs()
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text("Notiek kartes ielāde...", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
