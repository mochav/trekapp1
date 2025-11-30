package com.example.trekapp1.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.trekapp1.localDatabase.LocalDailyStats
import com.example.trekapp1.models.DailyStats
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(entity: DailyStats)

    @Query("SELECT * FROM daily_stats WHERE date = :date LIMIT 1")
    fun getStats(date: String): Flow<LocalDailyStats?>
}
