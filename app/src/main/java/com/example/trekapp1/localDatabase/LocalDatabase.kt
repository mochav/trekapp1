package com.example.trekapp1.localDatabase

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.trekapp1.dao.AvatarDao
import com.example.trekapp1.dao.CoinsDao
import com.example.trekapp1.dao.DailyDataDao
import com.example.trekapp1.dao.UserDao
import com.example.trekapp1.dao.UserTotalsDao

@Database(
    entities = [LocalUser::class, LocalUserTotals::class, LocalDailyData::class, LocalAvatar::class, LocalCoinBalance::class],
    version = 1,
    exportSchema = false
)
abstract class LocalDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun userTotalsDao(): UserTotalsDao
    abstract fun dailyDataDao(): DailyDataDao
    abstract fun avatarDao(): AvatarDao
    abstract fun coinsDao(): CoinsDao

    companion object {
        @Volatile
        private var INSTANCE: LocalDatabase? = null

        fun getInstance(context: Context): LocalDatabase {
            return INSTANCE ?: synchronized(this) {
                val inst = Room.databaseBuilder(
                    context.applicationContext,
                    LocalDatabase::class.java,
                    "trek_local_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = inst
                inst
            }
        }
    }
}
