package com.example.trekapp1.models

/**
 * Represents a single running/walking activity session.
 *
 * @property id Unique identifier for the activity.
 * @property date Date and time when the activity occurred.
 * @property distance Total distance covered in the activity.
 * @property duration Total time spent on the activity.
 * @property pace Average pace during the activity.
 */
data class ActivityRecord(
    val id: String,
    val date: String,
    val distance: String,
    val duration: String,
    val pace: String
)