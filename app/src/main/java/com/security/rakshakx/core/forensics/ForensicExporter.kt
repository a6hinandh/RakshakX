package com.security.rakshakx.core.forensics

import android.content.Context
import android.os.Build
import android.provider.Settings
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

data class ForensicBundle(
    val bundleId: String,
    val exportedAt: Long,
    val deviceFingerprint: String,
    val appVersion: String,
    val threatCount: Int,
    val integrityHash: String,
    val threats: List<Map<String, String>>,
    val stixJson: String
)

object ForensicExporter {

    private const val FORENSICS_DIR = "rakshakx_forensics"
    private val RFC3339_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    suspend fun export(context: Context, threats: List<Any>): ForensicBundle {
        val bundleId = UUID.randomUUID().toString()
        val appVersion = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        val deviceFingerprint = sha256Hex("$androidId$appVersion")

        val threatMaps = threats.map { threat ->
            when (threat) {
                is Map<*, *> -> threat.entries
                    .filter { it.key is String && it.value is String }
                    .associate { (k, v) -> (k as String) to (v as String) }
                else -> mapOf(
                    "type" to threat.javaClass.simpleName,
                    "data" to threat.toString(),
                    "timestamp" to System.currentTimeMillis().toString()
                )
            }
        }

        val allDataString = threatMaps.joinToString("") { it.values.joinToString("") }
        val integrityHash = computeIntegrityHash(allDataString)
        val stixJson = generateStix(threats, bundleId)

        return ForensicBundle(
            bundleId = bundleId,
            exportedAt = System.currentTimeMillis(),
            deviceFingerprint = deviceFingerprint,
            appVersion = appVersion,
            threatCount = threats.size,
            integrityHash = integrityHash,
            threats = threatMaps,
            stixJson = stixJson
        )
    }

    fun generateStix(threats: List<Any>, bundleId: String): String {
        val now = RFC3339_FORMAT.format(Date())
        val objects = JSONArray()

        val threatActorId = "threat-actor--${UUID.randomUUID()}"
        val threatActor = JSONObject().apply {
            put("type", "threat-actor")
            put("spec_version", "2.1")
            put("id", threatActorId)
            put("created", now)
            put("modified", now)
            put("name", "Unknown Threat Actor")
            put("threat_actor_types", JSONArray().apply { put("criminal") })
            put("sophistication", "intermediate")
            put("resource_level", "individual")
        }
        objects.put(threatActor)

        val identityId = "identity--${UUID.randomUUID()}"
        val identity = JSONObject().apply {
            put("type", "identity")
            put("spec_version", "2.1")
            put("id", identityId)
            put("created", now)
            put("modified", now)
            put("name", "RakshakX Security Platform")
            put("identity_class", "system")
        }
        objects.put(identity)

        for ((index, threat) in threats.withIndex()) {
            val indicatorId = "indicator--${UUID.randomUUID()}"
            val threatData: Map<String, String> = when (threat) {
                is Map<*, *> -> threat.entries
                    .filter { it.key is String && it.value is String }
                    .associate { (k, v) -> (k as String) to (v as String) }
                else -> mapOf(
                    "type" to threat.javaClass.simpleName,
                    "raw" to threat.toString()
                )
            }

            val threatType = threatData["type"] ?: threatData["category"] ?: "unknown"
            val threatTitle = threatData["title"] ?: threatData["name"] ?: "Threat #${index + 1}"
            val threatTimestamp = threatData["timestamp"] ?: now
            val channel = threatData["channel"] ?: "UNKNOWN"
            val pattern = buildStixPattern(threatType, channel, threatData)

            val indicator = JSONObject().apply {
                put("type", "indicator")
                put("spec_version", "2.1")
                put("id", indicatorId)
                put("created", now)
                put("modified", now)
                put("name", threatTitle)
                put("description", "Detected threat: $threatType via $channel channel")
                put("indicator_types", JSONArray().apply {
                    put(mapThreatTypeToStixIndicatorType(threatType))
                })
                put("pattern", pattern)
                put("pattern_type", "stix")
                put("pattern_version", "2.1")
                put("valid_from", now)
                put("confidence", 70)
                val labels = JSONArray().apply {
                    put("malicious-activity")
                    put("attribution")
                }
                put("labels", labels)
                val extRefs = JSONArray().apply {
                    put(JSONObject().apply {
                        put("source_name", "RakshakX")
                        put("description", "Detected by RakshakX Security Platform")
                    })
                }
                put("external_references", extRefs)
            }
            objects.put(indicator)

            val relationship = JSONObject().apply {
                put("type", "relationship")
                put("spec_version", "2.1")
                put("id", "relationship--${UUID.randomUUID()}")
                put("created", now)
                put("modified", now)
                put("relationship_type", "indicates")
                put("source_ref", indicatorId)
                put("target_ref", threatActorId)
            }
            objects.put(relationship)
        }

        val reportId = "report--${UUID.randomUUID()}"
        val objectRefs = JSONArray()
        for (i in 0 until objects.length()) {
            objectRefs.put((objects.getJSONObject(i)).getString("id"))
        }
        val report = JSONObject().apply {
            put("type", "report")
            put("spec_version", "2.1")
            put("id", reportId)
            put("created", now)
            put("modified", now)
            put("name", "RakshakX Threat Intelligence Report")
            put("description", "Automated threat intelligence bundle generated by RakshakX Security Platform")
            put("report_types", JSONArray().apply { put("threat-report") })
            put("published", now)
            put("object_refs", objectRefs)
        }
        objects.put(report)

        val bundle = JSONObject().apply {
            put("type", "bundle")
            put("id", "bundle--$bundleId")
            put("spec_version", "2.1")
            put("objects", objects)
        }

        return bundle.toString(2)
    }

