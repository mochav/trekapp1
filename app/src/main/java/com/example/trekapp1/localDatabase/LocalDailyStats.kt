package com.example.trekapp1.localDatabase

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stats")
data class LocalDailyStats(
    @PrimaryKey val date: String,
    val uid: String,
    val steps: Long,
    val distanceKm: Double,
    val calories: Double
)
