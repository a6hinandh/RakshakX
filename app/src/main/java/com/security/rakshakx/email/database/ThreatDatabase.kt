package com.security.rakshakx.email.database

import android.content.Context

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ThreatEntity::class],
    version = 1
)

abstract class ThreatDatabase : RoomDatabase() {

    abstract fun threatDao(): ThreatDao

    companion object {

        @Volatile
        private var INSTANCE: ThreatDatabase? = null

        fun getDatabase(
            context: Context
        ): ThreatDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(

                    context.applicationContext,

                    ThreatDatabase::class.java,

                    "rakshakx_threat_database"

                ).build()

                INSTANCE = instance

                instance
            }
        }
    }
}