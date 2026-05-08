package com.security.rakshakx.web.storage

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.File

class EncryptedStorageManager(private val context: Context) {
    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    fun deleteLegacyLog(fileName: String) {
        val file = File(context.filesDir, fileName)
        if (file.exists()) {
            file.delete()
        }
    }

    @Synchronized
    fun appendJson(fileName: String, jsonLine: String) {
        val file = File(context.filesDir, fileName)
        val existing = if (file.exists()) {
            val encryptedFile = buildEncryptedFile(file)
            encryptedFile.openFileInput().use { it.readBytes() }
        } else {
            ByteArray(0)
        }

        if (file.exists()) {
            file.delete()
        }

        val encryptedFile = buildEncryptedFile(file)
        encryptedFile.openFileOutput().use { output ->
            if (existing.isNotEmpty()) {
                output.write(existing)
                if (existing.last() != '\n'.code.toByte()) {
                    output.write('\n'.code)
                }
            }
            output.write(jsonLine.toByteArray(Charsets.UTF_8))
            output.write('\n'.code)
        }
    }

    private fun buildEncryptedFile(file: File): EncryptedFile {
        return EncryptedFile.Builder(
            context,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
    }
}
