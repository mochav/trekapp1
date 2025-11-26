package com.example.trekapp1.controllers

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.trekapp1.HealthConnectManager
import com.example.trekapp1.models.DashboardStats

/**
 * Controller for managing dashboard statistics.
 * Integrates with HealthConnect to fetch user health data.
 *
 * @property healthConnectManager Manager for accessing HealthConnect API.
 */
class DashboardController(
    private val healthConnectManager: HealthConnectManager
) {

    /**
     * Current dashboard statistics.
     * Exposed as Compose state for automatic recomposition.
     */
    var stats by mutableStateOf(DashboardStats())
        private set

    /**
     * Loading state indicator.
     */
    var isLoading by mutableStateOf(false)
        private set

    /**
     * Error message if stats loading fails.
     */
    var error by mutableStateOf<String?>(null)
        private set

    /**
     * Loads statistics from HealthConnect.
     * Should be called from a coroutine scope.
     */
    suspend fun loadStats() {
        isLoading = true
        error = null

        try {
            val steps = healthConnectManager.readTodaySteps()
            val calories = healthConnectManager.readTodayCalories()

            stats = DashboardStats(
                distance = "24.5", // TODO: Calculate from steps or GPS data
                steps = steps?.toString() ?: "--",
                calories = calories?.let { String.format("%.0f", it) } ?: "--",
                pace = "5:32" // TODO: Calculate from actual data
            )
        } catch (e: Exception) {
            error = e.message
            stats = DashboardStats()
        } finally {
            isLoading = false
        }
    }

    /**
     * Triggers a refresh of statistics.
     * Should be called from UI with appropriate coroutine scope.
     */
    fun refreshStats() {
        // Trigger refresh - typically called from UI with coroutine scope
    }
}