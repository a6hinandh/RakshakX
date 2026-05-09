package com.rakshakx.core.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "risk_scores")
data class RiskScoreEntity(
    @PrimaryKey val phoneNumber: String,
    val lastEventType: String,
    val lastMessageSnippet: String?,
    val riskScore: Float,
    val updatedAt: Long
)
