package com.example.wellmate.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthScoreDao {
    @Insert
    suspend fun insert(score: HealthScore)

    @Query("SELECT * FROM health_scores WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestForUser(userId: String): Flow<HealthScore?>
}
