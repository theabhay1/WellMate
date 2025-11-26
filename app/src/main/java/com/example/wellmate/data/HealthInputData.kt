package com.example.wellmate.data

data class HealthInputData(
    val age: Int?,                 // required (we validate)
    val gender: Int,               // 1 male, 0 female
    val heightCm: Float?,          // required (we validate)
    val weightKg: Float?,          // required (we validate)
    val steps: Float?,             // optional
    val sleep: Float?,             // optional
    val calories: Float?,          // optional
    val heartRate: Float?,         // optional
    val systolic: Float?,          // optional
    val diastolic: Float?,         // optional
    val exerciseHours: Float?,     // optional
    val alcoholPerWeek: Float?,    // optional — numeric drinks/week
    val smoker: Int,               // 1/0
    val diabetes: Int,             // 1/0
    val heartDisease: Int          // 1/0
)
