package com.example.tacticfakegps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.tacticfakegps.ui.theme.*

@Composable
fun MgrsInputScreen(
    logText: String,
    viewModel: LocationViewModel,
    onMgrsEntered: (String) -> Unit
) {
    val input by viewModel.mgrsCoordinates.collectAsState()
    var toggleState by remember { mutableStateOf(false) }
    val mgrsRegex = Regex("""^(?:[1-9]|[1-5][0-9]|60)[C-HJ-NP-X][A-HJ-NP-Z]{2}[0-9]{10}$""")
    val isInputValid = input.matches(mgrsRegex)
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("Karte", "Logošana")

    LaunchedEffect(toggleState) {
        if (toggleState) {
            viewModel.startMockLocationLoop(input)
        } else {
            viewModel.disableMockLocation()
        }
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        Text(
            "MGRS koordināšu ievade un lokācijas simulācija",
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
                }, modifier = Modifier.weight(1f)
            )

            Switch(
                checked = toggleState,
                onCheckedChange = { toggleState = it },
                enabled = isInputValid,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ToggleGreen,
                    checkedTrackColor = ToggleGreenLight,
                    uncheckedThumbColor = ToggleRed,
                    uncheckedTrackColor = ToggleRedLight,
                    uncheckedBorderColor = ToggleRedBorderLight,
                    disabledCheckedThumbColor = ToggleDisabledThumb,
                    disabledCheckedTrackColor = ToggleDisabledTrack,
                    disabledCheckedBorderColor = ToggleDisabledBorder,
                    disabledUncheckedThumbColor = ToggleDisabledThumb,
                    disabledUncheckedTrackColor = ToggleDisabledTrack,
                    disabledUncheckedBorderColor = ToggleDisabledBorder
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color.Transparent,
            indicator = { tabPositions ->
                Box(
                    Modifier
                        .tabIndicatorOffset(tabPositions[selectedTabIndex])
                        .height(0.dp)
                )
            },
            divider = {}
        ) {
            tabTitles.forEachIndexed { index, title ->
                val isSelected = selectedTabIndex == index
                Tab(
                    selected = isSelected,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            color = if (isSelected) Color.White else ToggleGreen
                        )
                    },
                    modifier = Modifier
                        .background(if (isSelected) ToggleGreen else ToggleGreenLight)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTabIndex) {
            0 -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                        .weight(1f)
                ) {
                    // TODO: ŠEIT jāievieto karte (MapLibreView)
                }
            }

            1 -> {
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
    }
}
