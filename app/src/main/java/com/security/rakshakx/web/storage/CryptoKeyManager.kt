package com.security.rakshakx.web.storage

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

object CryptoKeyManager {
    private const val PREFS_NAME = "rakshakx_secure_keys"
    private const val DB_PASSPHRASE_KEY = "db_passphrase"

    fun getMasterKey(context: Context): MasterKey {
        return MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    fun getOrCreateDatabasePassphrase(context: Context): ByteArray {
        val masterKey = getMasterKey(context)
        val prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val existing = prefs.getString(DB_PASSPHRASE_KEY, null)
        if (!existing.isNullOrBlank()) {
            return Base64.decode(existing, Base64.DEFAULT)
        }

        val random = ByteArray(32)
        SecureRandom().nextBytes(random)
        prefs.edit().putString(DB_PASSPHRASE_KEY, Base64.encodeToString(random, Base64.NO_WRAP)).apply()
        return random
    }
}
