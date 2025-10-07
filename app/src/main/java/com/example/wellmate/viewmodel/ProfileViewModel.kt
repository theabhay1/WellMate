package com.example.wellmate.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wellmate.data.HealthRepository
import com.example.wellmate.data.HealthScore
import com.example.wellmate.model.Recommender
import com.example.wellmate.tflite.TflitePredictor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class ProfileViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = HealthRepository(app.applicationContext)
    private val predictor by lazy { TflitePredictor(app.applicationContext) }

    // Expose status so UI can show success/error
    private val _status = MutableStateFlow<String>("idle")
    val status: StateFlow<String> = _status

    // Expose last saved HealthScore (null initially)
    private val _lastSaved = MutableStateFlow<HealthScore?>(null)
    val lastSaved: StateFlow<HealthScore?> = _lastSaved

    suspend fun computeAndSaveSuspend(
        userId: String,
        sex: Int, age: Int, heightCm: Float, weightKg: Float,
        activity: Int, sleep: Float, dietScore: Int,
        hasDiabetes: Int, hasHtn: Int, smoker: Int,
        goal: String, dietType: String
    ) {
        // run in IO, propagate exceptions to caller if needed
        withContext(Dispatchers.IO) {
            try {
                Log.d("ProfileVM", "Starting computeAndSaveSuspend for user=$userId")
                _status.value = "running"

                val features = floatArrayOf(
                    age.toFloat(), heightCm, weightKg,
                    activity.toFloat(), sleep, dietScore.toFloat(),
                    hasDiabetes.toFloat(), hasHtn.toFloat(), smoker.toFloat()
                )

                // run inference on Default (or keep on IO); wrap to catch errors
                val risk = try {
                    withContext(Dispatchers.Default) { predictor.predictRisk(features) }
                } catch (e: Exception) {
                    Log.e("ProfileVM", "TFLite inference failed", e)
                    _status.value = "model_error: ${e.message}"
                    throw e
                }

                Log.d("ProfileVM", "Raw risk from model = $risk")

                val finalRisk = if (hasDiabetes == 1 || hasHtn == 1) maxOf(risk, 60f) else risk
                val category = when (finalRisk.roundToInt()) {
                    in 0..19 -> "Low"
                    in 20..39 -> "Moderate"
                    in 40..59 -> "Elevated"
                    in 60..79 -> "High"
                    else -> "Very High"
                }

                val bmr = Recommender.bmr(sex, weightKg, heightCm, age)
                val calories = Recommender.recommendedCalories(bmr, activity, goal, sex)
                val (protein, fat, carbs) = Recommender.macros(calories, weightKg, goal)
                val meals = Recommender.sampleMealsIndian(dietType, calories)
                val plan = Recommender.exercisePlan(goal, activity)

                val recJson =
                    """{"calories":$calories,"macros":{"protein":$protein,"fat":$fat,"carbs":$carbs},"meals":[${meals.joinToString(",") { "\"$it\"" }}],"plan":"$plan"}"""

                val hs = HealthScore(
                    userId = userId,
                    timestamp = System.currentTimeMillis(),
                    score = finalRisk.roundToInt(),
                    category = category,
                    reason = "Model + rules",
                    confidence = 0.8f,
                    recommendationJson = recJson
                )

                // insert into DB
                repo.insertHealthScore(hs)
                Log.d("ProfileVM", "Inserted HealthScore: $hs")

                // publish to UI
                _lastSaved.value = hs
                _status.value = "ok"

            } catch (e: Exception) {
                Log.e("ProfileVM", "computeAndSaveSuspend failed", e)
                _status.value = "error: ${e.message}"
                throw e
            }
        }
    }

    // existing method that returns Flow from DB
    fun getLatestForUserFlow(userId: String) = repo.getLatestForUser(userId)

    override fun onCleared() {
        super.onCleared()
        try { predictor.close() } catch (e: Exception) { Log.e("ProfileVM", "close predictor", e) }
    }
}
