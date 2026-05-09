package com.security.rakshakx.call.core.storage

import android.content.Context
import androidx.room.Room
import com.security.rakshakx.call.core.security.EncryptionManager
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

object DatabaseFactory {
    @Volatile
    private var instance: RakshakDatabase? = null

    fun getInstance(context: Context): RakshakDatabase {
        return instance ?: synchronized(this) {
            instance ?: createEncrypted(context.applicationContext).also { instance = it }
        }
    }

    fun createEncrypted(context: Context): RakshakDatabase {
        System.loadLibrary("sqlcipher")
        val factory = SupportOpenHelperFactory(EncryptionManager.getOrCreateDbPassphrase())
        return Room.databaseBuilder(context, RakshakDatabase::class.java, "rakshakx-secure.db")
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
    }
}


