package com.example.wellmate.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wellmate.data.HealthInputData
import com.example.wellmate.data.HealthScore
import com.example.wellmate.data.HealthRepository
import com.example.wellmate.tflite.TflitePredictor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log

class HealthPredictionViewModel(app: Application) : AndroidViewModel(app) {

    private val predictor = TflitePredictor(app.applicationContext)
    private val repo = HealthRepository(app.applicationContext)

    // ---------------------------
    // FORM INPUT STATE (SURVIVES BACK PRESS)
    // ---------------------------
    val age = MutableStateFlow("")
    val height = MutableStateFlow("")
    val weight = MutableStateFlow("")
    val steps = MutableStateFlow("")
    val sleep = MutableStateFlow("")
    val calories = MutableStateFlow("")
    val heart = MutableStateFlow("")
    val exercise = MutableStateFlow("")
    val alcohol = MutableStateFlow("")

    val systolic = MutableStateFlow("110–130")
    val diastolic = MutableStateFlow("75–90")

    val gender = MutableStateFlow("Male")
    val smoker = MutableStateFlow("No")
    val diabetic = MutableStateFlow("No")
    val heartDisease = MutableStateFlow("No")

    // ---------------------------
    // MODEL OUTPUT STATE
    // ---------------------------
    private val _score = MutableStateFlow<Float?>(null)
    val score: StateFlow<Float?> = _score

    private val _status = MutableStateFlow("idle")
    val status: StateFlow<String> = _status

    // means for fallback
    private val DEFAULT_MEANS = mapOf(
        "Age" to 49.86875f,
        "Height_cm" to 174.50875f,
        "Weight_kg" to 84.27625f,
        "BMI" to 26.6795125f,
        "Daily_Steps" to 10763.7f,
        "Calories_Intake" to 2345.165f,
        "Hours_of_Sleep" to 6.956f,
        "Heart_Rate" to 85.14f,
        "Systolic_BP" to 113.77f,
        "Diastolic_BP" to 74.83875f,
        "Exercise_Hours_per_Week" to 5.036875f,
        "Alcohol_Consumption_per_Week" to 4.54f
    )

    private fun bpMid(range: String): Float =
        when (range) {
            "90–110" -> 100f
            "110–130" -> 120f
            "130–150" -> 140f
            "150–170" -> 160f
            "170+" -> 180f
            "60–75" -> 67f
            "75–90" -> 82f
            "90–105" -> 97f
            "105–120" -> 112f
            "120+" -> 130f
            else -> 110f
        }

    fun predictAndSave(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Required fields
                if (age.value.isBlank() || height.value.isBlank() || weight.value.isBlank()) {
                    _status.value = "error: missing_required_fields"
                    return@launch
                }

                _status.value = "running"

                val ageVal = age.value.toInt()
                val heightVal = height.value.toFloat()
                val weightVal = weight.value.toFloat()

                val bmi = weightVal / ((heightVal / 100f) * (heightVal / 100f))

                fun fallback(key: String, v: String): Float =
                    v.toFloatOrNull() ?: DEFAULT_MEANS[key]!!

                val map = mapOf(
                    "Age" to ageVal.toFloat(),
                    "Height_cm" to heightVal,
                    "Weight_kg" to weightVal,
                    "BMI" to bmi,
                    "Daily_Steps" to fallback("Daily_Steps", steps.value),
                    "Calories_Intake" to fallback("Calories_Intake", calories.value),
                    "Hours_of_Sleep" to fallback("Hours_of_Sleep", sleep.value),
                    "Heart_Rate" to fallback("Heart_Rate", heart.value),
                    "Systolic_BP" to bpMid(systolic.value),
                    "Diastolic_BP" to bpMid(diastolic.value),
                    "Exercise_Hours_per_Week" to fallback("Exercise_Hours_per_Week", exercise.value),
                    "Alcohol_Consumption_per_Week" to fallback("Alcohol_Consumption_per_Week", alcohol.value),
                    "Gender" to if (gender.value == "Male") 1f else 0f,
                    "Smoker" to if (smoker.value == "Yes") 1f else 0f,
                    "Diabetic" to if (diabetic.value == "Yes") 1f else 0f,
                    "Heart_Disease" to if (heartDisease.value == "Yes") 1f else 0f,
                )

                val result = predictor.predict(map)
                _score.value = result
                _status.value = "ok"

                // SAVE TO ROOM DB
                val hs = HealthScore(
                    userId = userId,
                    timestamp = System.currentTimeMillis(),
                    score = result.toInt(),
                    category = "Model",
                    reason = "ML model",
                    confidence = 0.85f,
                    recommendationJson = "{}"
                )
                repo.insertHealthScore(hs)

            } catch (e: Exception) {
                _status.value = "error: ${e.message}"
            }
        }
    }

    fun setStatus(s: String) { _status.value = s }
}
