package com.example.trekapp1.controllers

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.trekapp1.models.TrackingSessionStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Controller for managing active tracking sessions.
 * Handles starting/stopping tracking and updating real-time statistics.
 */
class TrackingController {

    /**
     * Current tracking session statistics.
     * Exposed as Compose state for automatic recomposition.
     */
    var sessionStats by mutableStateOf(TrackingSessionStats())
        private set

    /**
     * Whether a tracking session is currently active.
     */
    var isTracking by mutableStateOf(false)
        private set

    /** Coroutine job for the tracking timer. */
    private var trackingJob: Job? = null

    /** Total elapsed seconds in current session. */
    private var elapsedSeconds = 0

    /**
     * Starts a new tracking session.
     * Begins updating statistics every second.
     *
     * @param scope CoroutineScope in which to launch the tracking job.
     */
    fun startTracking(scope: CoroutineScope) {
        if (isTracking) return

        isTracking = true
        elapsedSeconds = 0
        sessionStats = TrackingSessionStats()

        trackingJob = scope.launch {
            while (isTracking) {
                delay(1000)
                elapsedSeconds++
                updateStats()
            }
        }
    }

    /**
     * Stops the current tracking session.
     * Cancels the tracking job and preserves final statistics.
     */
    fun stopTracking() {
        isTracking = false
        trackingJob?.cancel()
        trackingJob = null
    }

    /**
     * Updates session statistics based on elapsed time.
     * TODO: Replace simulated data with real sensor data.
     */
    private fun updateStats() {
        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60

        sessionStats = sessionStats.copy(
            steps = (elapsedSeconds * 2).toString(), // TODO: Get from step counter
            distance = String.format("%.2f km", elapsedSeconds * 0.001), // TODO: Get from GPS
            calories = (elapsedSeconds / 10).toString(), // TODO: Calculate from activity
            time = String.format("%02d:%02d", minutes, seconds)
        )
    }

    /**
     * Resets the tracking session.
     * Stops tracking and clears all statistics.
     */
    fun reset() {
        stopTracking()
        elapsedSeconds = 0
        sessionStats = TrackingSessionStats()
    }
}
