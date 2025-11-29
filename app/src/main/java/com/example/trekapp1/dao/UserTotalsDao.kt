package com.example.trekapp1.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.trekapp1.localDatabase.LocalUserTotals
import kotlinx.coroutines.flow.Flow

@Dao
interface UserTotalsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(totals: LocalUserTotals)

    @Query("SELECT * FROM user_totals WHERE uid = :uid LIMIT 1")
    fun getTotalsFlow(uid: String): Flow<LocalUserTotals?>

    @Query("DELETE FROM user_totals WHERE uid = :uid")
    suspend fun deleteTotals(uid: String)
}
