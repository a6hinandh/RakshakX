package com.security.rakshakx.core.vault

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class VaultEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val category: VaultCategory,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class VaultCategory {
    PASSWORD, NOTE, RECOVERY_CODE, API_KEY, CREDIT_CARD, OTHER
}

object SecureVault {

    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "rakshakx_vault_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val PREFS_FILE = "rakshakx_vault_store"

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        }
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun encrypt(plaintext: String): String {
        val secretKey = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(ciphertext: String): String {
        val secretKey = getOrCreateKey()
        val combined = Base64.decode(ciphertext, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, 12)
        val encrypted = combined.copyOfRange(12, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val decrypted = cipher.doFinal(encrypted)
        return String(decrypted, Charsets.UTF_8)
    }

    private fun getEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveEntry(context: Context, entry: VaultEntry): Boolean {
        return try {
            val encryptedContent = encrypt(entry.content)
            val json = JSONObject().apply {
                put("id", entry.id)
                put("title", entry.title)
                put("content", encryptedContent)
                put("category", entry.category.name)
                put("createdAt", entry.createdAt)
                put("updatedAt", entry.updatedAt)
            }
            getEncryptedPrefs(context).edit()
                .putString(entry.id, json.toString())
                .apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getEntry(context: Context, id: String): VaultEntry? {
        return try {
            val json = getEncryptedPrefs(context).getString(id, null) ?: return null
            parseEntry(json)
        } catch (e: Exception) {
            null
        }
    }

    fun getAllEntries(context: Context): List<VaultEntry> {
        return try {
            val prefs = getEncryptedPrefs(context)
            val entries = mutableListOf<VaultEntry>()
            for ((_, value) in prefs.all) {
                val entry = parseEntry(value.toString()) ?: continue
                entries.add(entry)
            }
            entries.sortedByDescending { it.updatedAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun deleteEntry(context: Context, id: String): Boolean {
        return try {
            getEncryptedPrefs(context).edit().remove(id).apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun deleteAll(context: Context) {
        try {
            getEncryptedPrefs(context).edit().clear().apply()
        } catch (e: Exception) {
            // silently handle
        }
    }

    private fun parseEntry(json: String): VaultEntry? {
        return try {
            val obj = JSONObject(json)
            val encryptedContent = obj.getString("content")
            val decryptedContent = try {
                decrypt(encryptedContent)
            } catch (e: Exception) {
                "[Decryption failed]"
            }
            VaultEntry(
                id = obj.getString("id"),
                title = obj.getString("title"),
                content = decryptedContent,
                category = runCatching {
                    VaultCategory.valueOf(obj.getString("category"))
                }.getOrDefault(VaultCategory.OTHER),
                createdAt = obj.getLong("createdAt"),
                updatedAt = obj.getLong("updatedAt")
            )
        } catch (e: Exception) {
            null
        }
    }
}
