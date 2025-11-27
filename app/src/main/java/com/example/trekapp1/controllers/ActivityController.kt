package com.example.trekapp1.controllers

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.trekapp1.models.ActivityRecord

/**
 * Controller for managing activity records.
 * Handles loading, storing, and providing access to user activities.
 */
class ActivityController {

    /**
     * List of all user activities.
     * Exposed as Compose state for automatic recomposition.
     */
    var activities by mutableStateOf<List<ActivityRecord>>(emptyList())
        private set

    init {
        // Initialize with sample data
        loadSampleActivities()
    }

    /**
     * Loads sample activities for testing and demonstration purposes.
     * This will be replaced with backend data loading in the future.
     */
    private fun loadSampleActivities() {
        activities = listOf(
            ActivityRecord(
                id = "1",
                date = "Today, 8:30 AM",
                distance = "5.2 km",
                duration = "28:15",
                pace = "5:25 min/km"
            ),
            ActivityRecord(
                id = "2",
                date = "Yesterday, 7:00 AM",
                distance = "7.8 km",
                duration = "42:30",
                pace = "5:27 min/km"
            ),
            ActivityRecord(
                id = "3",
                date = "2 days ago",
                distance = "6.0 km",
                duration = "31:00",
                pace = "5:22 min/km"
            ),
            ActivityRecord(
                id = "4",
                date = "3 days ago",
                distance = "7.0 km",
                duration = "34:15",
                pace = "5:23 min/km"
            ),
            ActivityRecord(
                id = "5",
                date = "4 days ago",
                distance = "8.0 km",
                duration = "37:17",
                pace = "5:24 min/km"
            ),
            ActivityRecord(
                id = "6",
                date = "5 days ago",
                distance = "9.0 km",
                duration = "40:19",
                pace = "5:25 min/km"
            )
        )
    }

    /**
     * Gets the most recent activities.
     *
     * @param count Number of recent activities to return.
     * @return List of recent activities.
     */
    fun getRecentActivities(count: Int = 2): List<ActivityRecord> {
        return activities.take(count)
    }

    /**
     * Gets all activities.
     *
     * @return Complete list of all activities.
     */
    fun getAllActivities(): List<ActivityRecord> {
        return activities
    }

    /**
     * Adds a new activity to the beginning of the list.
     *
     * @param activity The activity record to add.
     */
    fun addActivity(activity: ActivityRecord) {
        activities = listOf(activity) + activities
    }

    /**
     * Refreshes activities from backend/database.
     * TODO: Implement backend integration for loading/saving activities.
     */
    suspend fun refreshActivities() {
        // Load from backend/database
    }
}