package com.example.trekapp1.localDatabase

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_totals")
data class LocalUserTotals(
    @PrimaryKey val uid: String,
    val steps: Long = 0,
    val miles: Double = 0.0,
    val calories: Long = 0,
    val updatedAt: Long = 0L // epoch seconds
)
