package com.example.trekapp1.controllers

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.trekapp1.HealthConnectManager
import com.example.trekapp1.models.ActivityRecord
import com.example.trekapp1.models.TrackingSessionStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Controller for managing active tracking sessions.
 * Handles starting/stopping tracking and updating real-time statistics.
 */
class TrackingController(private val healthConnectManager: HealthConnectManager){

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

    private var sessionStartTime: Instant? = null

    /** Starting values for sessions */
    private var startingSteps: Long? = null
    private var startingCalories: Double? = null

    /** Current session steps from the step sensor. */
    private var baseSensorSteps: Long? = null
    /** Current session steps from the step sensor. */
    private var currSessionSteps: Long = 0

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
        sessionStartTime = Instant.now()

        baseSensorSteps = null
        currSessionSteps = 0

        trackingJob = scope.launch {
            startingSteps = healthConnectManager.readTodaySteps()
            startingCalories = healthConnectManager.readTodayCalories()
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
     * Writes session to HealthConnect. Should be called from UI after "End Session"
     * */
    suspend fun endSessionAndSave() {
        val start = sessionStartTime
        val end = Instant.now()
        stopTracking()
        if (start != null) {
            val steps = sessionStats.steps.toLongOrNull() ?: 0L
            val calories = sessionStats.calories.toDoubleOrNull() ?: 0.0
            if (steps > 0 || calories > 0.0) {
                healthConnectManager.writeExerciseSession(
                    start = start,
                    end = end,
                    steps = steps,
                    calories = calories
                )
            }
        }
        sessionStartTime = null
    }

    fun updateStepsFromSensor(stepsSinceRun: Long){
        if(!isTracking) return
        val base = baseSensorSteps ?: run{
            baseSensorSteps = stepsSinceRun
            stepsSinceRun
        }
        currSessionSteps = (stepsSinceRun - base).coerceAtLeast(0L)
    }

    /**
     * Updates session statistics based on HealthConnect data.
     * Real sensor data added.
     */
    private suspend fun updateStats() {
        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60

        val steps = currSessionSteps
        val distance = steps * 0.00072 //(average man 0.76m/step ; average woman 0.67m/step -> average overall 0.72m/step = 0.00072km)
        val calories = steps * 0.0465 //0.0465 avg calories burned for ppl between 160 to 200 lb

        sessionStats = sessionStats.copy(
            steps = steps.toString(),
            distance = String.format("%.2f mi", distance),
            calories = calories.toInt().toString(),
            time = String.format("%02d:%02d", minutes, seconds)
        )

        /**sessionStats = sessionStats.copy(
            steps = "trying to modify ", // TODO: Get from step counter
            distance = String.format("%.2f km", elapsedSeconds * 0.001), // TODO: Get from GPS
            calories = (elapsedSeconds / 10).toString(), // TODO: Calculate from activity
            time = String.format("%02d:%02d", minutes, seconds)
        )*/
    }


    fun buildActivityRecord(): ActivityRecord {
        return ActivityRecord(
            id = System.currentTimeMillis().toString(),
            date = Instant.now().toString(),
            distance = sessionStats.distance,
            duration = sessionStats.time,
            pace = calculatePace(sessionStats.time, sessionStats.distance)
        )
    }


    private fun calculatePace(duration: String, distance: String): String {
        // distance format example: "0.42 mi", so extract numeric part
        val dist = distance.split(" ")[0].toDoubleOrNull() ?: 0.0
        if (dist <= 0.0) return "--"

        val parts = duration.split(":")
        val minutes = parts[0].toInt()
        val seconds = parts[1].toInt()
        val totalMin = minutes + seconds / 60.0

        val pace = totalMin / dist
        return String.format("%.2f min/mi", pace)
    }
    /**
     * Resets the tracking session.
     * Stops tracking and clears all statistics.
     */
    fun reset() {
        stopTracking()
        elapsedSeconds = 0
        startingSteps = null
        startingCalories = null
        sessionStats = TrackingSessionStats()

    }
}
