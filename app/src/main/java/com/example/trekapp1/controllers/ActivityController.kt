package com.example.trekapp1.controllers

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.trekapp1.dao.ActivityDao
import com.example.trekapp1.localDatabase.LocalActivity
import com.example.trekapp1.models.ActivityRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Controller for managing activity records.
 * Handles loading, storing, and providing access to user activities.
 *
 * @param activityDao DAO for database operations
 * @param coroutineScope Scope for launching coroutines
 */
class ActivityController(
    private val activityDao: ActivityDao? = null,
    private val coroutineScope: CoroutineScope? = null
) {

    /**
     * List of all user activities.
     * Exposed as Compose state for automatic recomposition.
     */
    var activities by mutableStateOf<List<ActivityRecord>>(emptyList())
        private set

    /**
     * Loading state for UI feedback.
     */
    var isLoading by mutableStateOf(false)
        private set

    init {
        // Load activities from database or use sample data
        loadActivities()
    }

    /**
     * Loads activities from database.
     * Falls back to empty list if database is not available.
     */
    private fun loadActivities() {
        if (activityDao != null && coroutineScope != null) {
            coroutineScope.launch {
                isLoading = true
                try {
                    val dbActivities = withContext(Dispatchers.IO) {
                        activityDao.getAllActivities()
                    }
                    // Convert LocalActivity to ActivityRecord
                    activities = dbActivities.map { it.toActivityRecord() }
                } catch (e: Exception) {
                    // On error, set to empty list
                    activities = emptyList()
                } finally {
                    isLoading = false
                }
            }
        } else {
            // No database available, use empty list
            activities = emptyList()
        }
    }

    /**
     * Returns sample activities for testing and demonstration.
     */
    private fun getSampleActivities(): List<ActivityRecord> {
        // No sample data - return empty list
        return emptyList()
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
     * Gets a single activity by ID.
     *
     * @param id The activity ID.
     * @return The activity record, or null if not found.
     */
    suspend fun getActivityById(id: String): ActivityRecord? {
        return if (activityDao != null) {
            withContext(Dispatchers.IO) {
                activityDao.getActivityById(id)?.toActivityRecord()
            }
        } else {
            activities.find { it.id == id }
        }
    }

    /**
     * Adds a new activity to the database and updates the list.
     *
     * @param activity The activity record to add.
     */
    fun addActivity(activity: ActivityRecord) {
        if (activityDao != null && coroutineScope != null) {
            coroutineScope.launch(Dispatchers.IO) {
                activityDao.insertActivity(activity.toLocalActivity())
                // Reload activities to reflect changes
                withContext(Dispatchers.Main) {
                    loadActivities()
                }
            }
        } else {
            // In-memory only
            activities = listOf(activity) + activities
        }
    }

    /**
     * Deletes an activity from the database.
     *
     * @param activity The activity to delete.
     */
    fun deleteActivity(activity: ActivityRecord) {
        if (activityDao != null && coroutineScope != null) {
            coroutineScope.launch(Dispatchers.IO) {
                activityDao.deleteActivity(activity.toLocalActivity())
                withContext(Dispatchers.Main) {
                    loadActivities()
                }
            }
        } else {
            // In-memory only
            activities = activities.filter { it.id != activity.id }
        }
    }

    /**
     * Refreshes activities from the database.
     */
    suspend fun refreshActivities() {
        loadActivities()
    }

    companion object
}

// Extension functions for converting between LocalActivity and ActivityRecord
fun LocalActivity.toActivityRecord(): ActivityRecord {
    return ActivityRecord(
        id = id,
        date = date,
        distance = distance,
        duration = duration,
        pace = pace
    )
}

fun ActivityRecord.toLocalActivity(): LocalActivity {
    return LocalActivity(
        id = id,
        date = date,
        distance = distance,
        duration = duration,
        pace = pace
    )
}