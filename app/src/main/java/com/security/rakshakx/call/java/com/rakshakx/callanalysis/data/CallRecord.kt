package com.rakshakx.callanalysis.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * CallRecord
 *
 * Data model representing a single analyzed call.
 * Stored in Room database for history and analytics.
 */
@Entity(tableName = "call_records")
data class CallRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val phoneNumber: String,
    val transcript: String,
    val riskScore: Float,
    val action: String, // BLOCK, WARN, ALLOW
    val reason: String,
    val timestamp: Long
)