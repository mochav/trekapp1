package com.example.trekapp1.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Primary coral orange color used in gradients and accents. */
val CoralOrange = Color(0xFFFFA07A)

/** Primary coral pink color used in gradients and highlights. */
val CoralPink = Color(0xFFFF6B9D)

/** Dark background color for the app. */
val DarkBackground = Color(0xFF1A1A1A)

/** Card background color for elevated surfaces. */
val CardBackground = Color(0xFF2A2A2A)

/**
 * Main theme for the Trek running app.
 * Applies a dark color scheme with coral accent colors.
 *
 * @param content The composable content to wrap with the theme.
 */
@Composable
fun RunningAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = CoralPink,
            secondary = CoralOrange,
            background = DarkBackground,
            surface = CardBackground
        ),
        content = content
    )
}