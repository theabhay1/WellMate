package com.example.wellmate.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class HealthRepository(context: Context) {
    private val dao = AppDatabase.getInstance(context).healthScoreDao()

    suspend fun insertHealthScore(hs: HealthScore) = dao.insert(hs)

    fun getLatestForUser(userId: String): Flow<HealthScore?> = dao.getLatestForUser(userId)
}
