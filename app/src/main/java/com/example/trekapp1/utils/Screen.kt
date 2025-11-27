package com.example.trekapp1.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Enum representing the main navigation screens in the app.
 *
 * @property title Display title for the screen.
 * @property icon Material icon associated with the screen.
 */
enum class Screen(val title: String, val icon: ImageVector) {
    /** Dashboard screen showing overview statistics. */
    Dashboard("Dashboard", Icons.Default.Dashboard),

    /** Activities screen showing all recorded activities. */
    Activities("Activities", Icons.Default.DirectionsRun),

    /** Avatars screen showing the avatar shop. */
    Avatars("Avatars", Icons.Default.Person)
}