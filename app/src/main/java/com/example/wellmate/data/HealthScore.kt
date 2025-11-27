package com.example.wellmate.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "health_scores")
data class HealthScore(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val userId: String,
    val timestamp: Long,
    val score: Float,               // FLOAT to store decimal model outputs
    val category: String,
    val reason: String,
    val confidence: Float,
    val recommendationJson: String
)
