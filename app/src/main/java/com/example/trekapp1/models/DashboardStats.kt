package com.example.trekapp1.models

/**
 * Data class representing dashboard statistics.
 * All values default to "--" when no data is available.
 *
 * @property distance Total distance covered (defaults to "--").
 * @property steps Total step count (defaults to "--").
 * @property calories Calories burned (defaults to "--").
 * @property pace Average pace (defaults to "--").
 */
data class DashboardStats(
    val distance: String = "--",
    val steps: String = "--",
    val calories: String = "--",
    val pace: String = "--"
)