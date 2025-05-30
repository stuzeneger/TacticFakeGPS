package com.example.tacticfakegps

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import com.example.tacticfakegps.ui.theme.TacticFakeGPSTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: LocationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(
            this,
            LocationViewModelFactory(application)
        ).get(LocationViewModel::class.java)

        val prefs = getSharedPreferences("tactic_prefs", Context.MODE_PRIVATE)
        //val isBootEnabled = prefs.getBoolean("boot_location_enabled", false)
        viewModel.loadMgrsFromPrefs()

        setContent {
            TacticFakeGPSTheme {
                val log by viewModel.logText.collectAsState()
                MgrsInputScreen(
                    logText = log,
                    viewModel = viewModel,
                    onMgrsEntered = { inputMgrs ->
                        viewModel.appendLog("Lietotājs ievadīja MGRS: $inputMgrs")
                        viewModel.updateMgrsCoordinatesIfEnabled(inputMgrs)
                        viewModel.startMockLocationLoop(inputMgrs)
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
