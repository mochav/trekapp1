package com.example.trekapp1.localDatabase

import androidx.room.Entity
import androidx.room.PrimaryKey

// Use composite key simulated by combining uid + date as id
@Entity(tableName = "daily_data")
data class LocalDailyData(
    @PrimaryKey val id: String, // "${uid}_$date"
    val uid: String,
    val date: String, // yyyy-MM-dd
    val steps: Long = 0,
    val miles: Double = 0.0,
    val calories: Long = 0
)
