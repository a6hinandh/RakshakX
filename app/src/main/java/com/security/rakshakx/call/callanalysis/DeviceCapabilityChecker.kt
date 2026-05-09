package com.security.rakshakx.call.callanalysis

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log

/**
 * DeviceCapabilityChecker
 *
 * Detects device-specific limitations that affect call recording.
 * Many OEMs (Samsung, Xiaomi, OnePlus) block VOICE_CALL audio source.
 */
class DeviceCapabilityChecker(private val context: Context) {

    data class DeviceCapabilities(
        val canRecordCalls: Boolean,
        val recommendedAudioSource: Int,
        val limitations: List<String>,
        val workarounds: List<String>
    )

    /**
     * Check what the device supports for call recording.
     */
    fun checkCapabilities(): DeviceCapabilities {
        val limitations = mutableListOf<String>()
        val workarounds = mutableListOf<String>()
        var canRecordCalls = true
        var recommendedSource = MediaRecorder.AudioSource.VOICE_CALL

        // Check Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            limitations.add("Android 10+ restricts call recording")
            workarounds.add("Use speakerphone during calls")
        }

        // Check OEM restrictions
        val manufacturer = Build.MANUFACTURER.lowercase()
        when {
            manufacturer.contains("samsung") -> {
                limitations.add("Samsung devices may block VOICE_CALL source")
                recommendedSource = MediaRecorder.AudioSource.MIC
                workarounds.add("Enable speakerphone before analyzing")
            }
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> {
                limitations.add("Xiaomi/Redmi may require MIUI permissions")
                workarounds.add("Enable 'Record audio' in MIUI Security settings")
                workarounds.add("Use speakerphone mode")
            }
            manufacturer.contains("oneplus") -> {
                limitations.add("OnePlus devices often block call recording")
                recommendedSource = MediaRecorder.AudioSource.MIC
                workarounds.add("Use speakerphone + MIC source")
            }
            manufacturer.contains("google") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                canRecordCalls = false
                limitations.add("Google Pixel blocks call recording on Android 10+")
                workarounds.add("No workaround available - SMS analysis only")
            }
        }

        // Test if recording is actually possible
        val testResult = testRecordingCapability()
        if (!testResult) {
            canRecordCalls = false
            limitations.add("Call recording blocked by system")
            workarounds.add("Switch to SMS/web link analysis mode")
        }

        return DeviceCapabilities(
            canRecordCalls = canRecordCalls,
            recommendedAudioSource = recommendedSource,
            limitations = limitations,
            workarounds = workarounds
        )
    }

    /**
     * Attempt a quick test recording to verify capability.
     */
    private fun testRecordingCapability(): Boolean {
        return try {
            val recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.VOICE_CALL)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile("${context.cacheDir}/test.3gp")
            }

            recorder.prepare()
            recorder.release()

            // Clean up test file
            context.cacheDir.resolve("test.3gp").delete()

            true
        } catch (e: Exception) {
            Log.w("DeviceCapabilityChecker", "Call recording test failed: ${e.message}")
            false
        }
    }

    /**
     * Get user-friendly capability report.
     */
    fun getCapabilityReport(): String {
        val caps = checkCapabilities()

        return buildString {
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine()
            appendLine("Call Recording: ${if (caps.canRecordCalls) "✓ Supported" else "✗ Blocked"}")

            if (caps.limitations.isNotEmpty()) {
                appendLine()
                appendLine("Limitations:")
                caps.limitations.forEach { appendLine("  • $it") }
            }

            if (caps.workarounds.isNotEmpty()) {
                appendLine()
                appendLine("Recommended:")
                caps.workarounds.forEach { appendLine("  • $it") }
            }
        }
    }
}

