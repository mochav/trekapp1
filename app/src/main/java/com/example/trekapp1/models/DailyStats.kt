package com.example.trekapp1.models

data class DailyStats(
    val date: String,
    val steps: Long,
    val miles: Double,
    val calories: Long
)
