package com.example.wellmate.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.wellmate.viewmodel.HealthPredictionViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewResultScreen(
    viewModel: HealthPredictionViewModel,
    onBack: () -> Unit
) {
    val score by viewModel.score.collectAsState()

    val age by viewModel.age.collectAsState()
    val height by viewModel.height.collectAsState()
    val weight by viewModel.weight.collectAsState()
    val steps by viewModel.steps.collectAsState()
    val sleep by viewModel.sleep.collectAsState()
    val exercise by viewModel.exercise.collectAsState()
    val calories by viewModel.calories.collectAsState()
    val systolic by viewModel.systolic.collectAsState()
    val diastolic by viewModel.diastolic.collectAsState()
    val heart by viewModel.heart.collectAsState()
    val gender by viewModel.gender.collectAsState()

    val scoreInt = score?.roundToInt() ?: 0

    // ---------------- SCORE ANIMATION ----------------
    val animatedScore by animateIntAsState(
        targetValue = scoreInt,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
    )

    val scoreColor by animateColorAsState(
        targetValue = when (scoreInt) {
            in 0..25 -> Color(0xFFE53935)  // Danger Red
            in 26..50 -> Color(0xFFFFA726) // Orange
            in 51..75 -> Color(0xFFFFEB3B) // Yellow
            else -> Color(0xFF43A047)      // Green
        },
        animationSpec = tween(900)
    )

    // ---------------- COMPUTE BMI ----------------
    val bmi = if (height.isNotBlank() && weight.isNotBlank()) {
        val h = height.toFloat() / 100f
        weight.toFloat() / (h * h)
    } else 0f

    // ---------------- COMPUTE MAINTENANCE CALORIES ----------------
    val bmr = remember(age, height, weight) {
        val w = weight.toFloatOrNull() ?: 60f
        val h = height.toFloatOrNull() ?: 170f
        val a = age.toIntOrNull() ?: 25

        if (gender == "Male") {
            10 * w + 6.25f * h - 5 * a + 5
        } else {
            10 * w + 6.25f * h - 5 * a - 161
        }
    }

    val actFactor = when {
        steps.toFloatOrNull() ?: 0f < 3000 -> 1.2f
        steps.toFloatOrNull() ?: 0f < 6000 -> 1.375f
        steps.toFloatOrNull() ?: 0f < 9000 -> 1.55f
        steps.toFloatOrNull() ?: 0f < 13000 -> 1.725f
        else -> 1.9f
    }

    val maintenanceCalories = (bmr * actFactor).roundToInt()

    val category = when (scoreInt) {
        in 0..25 -> "High Risk"
        in 26..50 -> "Moderate Risk"
        in 51..75 -> "Good"
        else -> "Excellent"
    }

    val recs = generateIndianRecommendations(
        bmi = bmi,
        steps = steps.toFloatOrNull() ?: 6000f,
        sleep = sleep.toFloatOrNull() ?: 7f,
        exercise = exercise.toFloatOrNull() ?: 2f,
        systolic = systolic,
        diastolic = diastolic,
        heartRate = heart.toFloatOrNull() ?: 80f
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Health Score") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            ScoreCardAnimated(animatedScore, category, scoreColor)

            VitalsCardEnhanced(
                bmi = bmi,
                sys = systolic,
                dia = diastolic,
                heart = heart.toFloatOrNull() ?: 80f,
                maintenanceCalories = maintenanceCalories,
                userCalories = calories
            )

            RecommendationCard(recs)
        }
    }
}

// -------------------------------------------------------------------
// ANIMATED SCORE CARD
// -------------------------------------------------------------------

@Composable
fun ScoreCardAnimated(score: Int, category: String, scoreColor: Color) {

    Card(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = scoreColor.copy(alpha = 0.2f))
    ) {

        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Health Score", style = MaterialTheme.typography.titleLarge)

            Text(
                text = score.toString(),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = scoreColor
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = category,
                style = MaterialTheme.typography.titleMedium,
                color = scoreColor
            )
        }
    }
}

// -------------------------------------------------------------------
// ENHANCED VITALS CARD
// -------------------------------------------------------------------

