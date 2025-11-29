package com.example.trekapp1.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.trekapp1.localDatabase.LocalAvatar
import kotlinx.coroutines.flow.Flow

@Dao
interface AvatarDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(avatar: LocalAvatar)

    @Query("SELECT * FROM avatars WHERE locked = 1")
    fun getLockedFlow(): Flow<List<LocalAvatar>>

    @Query("SELECT * FROM avatars WHERE locked = 0")
    fun getUnlockedFlow(): Flow<List<LocalAvatar>>

    @Query("SELECT * FROM avatars WHERE fileName = :fileName LIMIT 1")
    fun getAvatarFlow(fileName: String): Flow<LocalAvatar?>

    @Query("DELETE FROM avatars")
    suspend fun clearAll()
}
