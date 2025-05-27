package com.example.tacticfakegps

import android.Manifest
import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.tacticfakegps.ui.theme.TacticFakeGPSTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: LocationViewModel

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            viewModel.appendLog("Nav piekļuves vietai — mock nedarbosies.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(
            this,
            LocationViewModelFactory(application)
        ).get(LocationViewModel::class.java)

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val prefs = getSharedPreferences("tactic_prefs", Context.MODE_PRIVATE)

        val isBootEnabled = prefs.getBoolean("boot_location_enabled", false)
        if (isBootEnabled) {
            viewModel.loadMgrsFromPrefs()
            viewModel.startMockLocationLoop()
            prefs.edit().putBoolean(PrefKeys.PREF_BOOT_LOCATION_ENABLED, false).apply()
            
            viewModel.appendLog("boot_location_enabled tika automātiski izslēgts pēc starta.")
        }
        viewModel.appendLog("boot_location_enabled = $isBootEnabled")

        setContent {
            TacticFakeGPSTheme {
                val mgrs by viewModel.mgrsCoordinates.collectAsState()
                val log by viewModel.logText.collectAsState()

                MgrsInputScreen(
                    mgrsCoordinates = mgrs,
                    logText = log,
                    viewModel = viewModel,
                    onMgrsEntered = { inputMgrs ->
                        viewModel.appendLog("Lietotājs ievadīja MGRS: $inputMgrs")
                        viewModel.updateMgrsCoordinatesIfEnabled(inputMgrs)
                        viewModel.saveMgrsToPrefs(inputMgrs)
                        viewModel.startMockLocationLoop()
                    },
                    onToggleBootChanged = { isChecked ->
                        viewModel.appendLog("Sāknēšana: $isChecked")
                        viewModel.setBootLocationEnabled(isChecked)
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
