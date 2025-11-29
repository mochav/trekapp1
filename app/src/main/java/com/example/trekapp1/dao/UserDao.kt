package com.example.trekapp1.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.trekapp1.localDatabase.LocalUser
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: LocalUser)

    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    fun getUserFlow(uid: String): Flow<LocalUser?>

    @Query("DELETE FROM users WHERE uid = :uid")
    suspend fun deleteUser(uid: String)
}
