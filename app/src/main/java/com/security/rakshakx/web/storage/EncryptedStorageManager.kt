package com.security.rakshakx.web.storage

import android.content.Context
import java.io.File

class EncryptedStorageManager(private val context: Context) {
    fun deleteLegacyLog(fileName: String) {
        val file = File(context.filesDir, fileName)
        if (file.exists()) {
            file.delete()
        }
    }
}
