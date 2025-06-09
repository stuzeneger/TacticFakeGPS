import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.*

suspend fun preloadLatviaTiles(context: Context, zoomMin: Int = 6, zoomMax: Int = 15) {
    val osmdroidBasePath = File(context.getExternalFilesDir(null), "osmdroid")
    val tileCacheDir = File(osmdroidBasePath, "tiles/Mapnik") // Mapnik tile avots
    if (!tileCacheDir.exists()) tileCacheDir.mkdirs()

    // Latvijas koordinātes apgabals
    val latMin = 55.5
    val latMax = 58.1
    val lonMin = 20.0
    val lonMax = 28.2

    for (zoom in zoomMin..zoomMax) {
        // pārvēršam lat/lon uz tile koordinātēm
        val xMin = lonToTileX(lonMin, zoom)
        val xMax = lonToTileX(lonMax, zoom)
        val yMin = latToTileY(latMax, zoom) // uzmanies — y palielinās uz leju
        val yMax = latToTileY(latMin, zoom)

        for (x in xMin..xMax) {
            for (y in yMin..yMax) {
                val tileDir = File(tileCacheDir, "$zoom/$x")
                if (!tileDir.exists()) tileDir.mkdirs()

                val tileFile = File(tileDir, "$y.png")
                if (!tileFile.exists()) {
                    val url = "https://tile.openstreetmap.org/$zoom/$x/$y.png"
                    downloadTile(url, tileFile)
                }
            }
        }
    }
}

// Palīgfunkcijas tile koordinātu aprēķināšanai
fun lonToTileX(lon: Double, zoom: Int): Int {
    return floor((lon + 180) / 360 * (1 shl zoom)).toInt()
}

fun latToTileY(lat: Double, zoom: Int): Int {
    val latRad = Math.toRadians(lat)
    return floor((1 - ln(tan(latRad) + 1 / cos(latRad)) / Math.PI) / 2 * (1 shl zoom)).toInt()
}

// Funkcija lejupielādei
suspend fun downloadTile(urlStr: String, file: File) {
    withContext(Dispatchers.IO) {
        try {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                conn.inputStream.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun isLatviaCached(context: Context): Boolean {
    val cacheDir = File(context.getExternalFilesDir(null), "osmdroid/tiles")
    val files = cacheDir.listFiles()
    //Log.d("isLatviaCached", "Pārbaudu kešu: ${cacheDir.path}, faili: ${files?.size ?: 0}")
    return files?.isNotEmpty() == true
}
