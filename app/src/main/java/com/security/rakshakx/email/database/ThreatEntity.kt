package com.security.rakshakx.email.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "threats")

data class ThreatEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,

    val message: String,

    val riskScore: Int,

    val riskLevel: String,

    val timestamp: Long
)