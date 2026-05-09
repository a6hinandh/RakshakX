package com.rakshakx.core.storage

import android.content.Context
import androidx.room.Room
import com.rakshakx.core.security.EncryptionManager
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

object DatabaseFactory {
    @Volatile
    private var instance: RakshakDatabase? = null

    fun getInstance(context: Context): RakshakDatabase {
        return instance ?: synchronized(this) {
            instance ?: createEncrypted(context.applicationContext).also { instance = it }
        }
    }

    fun createEncrypted(context: Context): RakshakDatabase {
        SQLiteDatabase.loadLibs(context)
        val factory = SupportFactory(EncryptionManager.getOrCreateDbPassphrase())
        return Room.databaseBuilder(context, RakshakDatabase::class.java, "rakshakx-secure.db")
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
    }
}
