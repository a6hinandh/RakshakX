package com.rakshakx.callanalysis

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class DemoAudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    fun playScenarioAudio(rawResId: Int) {
        stopAudio()
        try {
            mediaPlayer = MediaPlayer.create(context, rawResId).apply {
                setOnCompletionListener { 
                    it.release()
                    mediaPlayer = null
                }
                start()
            }
        } catch (e: Exception) {
            Log.e("DemoAudioPlayer", "Failed to play audio", e)
        }
    }

    fun stopAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
    }
}

suspend fun copyRawToTempPath(context: Context, rawResId: Int): String? = withContext(Dispatchers.IO) {
    try {
        val inputStream = context.resources.openRawResource(rawResId)
        val tempFile = File(context.cacheDir, "demo_scenario_${rawResId}.wav")
        
        val outputStream = FileOutputStream(tempFile)
        val buffer = ByteArray(1024)
        var length: Int
        while (inputStream.read(buffer).also { length = it } > 0) {
            outputStream.write(buffer, 0, length)
        }
        
        outputStream.flush()
        outputStream.close()
        inputStream.close()
        
        tempFile.absolutePath
    } catch (e: Exception) {
        Log.e("DemoAudioUtils", "Failed to copy raw resource to temp path", e)
        null
    }
}
