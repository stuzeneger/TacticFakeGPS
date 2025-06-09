package com.example.tacticfakegps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import com.example.tacticfakegps.ui.theme.TacticFakeGPSTheme
import org.osmdroid.views.MapView

class MainActivity : ComponentActivity() {

    object MapHolder {
        var mapView: MapView? = null
    }

    private lateinit var viewModel: LocationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(
            this,
            LocationViewModelFactory(application)
        ).get(LocationViewModel::class.java)

        // Ielādē saglabātās koordinātes uz startu
        viewModel.loadMgrsFromPrefs()

        setContent {
            TacticFakeGPSTheme {
                val log by viewModel.logText.collectAsState()
                MgrsInputScreen(
                    logText = log,
                    viewModel = viewModel,
                    onMgrsEntered = { inputMgrs ->
                        viewModel.appendLog("Lietotājs ievadīja MGRS: $inputMgrs")
                        viewModel.updateMgrsCoordinatesManually(inputMgrs)
                        if (viewModel.isBootEnabled()) {
                            viewModel.startMockLocationLoop(inputMgrs)
                        }
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopMockLocationLoop()
    }
}
