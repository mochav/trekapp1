package com.example.trekapp1.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.trekapp1.localDatabase.LocalCoinBalance
import kotlinx.coroutines.flow.Flow

@Dao
interface CoinsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(balance: LocalCoinBalance)

    @Query("SELECT * FROM coin_balance WHERE uid = :uid LIMIT 1")
    fun getCoinsFlow(uid: String): Flow<LocalCoinBalance?>

    @Query("DELETE FROM coin_balance WHERE uid = :uid")
    suspend fun deleteForUser(uid: String)
}
