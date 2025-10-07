package com.example.wellmate.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity class representing a saved health prediction result.
 * Each row corresponds to one risk prediction (score + category + recommendation).
 */
@Entity(tableName = "health_scores")
data class HealthScore(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    // user info
    val userId: String,

    // timestamp when this record was generated
    val timestamp: Long,

    // numeric health risk score (0–100)
    val score: Int,

    // category (Low, Moderate, High, etc.)
    val category: String,

    // short reason or model info
    val reason: String,

    // how confident the model is (0–1)
    val confidence: Float,

    // a JSON string with diet + exercise recommendations
    val recommendationJson: String
)
