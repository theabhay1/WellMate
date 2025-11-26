package com.example.wellmate.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "health_scores")
data class HealthScore(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val userId: String,

    val timestamp: Long,

    val score: Int,

    val category: String,

    val reason: String,

    val confidence: Float,

    val recommendationJson: String
)
