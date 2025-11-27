package com.example.wellmate.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.wellmate.viewmodel.HealthPredictionViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewResultScreen(
    viewModel: HealthPredictionViewModel,
    onBack: () -> Unit
) {
    val score by viewModel.score.collectAsState()
    val isDark = isSystemInDarkTheme()

    // Inputs
    val ageStr by viewModel.age.collectAsState()
    val heightStr by viewModel.height.collectAsState()
    val weightStr by viewModel.weight.collectAsState()
    val genderStr by viewModel.gender.collectAsState()
    val stepsStr by viewModel.steps.collectAsState()
    val sysStr by viewModel.systolic.collectAsState()
    val diaStr by viewModel.diastolic.collectAsState()

    val age = ageStr.toIntOrNull() ?: 25
    val height = heightStr.toFloatOrNull() ?: 170f
    val weight = weightStr.toFloatOrNull() ?: 70f
    val steps = stepsStr.toIntOrNull() ?: 5000
    val isMale = genderStr == "Male"

    // Dialog States
    var showVitalsDialog by remember { mutableStateOf(false) }
    var showBPDialog by remember { mutableStateOf(false) }

    val scoreInt = score?.roundToInt() ?: 0

    // Theme-Aware Background Gradient
    val backgroundGradient = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF121212), Color(0xFF37474F)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFF5F7FA), Color(0xFFC3CFE2)))
    }

    // Determine Color/Text based on Score
    val (scoreColor, category, description) = when (scoreInt) {
        in 80..100 -> Triple(Color(0xFF00C853), "Excellent", "Your health indicators are optimal.")
        in 60..79 -> Triple(Color(0xFF64DD17), "Good", "Healthy, but room for improvement.")
        in 40..59 -> Triple(Color(0xFFFFAB00), "Moderate Risk", "Warning signs detected.")
        else -> Triple(Color(0xFFD50000), "High Risk", "Immediate changes recommended.")
    }

    // Dialogs
    if (showVitalsDialog) {
        DetailedVitalsDialog(onDismiss = { showVitalsDialog = false })
    }
    if (showBPDialog) {
        DetailedBPDialog(onDismiss = { showBPDialog = false })
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Health Report") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 1. LIQUID SCORE CARD
            ScoreCard(scoreInt, category, description, scoreColor)

            // 2. VITALS GRID HEADER (Clickable)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showVitalsDialog = true }
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    "Derived Vitals",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Info",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    " (How is this calculated?)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            VitalsGrid(age, height, weight, isMale, steps)

            // 3. BLOOD PRESSURE CARD (Pass callback to open dialog)
            BPCard_ICMR(sysStr, diaStr, onInfoClick = { showBPDialog = true })

            // 4. DISCLAIMER
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "AI prediction only. Consult a doctor for medical advice.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

// =========================================================================
//  DIALOG 1: DETAILED VITALS EXPLANATION
// =========================================================================
@Composable
fun DetailedVitalsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Metric Calculations") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // BMI
                CalculationItem(
                    title = "1. BMI (Body Mass Index)",
                    formula = "Weight (kg) / Height (m)²",
                    desc = "A standard measure of body fat based on height and weight. Normal range is 18.5 – 24.9."
                )

                // BMR
                CalculationItem(
                    title = "2. BMR (Basal Metabolic Rate)",
                    formula = "Mifflin-St Jeor Equation",
                    desc = "The energy your body needs at complete rest. \nMen: (10×W) + (6.25×H) - (5×Age) + 5\nWomen: (10×W) + (6.25×H) - (5×Age) - 161"
                )

                // TDEE
                CalculationItem(
                    title = "3. Maintenance Calories (TDEE)",
                    formula = "BMR × Activity Factor",
                    desc = "Adjusts BMR based on your steps:\n• <3k steps: 1.2x (Sedentary)\n• 3k-6k steps: 1.375x (Light)\n• 6k-10k steps: 1.55x (Moderate)\n• 10k+ steps: 1.725x (Active)"
                )

                // Water
                CalculationItem(
                    title = "4. Water Intake",
                    formula = "Weight (kg) × 35 ml",
                    desc = "General recommendation for daily hydration needs."
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun CalculationItem(title: String, formula: String, desc: String) {
    Column {
        Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Formula: $formula", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top=2.dp))
        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top=2.dp))
    }
}