@Composable
fun VitalsCardEnhanced(
    bmi: Float,
    sys: String,
    dia: String,
    heart: Float,
    maintenanceCalories: Int,
    userCalories: String
) {

    val userCalInt = userCalories.toIntOrNull() ?: 0

    Card(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Text("Vitals Summary", style = MaterialTheme.typography.titleMedium)

            Text("BMI: %.1f".format(bmi))
            Text("Blood Pressure: $sys / $dia")
            Text("Heart Rate: ${heart.roundToInt()} bpm")
            Text("Maintenance Calories: $maintenanceCalories kcal/day")

            if (userCalInt > 0) {
                Text("Your Intake: $userCalInt kcal/day")
                if (userCalInt > maintenanceCalories + 200)
                    Text("• You are eating more than required. Reduce portions slightly.", color = Color(0xFFE53935))
                if (userCalInt < maintenanceCalories - 200)
                    Text("• You are eating too little. Increase healthy calories.", color = Color(0xFFFB8C00))
            }
        }
    }
}

// -------------------------------------------------------------------

@Composable
fun RecommendationCard(recs: List<String>) {

    Card(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text("Recommendations", style = MaterialTheme.typography.titleMedium)

            recs.forEach { Text("• $it") }
        }
    }
}



fun generateIndianRecommendations(
    bmi: Float,
    steps: Float,
    sleep: Float,
    exercise: Float,
    systolic: String,
    diastolic: String,
    heartRate: Float
): List<String> {

    val list = mutableListOf<String>()

    // ------------------ BMI Based ------------------
    when {
        bmi < 18.5 -> {
            list += "Increase calorie intake using dal, paneer, peanut chikki, ghee on roti, and bananas."
            list += "Add protein-rich foods: rajma, chole, moong dal, curd + jaggery."
            list += "Avoid skipping breakfast. Include poha/upma with peanuts."
        }
        bmi in 18.5..24.9 -> {
            list += "Maintain balance diet with roti + sabzi + dal + curd."
            list += "Prefer whole grains: multigrain roti, brown rice, millet khichdi."
        }
        bmi in 25f..29.9f -> {
            list += "Reduce refined carbs: cut down on white rice & sugar tea."
            list += "Add 20–30 min brisk walk after dinner to improve digestion."
            list += "Use low-oil cooking: air fryer, boiling, steaming."
        }
        bmi >= 30 -> {
            list += "Replace dinner with light options: moong dal chilla, oats upma, vegetable soup."
            list += "Avoid fried foods: samosa, kachori, pakoda, paratha with excess oil."
            list += "Walk 8,000–10,000 steps everyday; break sitting every 45 min."
        }
    }

    // ------------------ Blood Pressure ------------------
    val sysNum = systolic.substringBefore("–").toFloatOrNull() ?: 120f
    val diaNum = diastolic.substringBefore("–").toFloatOrNull() ?: 80f

    if (sysNum > 140 || diaNum > 90) {
        list += "Reduce salt intake. Use Sendha namak and avoid packaged snacks."
        list += "Drink coconut water or lemon water without sugar."
        list += "Do 10 minutes pranayama: Anulom Vilom + Bhramari."
    }

    // ------------------ Heart Rate ------------------
    when {
        heartRate > 95 -> {
            list += "Practice deep breathing 3 times a day (5 minutes each)."
            list += "Avoid excessive caffeine (tea/coffee)."
        }
        heartRate < 55 -> {
            list += "Increase electrolyte intake: ORS, lemon water, coconut water."
        }
    }

    // ------------------ Steps ------------------
    when {
        steps < 3000 -> {
            list += "Walk at least 20 minutes after lunch or dinner daily."
            list += "Aim for 6,000–8,000 steps using small breaks."
        }
        steps in 3000f..7000f -> {
            list += "Good effort! Try adding 10 min jogging or skipping."
        }
        else -> {
            list += "Great activity levels! Maintain consistency."
        }
    }

    // ------------------ Sleep ------------------
    when {
        sleep < 6 -> list += "Fix a sleep schedule; avoid phone 1 hour before bedtime."
        sleep in 6f..8f -> list += "Good sleep routine! Continue maintaining it."
        sleep > 8.5 -> list += "Avoid oversleeping; try morning walk for energy."
    }

    // ------------------ Exercise ------------------
    when {
        exercise < 2 -> list += "Add 15–20 min home workout: Surya Namaskar, squats, push-ups."
        exercise in 2f..4f -> list += "Good workout habit; gradually increase intensity."
        else -> list += "Excellent exercise routine! Keep it going."
    }

    // ------------------ Random Lifestyle Tips ------------------
    val extra = listOf(
        "Drink warm water in morning for digestion.",
        "Replace sugar tea with masala chai without sugar.",
        "Add fruits like guava, apple, banana, papaya daily.",
        "Use cold-pressed mustard/olive oil. Avoid vanaspati.",
        "Include buttermilk or curd with lunch.",
        "Avoid eating heavy meals after 9 PM."
    )

    // pick 2 random extras for variety
    list += extra.shuffled().take(2)

    return list
}
