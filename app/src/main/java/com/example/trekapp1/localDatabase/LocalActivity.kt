package com.example.trekapp1.localDatabase

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activities")
data class LocalActivity(
    @PrimaryKey
    val id: String,
    val date: String,
    val distance: String,
    val duration: String,
    val pace: String
)