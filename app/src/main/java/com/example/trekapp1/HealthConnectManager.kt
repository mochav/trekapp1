package com.example.trekapp1
//this is a kotlin/java file
/**BRIEF:
 * meant to handle most of the Health Connect API calls (and permissions?)
 * need to request permissions (make sure there's full run through with.without permissions set)
 * need to get information from healthConnect, we will run cumulative data locally via Database
 *
 * */
import android.content.Context
import android.health.connect.datatypes.Device
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
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


class HealthConnectManager(private val context: Context) {
    private val healthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(ExerciseSessionRecord::class)
    )//added read helpers for permissions

    private fun todayTimeRange(): TimeRangeFilter {
        val now = Instant.now()
        val zoneId = ZoneId.systemDefault()
        val startOfDay = LocalDate
            .now(zoneId)
            .atStartOfDay(zoneId)
            .toInstant()

        return TimeRangeFilter.between(startOfDay, now)
    }//reading today's date

    suspend fun readTodaySteps(): Long {
        val timeRange = todayTimeRange()
        val response = healthConnectClient.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = timeRange
            )
        )
        //could be null if there’s no data for today
        return response[StepsRecord.COUNT_TOTAL] ?: 0L
    }

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

}
