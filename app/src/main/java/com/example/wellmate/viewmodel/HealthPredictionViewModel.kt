package com.example.wellmate.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wellmate.data.HealthScore
import com.example.wellmate.data.HealthRepository
import com.example.wellmate.tflite.TflitePredictor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HealthPredictionViewModel(app: Application) : AndroidViewModel(app) {

    private val predictor = TflitePredictor(app.applicationContext)
    private val repo = HealthRepository(app.applicationContext)

    val age = MutableStateFlow("23")
    val height = MutableStateFlow("175")
    val weight = MutableStateFlow("70")
    val steps = MutableStateFlow("6000")
    val sleep = MutableStateFlow("7") // Default to 7
    val calories = MutableStateFlow("2500")
    val heart = MutableStateFlow("72")
    val exercise = MutableStateFlow("6")
    val alcohol = MutableStateFlow("0")

    val systolic = MutableStateFlow("120")
    val diastolic = MutableStateFlow("80")

    val gender = MutableStateFlow("Male")
    val smoker = MutableStateFlow("No")
    val diabetic = MutableStateFlow("No")
    val heartDisease = MutableStateFlow("No")

    // Output
    private val _score = MutableStateFlow<Float?>(null)
    val score: StateFlow<Float?> = _score

    private val _status = MutableStateFlow("idle")
    val status: StateFlow<String> = _status

    private fun safeFloat(v: String, fallback: Float): Float = v.toFloatOrNull() ?: fallback
    private fun ynToBinary(v: String): Float = if (v == "Yes") 1f else 0f
    private fun genderToBinary(v: String): Float = if (v == "Male") 1f else 0f

    private fun validateInputs(): String? {
        // 1. Check for Empty Fields first
        if (age.value.isBlank()) return "Age is required"
        if (height.value.isBlank()) return "Height is required"
        if (weight.value.isBlank()) return "Weight is required"
        if (steps.value.isBlank()) return "Daily Steps is required"
        if (sleep.value.isBlank()) return "Sleep hours is required"
        if (calories.value.isBlank()) return "Calorie intake is required"
        if (heart.value.isBlank()) return "Heart Rate is required"
        if (exercise.value.isBlank()) return "Exercise hours is required"
        if (alcohol.value.isBlank()) return "Alcohol intake is required (enter 0 if none)"

        // 2. Check Ranges (Logic remains same, but strict now)
        val a = age.value.toFloatOrNull()
        if (a == null || a !in 18f..100f) return "Age must be between 18 and 100"

        val h = height.value.toFloatOrNull()
        if (h == null || h !in 50f..250f) return "Height must be valid (50-250 cm)"

        val w = weight.value.toFloatOrNull()
        if (w == null || w !in 20f..300f) return "Weight must be valid (20-300 kg)"

        val sl = sleep.value.toFloatOrNull()
        if (sl == null || sl !in 4f..12f) return "Sleep must be between 4 and 12 hours"

        val cal = calories.value.toFloatOrNull()
        if (cal == null || cal !in 500f..15000f) return "Calories seem unrealistic"

        return null // No error
    }

    fun predictAndSave(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Validate inputs first
                val error = validateInputs()
                if (error != null) {
                    _status.value = "error: $error"
                    return@launch
                }

                _status.value = "running"
                _score.value = null

                // 2. Prepare Data
                val hCm = safeFloat(height.value, 170f)
                val wKg = safeFloat(weight.value, 70f)

                // Calculate TFLite Inputs
                val inputMap = mapOf(
                    "Age" to safeFloat(age.value, 30f),
                    "Height_cm" to hCm,
                    "Weight_kg" to wKg,
                    "BMI" to calculateBMI(),
                    "Daily_Steps" to safeFloat(steps.value, 5000f),
                    "Calories_Intake" to safeFloat(calories.value, 2000f),
                    "Hours_of_Sleep" to safeFloat(sleep.value, 7f),
                    "Heart_Rate" to safeFloat(heart.value, 72f),
                    "Systolic_BP" to safeFloat(systolic.value, 120f),
                    "Diastolic_BP" to safeFloat(diastolic.value, 80f),
                    "Exercise_Hours_per_Week" to safeFloat(exercise.value, 2f),
                    "Alcohol_Consumption_per_Week" to safeFloat(alcohol.value, 0f),
                    "Gender" to genderToBinary(gender.value),
                    "Smoker" to ynToBinary(smoker.value),
                    "Diabetic" to ynToBinary(diabetic.value),
                    "Heart_Disease" to ynToBinary(heartDisease.value)
                )

                // 3. Get Raw Score from Model (0 - 100)
                val rawScore = predictor.predict(inputMap)

                // ---------------------------------------------------------
                // 4. THE "MAGICAL FORMULA" (Post-Processing Adjustment)
                // ---------------------------------------------------------

                // Step A: Calculate ICMR Ideal Weight (BMI 22)
                val heightM = hCm / 100f
                val idealWeight = 22f * heightM * heightM

                // Step B: Calculate Percentage Deviation
                // Example: Ideal 70kg, Actual 85kg -> Diff 15kg -> ~21% deviation
                val diff = kotlin.math.abs(wKg - idealWeight)
                val deviationPercentage = (diff / idealWeight) * 100f

                // Step C: Apply Soft Penalty
                // We allow a 10% "Healthy Buffer" (BMI approx 20-24). No penalty here.
                // For every 1% deviation ABOVE 10%, we subtract 0.5 score.
                val penalty = if (deviationPercentage > 10f) {
                    (deviationPercentage - 10f) * 0.5f
                } else {
                    0f
                }

                // Step D: Safety Limits
                // 1. Cap penalty at 20 points (don't punish too hard)
                // 2. Ensure final score never goes below 0 or above 100
                val cappedPenalty = penalty.coerceAtMost(20f)
                val finalScore = (rawScore - cappedPenalty).coerceIn(0f, 100f)

                Log.d("ADJUSTMENT", "Raw: $rawScore | Ideal: $idealWeight | Dev: $deviationPercentage% | Penalty: $cappedPenalty | Final: $finalScore")

                // ---------------------------------------------------------

                _score.value = finalScore
                _status.value = "ok"

                // 5. Save to Room
                val hs = HealthScore(
                    userId = userId,
                    timestamp = System.currentTimeMillis(),
                    score = finalScore,
                    category = "Model",
                    reason = "AI + BMI Adjustment",
                    confidence = 0.85f,
                    recommendationJson = "{}"
                )
                repo.insertHealthScore(hs)

            } catch (e: Exception) {
                _status.value = "error: ${e.message}"
                Log.e("PREDICT_ERROR", "ERROR", e)
            }
        }
    }

    private fun calculateBMI(): Float {
        val h = safeFloat(height.value, 0f)
        val w = safeFloat(weight.value, 0f)
        return if (h > 0f) w / ((h / 100f) * (h / 100f)) else 0f
    }

    fun resetState() {
        _status.value = "idle"
        _score.value = null
    }

    fun setStatus(s: String) { _status.value = s }
}