package com.example.tacticfakegps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color

@Composable
fun MgrsInputScreen(
    logText: String,
    viewModel: LocationViewModel,
    onMgrsEntered: (String) -> Unit,
    onToggleBootChanged: (Boolean) -> Unit
) {
    val input by viewModel.mgrsCoordinates.collectAsState()
    var toggleState by remember { mutableStateOf(false) }

    val isBootEnabled by remember { mutableStateOf(viewModel.isBootEnabled()) }
    val mgrsRegex = Regex("""^(?:[1-9]|[1-5][0-9]|60)[C-HJ-NP-X][A-HJ-NP-Z]{2}[0-9]{10}$""")
    val isInputValid = input.matches(mgrsRegex)

    LaunchedEffect(isInputValid) {
        if (!isInputValid && toggleState) {
            toggleState = false
            onToggleBootChanged(false)
        }
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        Text("MGRS koordināšu ievade un lokācijas simulācija", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = input,
            onValueChange = { newValue ->
                val filtered = newValue.uppercase()
                    .filter { it.isDigit() || it in 'A'..'Z' }
                    .take(15)
                viewModel.updateMgrsCoordinatesManually(filtered)
            },
            label = { Text("Ievadi MGRS koordinātes") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text("Sāknēt ierīci ar lokācijas simulāciju", modifier = Modifier.weight(1f))
            Switch(
                checked = toggleState,
                onCheckedChange = { isChecked ->
                    if (isInputValid) {
                        toggleState = isChecked
                        onToggleBootChanged(isChecked)
                    }
                },
                enabled = isInputValid
            )
        }

        Button(
            onClick = {
                onMgrsEntered(input)
                viewModel.startMockLocationLoop(input)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = isInputValid
        ) {
            Text("Sākt lokācijas simulēšanu")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { viewModel.disableMockLocation() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD32F2F),
                contentColor = Color.White
            )
        ) {
            Text("Atslēgt lokācijas simulēšanu")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Log:")

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
