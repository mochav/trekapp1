package com.example.trekapp1.models

/**
 * Represents aggregated statistics displayed on the dashboard.
 *
 * @property distance Total distance covered (default "--").
 * @property steps Total steps taken (default "--").
 * @property calories Total calories burned (default "--").
 * @property pace Average pace (default "--").
 */
data class DashboardStats(
    val distance: String = "--",
    val steps: String = "--",
    val calories: String = "--",
    val pace: String = "--"
)