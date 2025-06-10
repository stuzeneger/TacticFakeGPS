package com.example.tacticfakegps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.tacticfakegps.ui.theme.TacticFakeGPSTheme
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: LocationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val viewModel: LocationViewModel by viewModels {
            ViewModelFactory(application)
        }

        viewModel.loadMgrsFromPrefs()

        setContent {
            TacticFakeGPSTheme {
                val log by viewModel.logText.collectAsState()
                InputScreen(
                    logText = log,
                    viewModel = viewModel,
                    onMgrsEntered = { mgrs ->
                        viewModel.appendLog("Lietotājs ievadīja MGRS: $mgrs")
                        viewModel.updateMgrsCoordinatesManually(mgrs)
                        if (viewModel.isBootEnabled()) {
                            viewModel.startMockLocationLoop()
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
