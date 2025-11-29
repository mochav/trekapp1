package com.example.trekapp1.dao

import androidx.room.*
import com.example.trekapp1.localDatabase.LocalActivity

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities ORDER BY id DESC")
    suspend fun getAllActivities(): List<LocalActivity>

    @Query("SELECT * FROM activities WHERE id = :id")
    suspend fun getActivityById(id: String): LocalActivity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: LocalActivity)

    @Delete
    suspend fun deleteActivity(activity: LocalActivity)

    @Update
    suspend fun updateActivity(activity: LocalActivity)

    @Query("DELETE FROM activities")
    suspend fun deleteAllActivities()
}