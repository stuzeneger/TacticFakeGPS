package com.example.tacticfakegps.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = DarkGreen,
    secondary = DarkBrown,
    tertiary = OliveGreen,
    background = Black,
    surface = DarkBrown,
    onPrimary = White,
    onSecondary = Black,
    onBackground = DarkGreen,
    onSurface = DarkGreen
)

private val LightColorScheme = lightColorScheme(
    primary = DarkGreen,
    secondary = DarkBrown,
    tertiary = OliveGreen,
    background = Black,
    surface = DarkBrown,
    onPrimary = White,
    onSecondary = Black,
    onBackground = DarkGreen,
    onSurface = DarkGreen
)

@Composable
fun mySwitchColors() = SwitchDefaults.colors(
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

@Composable
fun TacticFakeGPSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
