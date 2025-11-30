package com.example.trekapp1.controllers

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.trekapp1.HealthConnectManager
import com.example.trekapp1.models.ActivityRecord
import com.example.trekapp1.localDatabase.SyncManager
import com.example.trekapp1.models.DailyStats
import com.example.trekapp1.models.DashboardStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Controller for managing dashboard statistics.
 * Integrates with HealthConnect and ActivityController to fetch user health data.
 *
 * @property healthConnectManager Manager for accessing HealthConnect API.
 * @property activityController Controller for accessing activity records.
 */
class DashboardController(
    private val healthConnectManager: HealthConnectManager,
    private val activityController: ActivityController
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
     * Loads statistics from HealthConnect and calculates totals from activities.
     * Should be called from a coroutine scope.
     */
    suspend fun loadStats() {
        isLoading = true
        error = null

        try {
            // Get data from HealthConnect
            val steps = healthConnectManager.readTodaySteps()
            val calories = healthConnectManager.readTodayCalories()

            // Calculate totals from recent activities (only the ones shown on dashboard)
            val recentActivities = activityController.getRecentActivities(count = 2)
            val activityTotals = calculateActivityTotals(recentActivities)

            stats = DashboardStats(
                distance = activityTotals.totalDistance,
                steps = if (steps != null && steps > 0) steps.toString() else "--",
                calories = if (calories != null && calories > 0) String.format("%.0f", calories) else "--",
                pace = activityTotals.averagePace
            )
        } catch (e: Exception) {
            error = e.message
            stats = DashboardStats()
        } finally {
            isLoading = false
        }
    }

    /**
     * Calculates total distance and average pace from all activities.
     *
     * @param activities List of activity records.
     * @return ActivityTotals containing calculated totals.
     */
    private suspend fun calculateActivityTotals(activities: List<ActivityRecord>): ActivityTotals {
        return withContext(Dispatchers.Default) {
            if (activities.isEmpty()) {
                return@withContext ActivityTotals(
                    totalDistance = "--",
                    averagePace = "--"
                )
            }

            var totalMiles = 0.0
            var totalMinutes = 0.0
            var validActivities = 0

            activities.forEach { activity ->
                // Parse distance (e.g., "5.2 mi" or "5.2 km")
                val distanceValue = parseDistance(activity.distance)
                if (distanceValue > 0) {
                    totalMiles += distanceValue
                }

                // Parse duration (e.g., "28:15" = 28 minutes 15 seconds)
                val durationMinutes = parseDuration(activity.duration)
                if (durationMinutes > 0) {
                    totalMinutes += durationMinutes
                    validActivities++
                }
            }

            // Calculate average pace (min/mi)
            val averagePace = if (totalMiles > 0 && totalMinutes > 0) {
                val paceMinutes = totalMinutes / totalMiles
                formatPace(paceMinutes)
            } else {
                "--"
            }

            // Show distance as "--" if no valid data
            val distanceStr = if (totalMiles > 0) {
                String.format("%.1f", totalMiles)
            } else {
                "--"
            }

            ActivityTotals(
                totalDistance = distanceStr,
                averagePace = averagePace
            )
        }
    }

    /**
     * Parses distance string to double value in miles.
     * Supports formats: "5.2 mi", "5.2 km", "5.2"
     */
    private fun parseDistance(distance: String): Double {
        return try {
            val cleanDistance = distance.replace(Regex("[^0-9.]"), "").trim()
            val value = cleanDistance.toDoubleOrNull() ?: 0.0

            // Convert km to miles if needed
            if (distance.contains("km", ignoreCase = true)) {
                value * 0.621371 // km to miles conversion
            } else {
                value
            }
        } catch (e: Exception) {
            0.0
        }
    }

    /**
     * Parses duration string to minutes.
     * Supports formats: "28:15" (28 min 15 sec), "1:05:30" (1 hr 5 min 30 sec)
     */
    private fun parseDuration(duration: String): Double {
        return try {
            val parts = duration.split(":")
            when (parts.size) {
                2 -> {
                    // Format: MM:SS
                    val minutes = parts[0].toDoubleOrNull() ?: 0.0
                    val seconds = parts[1].toDoubleOrNull() ?: 0.0
                    minutes + (seconds / 60.0)
                }
                3 -> {
                    // Format: HH:MM:SS
                    val hours = parts[0].toDoubleOrNull() ?: 0.0
                    val minutes = parts[1].toDoubleOrNull() ?: 0.0
                    val seconds = parts[2].toDoubleOrNull() ?: 0.0
                    (hours * 60.0) + minutes + (seconds / 60.0)
                }
                else -> 0.0
            }
        } catch (e: Exception) {
            0.0
        }
    }

    /**
     * Formats pace in minutes to MM:SS format.
     */
    private fun formatPace(paceMinutes: Double): String {
        val minutes = paceMinutes.toInt()
        val seconds = ((paceMinutes - minutes) * 60).toInt()
        return String.format("%d:%02d", minutes, seconds)
    }

    /**
     * Triggers a refresh of statistics.
     * Should be called from UI with appropriate coroutine scope.
     */
    suspend fun refreshStats() {
        loadStats()
    }
}

/**
 * Data class to hold calculated activity totals.
 */
private data class ActivityTotals(
    val totalDistance: String,
    val averagePace: String
)

private fun SyncManager.updateDailyStats(stats: DailyStats) {}
