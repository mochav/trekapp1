package com.example.trekapp1
//this is a kotlin/java file
/**BRIEF:
 * meant to handle most of the Health Connect API calls (and permissions?)
 * need to request permissions (make sure there's full run through with.without permissions set)
 * need to get information from healthConnect, we will run cumulative data locally via Database
 *
 * */
import android.annotation.SuppressLint
import android.content.Context
import android.health.connect.datatypes.Device
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.units.Energy
import java.time.LocalDate
import java.time.ZoneId
import androidx.health.platform.client.exerciseroute.ExerciseRoute
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.units.Length


class HealthConnectManager(private val context: Context) {
    /**Making the HealthConnect Client*/
    private val healthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(ExerciseSessionRecord::class)
    )

    private fun todayTimeRange(): TimeRangeFilter {
        val now = Instant.now()
        val zoneId = ZoneId.systemDefault()
        val startOfDay = LocalDate
            .now(zoneId)
            .atStartOfDay(zoneId)
            .toInstant()

        return TimeRangeFilter.between(startOfDay, now)
    }//reading today's time range, between midnight to now

    /**read today's steps and send to database to be reflected to UI*/
    suspend fun readTodaySteps(): Long {
        val timeRange = todayTimeRange()
        val response = healthConnectClient.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = timeRange
            )
        ) //could be null if there’s no data for today
        return response[StepsRecord.COUNT_TOTAL] ?: 0L
    }//reading today's steps total

    /**read today's calories and send to database to be reflected on UI*/
    suspend fun readTodayCalories(): Double {
        val timeRange = todayTimeRange()
        val response = healthConnectClient.aggregate(
            AggregateRequest(
                metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                timeRangeFilter = timeRange
            )
        )
        val energy: Energy? = response[TotalCaloriesBurnedRecord.ENERGY_TOTAL]
        // inKilocalories = “food calories” / kcal
        return energy?.inKilocalories ?: 0.0
    }

    /**
     * need exercise session for local data source instead of outside app
     */
    @SuppressLint("RestrictedApi")
    suspend fun writeExerciseSession(start: Instant, end: Instant, steps: Long, calories: Double) {
        val zoneOffsetStart = ZoneId.systemDefault().rules.getOffset(start)
        val zoneOffsetEnd = ZoneId.systemDefault().rules.getOffset(end)
        val sessionRecord = ExerciseSessionRecord(
            startTime = start,
            startZoneOffset = zoneOffsetStart,
            endTime = end,
            endZoneOffset = zoneOffsetEnd,
            metadata = Metadata.manualEntry(),
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
            title = "Trek Session"
        )
        val stepsRecord = StepsRecord(
            count = steps,
            startTime = start,
            startZoneOffset = zoneOffsetStart,
            endTime = end,
            endZoneOffset = zoneOffsetEnd,
            metadata = Metadata.manualEntry()
        )
        val caloriesRecord = TotalCaloriesBurnedRecord(
            energy = Energy.kilocalories(calories),
            startTime = start,
            startZoneOffset = zoneOffsetStart,
            endTime = end,
            endZoneOffset = zoneOffsetEnd,
            metadata = Metadata.manualEntry()
        )
        healthConnectClient.insertRecords(listOf(sessionRecord, stepsRecord, caloriesRecord))
    }
    suspend fun readTodayDistance(): Double {
        val timeRange = todayTimeRange()
        val response = healthConnectClient.aggregate(
            AggregateRequest(
                metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                timeRangeFilter = timeRange
            )
        )

        val distance: Length? = response[DistanceRecord.DISTANCE_TOTAL]
        return distance?.inMiles ?: 0.0
    }
}
