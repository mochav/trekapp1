package com.example.trekapp1.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.trekapp1.localDatabase.LocalDailyData
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(day: LocalDailyData)

    @Query("SELECT * FROM daily_data WHERE uid = :uid AND date = :date LIMIT 1")
    fun getDailyFlow(uid: String, date: String): Flow<LocalDailyData?>

    @Query("SELECT * FROM daily_data WHERE uid = :uid ORDER BY date DESC")
    fun getDailyListFlow(uid: String): Flow<List<LocalDailyData>>

    @Query("DELETE FROM daily_data WHERE uid = :uid")
    suspend fun deleteAllForUser(uid: String)
}