// =========================================================================
//  DIALOG 2: DETAILED BLOOD PRESSURE GUIDE (ICMR)
// =========================================================================
@Composable
fun DetailedBPDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ICMR BP Guidelines") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Standard reference for Indian population:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(Modifier.height(4.dp))

                // Table Header
                Row(Modifier.fillMaxWidth()) {
                    Text("Category", Modifier.weight(1.2f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    Text("Sys", Modifier.weight(0.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                    Text("Dia", Modifier.weight(0.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                }
                HorizontalDivider()

                // Table Rows
                BpRow("Optimal", "<120", "<80", Color(0xFF2E7D32))
                BpRow("Normal", "120-129", "80-84", Color(0xFF7CB342))
                BpRow("High Normal", "130-139", "85-89", Color(0xFFFFA000))
                BpRow("Grade 1 HTN", "140-159", "90-99", Color(0xFFF57C00))
                BpRow("Grade 2 HTN", "160-179", "100-109", Color(0xFFD32F2F))
                BpRow("Grade 3 HTN", "≥180", "≥110", Color(0xFFB71C1C))
                BpRow("Isolated Sys", "≥140", "<90", Color(0xFFEF6C00))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun BpRow(cat: String, s: String, d: String, c: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(cat, Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall, color = c, fontWeight = FontWeight.SemiBold)
        Text(s, Modifier.weight(0.5f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
        Text(d, Modifier.weight(0.5f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
    }
}

// =========================================================================
//  COMPONENT: LIQUID WAVE SCORE CARD
// =========================================================================
@Composable
fun ScoreCard(score: Int, category: String, desc: String, color: Color) {

    // --- Animation State ---
    val transition = rememberInfiniteTransition(label = "wave")

    // Wave 1 (Front - Slower)
    val waveOffset1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing), // 4 Seconds (Slow)
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1"
    )

    // Wave 2 (Back - Slightly faster/offset for depth)
    val waveOffset2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- LIQUID CONTAINER ---
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(180.dp) // Slightly larger for better look
                    .clip(CircleShape)
                    .background(Color(0xFFF5F5F5)) // Very light grey background
                    .border(4.dp, color.copy(alpha = 0.2f), CircleShape)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    // Water Level Logic
                    val waterLevel = (score / 100f).coerceIn(0.1f, 0.9f)
                    val baseHeight = height * (1 - waterLevel)

                    val waveAmp = 12.dp.toPx() // Height of wave
                    val waveFreq = 1.2f

                    // --- DRAW BACK WAVE (Lighter, for depth) ---
                    val path2 = Path().apply {
                        moveTo(0f, height)
                        lineTo(0f, baseHeight)
                        for (x in 0..width.toInt() step 5) {
                            val xPos = x.toFloat()
                            // Offset by PI to be out of sync with front wave
                            val yPos = (waveAmp * Math.sin((2 * Math.PI / width) * waveFreq * xPos + waveOffset2 + Math.PI)).toFloat() + baseHeight
                            lineTo(xPos, yPos)
                        }
                        lineTo(width, height)
                        close()
                    }
                    drawPath(
                        path = path2,
                        color = color.copy(alpha = 0.3f) // Transparent version of theme color
                    )

                    // --- DRAW FRONT WAVE (Main Color) ---
                    val path1 = Path().apply {
                        moveTo(0f, height)
                        lineTo(0f, baseHeight)
                        for (x in 0..width.toInt() step 5) {
                            val xPos = x.toFloat()
                            val yPos = (waveAmp * Math.sin((2 * Math.PI / width) * waveFreq * xPos + waveOffset1)).toFloat() + baseHeight
                            lineTo(xPos, yPos)
                        }
                        lineTo(width, height)
                        close()
                    }
                    drawPath(
                        path = path1,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                color.copy(alpha = 0.85f),
                                color
                            ),
                            startY = baseHeight - waveAmp,
                            endY = height
                        )
                    )
                }

                // --- TEXT OVERLAY ---
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$score",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 56.sp,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        // Smart text color: White if water is high, Dark if water is low
                        color = if (score > 50) Color.White else color.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "HEALTH SCORE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        ),
                        color = if (score > 50) Color.White.copy(alpha = 0.8f) else Color.Gray
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = category,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = color
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// =========================================================================
//  COMPONENT: VITALS GRID
// =========================================================================
@Composable
fun VitalsGrid(age: Int, height: Float, weight: Float, isMale: Boolean, steps: Int) {
    val heightM = height / 100f
    val bmi = if(heightM > 0) weight / (heightM * heightM) else 0f

    val bmr = if (isMale) (10 * weight) + (6.25 * height) - (5 * age) + 5
    else (10 * weight) + (6.25 * height) - (5 * age) - 161

    val activityMultiplier = when {
        steps < 3000 -> 1.2
        steps < 6000 -> 1.375
        steps < 10000 -> 1.55
        steps < 15000 -> 1.725
        else -> 1.9
    }
    val tdee = (bmr * activityMultiplier).toInt()
    val waterLiters = (weight * 0.033).coerceAtLeast(2.0)
    val maxHr = 220 - age

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VitalCardSmall("BMI", "%.1f".format(bmi), "kg/m²", Modifier.weight(1f))
            VitalCardSmall("Maintenance", "$tdee kcal", "Daily Needs", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VitalCardSmall("Water", "%.1f L".format(waterLiters), "Recommended", Modifier.weight(1f))
            VitalCardSmall("Max HR", "$maxHr bpm", "Limit", Modifier.weight(1f))
        }
    }
}

@Composable
fun VitalCardSmall(title: String, value: String, sub: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

// =========================================================================
//  COMPONENT: BLOOD PRESSURE CARD (ICMR)
// =========================================================================
@Composable
fun BPCard_ICMR(sysStr: String, diaStr: String, onInfoClick: () -> Unit) {
    val s = sysStr.toIntOrNull() ?: 120
    val d = diaStr.toIntOrNull() ?: 80

    val (status, color) = when {
        s >= 180 || d >= 110 -> "Grade 3 HTN" to Color(0xFFB71C1C)
        s >= 160 || d >= 100 -> "Grade 2 HTN" to Color(0xFFD32F2F)
        s >= 140 && d < 90   -> "Isolated Sys HTN" to Color(0xFFEF6C00)
        s >= 140 || d >= 90  -> "Grade 1 HTN" to Color(0xFFF57C00)
        s >= 130 || d >= 85  -> "High Normal" to Color(0xFFFFA000)
        s >= 120 || d >= 80  -> "Normal" to Color(0xFF7CB342)
        else -> "Optimal" to Color(0xFF2E7D32)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // Header with Click Action
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onInfoClick() }
            ) {
                Text("Blood Pressure", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Outlined.Info, "Info", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.weight(1f))
                Text("ICMR Standard", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$s / $d", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(12.dp))
                Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                    Text(status, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}