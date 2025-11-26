package com.example.trekapp1.models

/**
 * Represents real-time statistics during an active tracking session.
 *
 * @property steps Current step count in the session (default "0").
 * @property distance Current distance covered in the session (default "0.0 km").
 * @property calories Current calories burned in the session (default "0").
 * @property time Elapsed time in the session (default "00:00").
 */
data class TrackingSessionStats(
    val steps: String = "0",
    val distance: String = "0.0 km",
    val calories: String = "0",
    val time: String = "00:00"
)