package com.example.tacticfakegps.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
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
fun TacticFakeGPSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Izslēdzam dynamic color, lai netiktu mainītas krāsas Android 12+
    dynamicColor: Boolean = false,
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
