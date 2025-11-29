package com.example.trekapp1.localDatabase

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "coin_balance")
data class LocalCoinBalance(
    @PrimaryKey val uid: String,
    val coins: Long = 0
)
