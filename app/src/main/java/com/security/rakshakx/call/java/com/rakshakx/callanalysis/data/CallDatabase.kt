package com.rakshakx.callanalysis.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room DB for RakshakX call analysis records (separate from main RakshakDatabase).
 */
@Database(entities = [CallRecord::class], version = 1, exportSchema = false)
abstract class CallDatabase : RoomDatabase() {
    abstract fun callDao(): CallDao

    companion object {
        @Volatile
        private var INSTANCE: CallDatabase? = null

        fun getInstance(context: Context): CallDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CallDatabase::class.java,
                    "call_analysis.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