    fun saveToFile(context: Context, bundle: ForensicBundle): File {
        val dir = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), FORENSICS_DIR)
        if (!dir.exists()) dir.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(bundle.exportedAt))
        val file = File(dir, "rakshakx_bundle_$timestamp.json")
        val content = JSONObject().apply {
            put("bundleId", bundle.bundleId)
            put("exportedAt", RFC3339_FORMAT.format(Date(bundle.exportedAt)))
            put("deviceFingerprint", bundle.deviceFingerprint)
            put("appVersion", bundle.appVersion)
            put("threatCount", bundle.threatCount)
            put("integrityHash", bundle.integrityHash)
            val threatsArray = JSONArray()
            for (threat in bundle.threats) {
                val obj = JSONObject()
                for ((k, v) in threat) obj.put(k, v)
                threatsArray.put(obj)
            }
            put("threats", threatsArray)
            put("stix", JSONObject(bundle.stixJson))
        }
        file.writeText(content.toString(2))
        return file
    }

    fun computeIntegrityHash(data: String): String = sha256Hex(data)

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun buildStixPattern(
        threatType: String,
        channel: String,
        data: Map<String, String>
    ): String {
        val sender = data["sender"] ?: data["from"] ?: data["number"] ?: "unknown"
        val url = data["url"] ?: data["link"] ?: ""
        return when {
            url.isNotBlank() -> "[url:value = '${url.replace("'", "\\'")}']"
            sender != "unknown" -> "[network-traffic:src_ref.type = 'phone-number' AND network-traffic:src_ref.value = '${sender.replace("'", "\\'")}']"
            threatType.contains("SMS", ignoreCase = true) -> "[email-message:content_type = 'sms' AND email-message:subject LIKE '%phishing%']"
            threatType.contains("CALL", ignoreCase = true) -> "[network-traffic:dst_port = 5060]"
            else -> "[domain-name:value LIKE '%.${channel.lowercase()}.%']"
        }
    }

    private fun mapThreatTypeToStixIndicatorType(threatType: String): String = when {
        threatType.contains("PHISHING", ignoreCase = true) -> "malicious-activity"
        threatType.contains("MALWARE", ignoreCase = true) -> "malware-artifact"
        threatType.contains("FRAUD", ignoreCase = true) -> "malicious-activity"
        threatType.contains("EXFIL", ignoreCase = true) -> "compromised"
        threatType.contains("C2", ignoreCase = true) || threatType.contains("BEACON", ignoreCase = true) -> "compromised"
        else -> "malicious-activity"
    }
}
