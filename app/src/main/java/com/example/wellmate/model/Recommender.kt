package com.example.wellmate.model

/**
 * Small utility object that contains BMR, calorie, macro and sample meal helpers.
 * These are intentionally simple rule-of-thumb formulas for demo / prototype use.
 *
 * Place under: com.example.wellmate.model
 */

object Recommender {

    // Harris-Benedict approx BMR (sex: 0 = male, 1 = female)
    fun bmr(sex: Int, weightKg: Float, heightCm: Float, age: Int): Float {
        return if (sex == 0) {
            // male
            88.362f + (13.397f * weightKg) + (4.799f * heightCm) - (5.677f * age)
        } else {
            // female
            447.593f + (9.247f * weightKg) + (3.098f * heightCm) - (4.330f * age)
        }
    }

    // activity: 0..4 (sedentary -> extra active)
    private fun activityMultiplier(activity: Int): Float {
        return when (activity.coerceIn(0, 4)) {
            0 -> 1.2f
            1 -> 1.375f
            2 -> 1.55f
            3 -> 1.725f
            else -> 1.9f
        }
    }

    // goal: "lose", "gain", "maintain"
    fun recommendedCalories(bmr: Float, activity: Int, goal: String, sex: Int): Int {
        val maintenance = bmr * activityMultiplier(activity)
        return when (goal.lowercase()) {
            "lose" -> (maintenance - 500).toInt().coerceAtLeast(1200)
            "gain" -> (maintenance + 300).toInt()
            else -> maintenance.toInt()
        }
    }

    // returns protein(g/day), fat(g/day), carbs(g/day)
    fun macros(totalCalories: Int, weightKg: Float, goal: String): Triple<Int, Int, Int> {
        // simple rules:
        val proteinPerKg = when (goal.lowercase()) {
            "gain" -> 2.0f
            "lose" -> 1.8f
            else -> 1.6f
        }
        val proteinCalories = (proteinPerKg * weightKg * 4f).toInt()
        val fatCalories = (0.25f * totalCalories).toInt() // 25% from fat
        val carbsCalories = totalCalories - proteinCalories - fatCalories
        val proteinG = (proteinCalories / 4f).toInt()
        val fatG = (fatCalories / 9f).toInt()
        val carbsG = (carbsCalories / 4f).coerceAtLeast(0f).toInt()
        return Triple(proteinG, fatG, carbsG)
    }

    // Simple sample meals tailored to Indian diet preference (vegetarian/non_vegetarian)
    fun sampleMealsIndian(dietType: String, calories: Int): List<String> {
        val d = dietType.lowercase().trim()
        val meals = mutableListOf<String>()
        if (d.contains("veg") || d == "vegetarian") {
            meals.add("Breakfast: Dalia / Poha + low-fat milk (approx 350 kcal)")
            meals.add("Mid-morning: Fruit (banana / apple)")
            meals.add("Lunch: 1.5 rotis + dal + sabzi + salad")
            meals.add("Evening: Buttermilk or tea + roasted chana")
            meals.add("Dinner: Paneer curry or soya chunk curry + 2 rotis")
        } else {
            meals.add("Breakfast: Omelette (2 eggs) + 1 slice whole wheat toast")
            meals.add("Mid-morning: Fruit (banana / orange)")
            meals.add("Lunch: Rice + chicken curry + salad")
            meals.add("Evening: Whey shake or roasted peanuts")
            meals.add("Dinner: Grilled chicken or egg curry + 1-2 rotis")
        }
        meals.add("Adjust portions to target calories: ~$calories kcal/day")
        return meals
    }

    // Very simple exercise plan suggestions
    fun exercisePlan(goal: String, activity: Int): String {
        val base = when (activity) {
            0 -> "Start with 15-20 min walking, 3x/week"
            1 -> "30 min brisk walk or light cardio 3-4x/week"
            2 -> "40-60 min mixed cardio + strength 4x/week"
            3 -> "45-75 min intensive training 4-6x/week"
            else -> "Daily physical activity: cardio + strength"
        }
        val goalAdvice = when (goal.lowercase()) {
            "lose" -> "Focus on calorie deficit + cardio + resistance training"
            "gain" -> "Slight calorie surplus + progressive overload strength training"
            else -> "Maintain weight with balanced diet and mixed training"
        }
        return "$base. Goal advice: $goalAdvice."
    }
}
