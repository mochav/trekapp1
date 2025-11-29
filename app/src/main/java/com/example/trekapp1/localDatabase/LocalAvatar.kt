package com.example.trekapp1.localDatabase

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "avatars")
data class LocalAvatar(
    @PrimaryKey val fileName: String, // use filename as id
    val locked: Boolean = true
)
