package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.LockedPolicyDao
import com.example.data.local.entity.LockedPolicyEntity

@Database(
    entities = [LockedPolicyEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DeadlockDatabase : RoomDatabase() {
    abstract fun lockedPolicyDao(): LockedPolicyDao

    companion object {
        @Volatile
        private var INSTANCE: DeadlockDatabase? = null

        fun getInstance(context: Context): DeadlockDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DeadlockDatabase::class.java,
                    "deadlock_database.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
