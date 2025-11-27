package com.example.wellmate.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wellmate.viewmodel.HealthPredictionViewModel

@Composable
fun NewHealthInputScreen(
    viewModel: HealthPredictionViewModel,
    userId: String,
    onResult: () -> Unit
) {
    val scroll = rememberScrollState()
    val focus = LocalFocusManager.current
    val isDark = isSystemInDarkTheme()

    // Collect ViewModel state
    val age by viewModel.age.collectAsState()
    val height by viewModel.height.collectAsState()
    val weight by viewModel.weight.collectAsState()
    val steps by viewModel.steps.collectAsState()
    val sleep by viewModel.sleep.collectAsState()
    val calories by viewModel.calories.collectAsState()
    val heart by viewModel.heart.collectAsState()
    val exercise by viewModel.exercise.collectAsState()
    val alcohol by viewModel.alcohol.collectAsState()

    val systolic by viewModel.systolic.collectAsState()
    val diastolic by viewModel.diastolic.collectAsState()

    val gender by viewModel.gender.collectAsState()
    val smoker by viewModel.smoker.collectAsState()
    val diabetic by viewModel.diabetic.collectAsState()
    val heartDisease by viewModel.heartDisease.collectAsState()

    val status by viewModel.status.collectAsState()

    var shouldNavigate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.resetState() }

    LaunchedEffect(status) {
        if (shouldNavigate && status == "ok") {
            shouldNavigate = false
            onResult()
        }
    }

    /* ---------------- Theme-Aware Gradient ---------------- */
    val gradient = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF121212), Color(0xFF263238)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF2C3E50), Color(0xFF4CA1AF)))
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(16.dp)
                ) {
                    if (status.startsWith("error")) {
                        Text(
                            text = "⚠ " + status.removePrefix("error: "),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                    }

                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        onClick = {
                            focus.clearFocus()
                            shouldNavigate = true
                            viewModel.predictAndSave(userId)
                        },
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (status == "running") {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Calculating...", style = MaterialTheme.typography.titleMedium)
                        } else {
                            Text("Calculate Score", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Spacer(Modifier.height(8.dp))

            /* ---------------- BRANDED HEADER ---------------- */
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "WELLMATE",
                    style = androidx.compose.ui.text.TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFFFFFFF), Color(0xFF80CBC4))
                        ),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 4.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
                    )
                )

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF80CBC4),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "AI Powered Health Companion",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFB2DFDB),
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    )
                }
            }

            /* ---------------- SECTION: Personal ---------------- */
            SectionCardSolid(title = "Personal Details") {
                DropdownField("Gender", gender, listOf("Male", "Female")) { viewModel.gender.value = it }
                NumberField("Age", age, "yrs", maxLength = 3) { viewModel.age.value = it }
            }

            /* ---------------- SECTION: Body Metrics ---------------- */
            SectionCardSolid(title = "Body Metrics") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Height", height, "cm", Modifier.weight(1f), maxLength = 5) { viewModel.height.value = it }
                    NumberField("Weight", weight, "kg", Modifier.weight(1f), maxLength = 5) { viewModel.weight.value = it }
                }

                val h = height.toFloatOrNull()
                val w = weight.toFloatOrNull()
                if (h != null && w != null && h > 0) {
                    val bmi = w / ((h / 100) * (h / 100))
                    Text(
                        "Estimated BMI: %.1f".format(bmi),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            /* ---------------- SECTION: Activity & Sleep ---------------- */
            SectionCardSolid(title = "Activity & Sleep") {
                // Steps Logic with Error State
                Column {
                    val stepVal = steps.toIntOrNull() ?: 0
                    val isStepError = stepVal > 15000

                    NumberField(
                        label = "Daily Steps",
                        value = steps,
                        suffix = "steps",
                        maxLength = 6,
                        isError = isStepError // Turns field red
                    ) { viewModel.steps.value = it }

                    if (isStepError) {
                        Text(
                            text = "⚠️ > 15k steps is unusually high for daily avg.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val sleepOptions = (4..12).map { it.toString() }
                    DropdownField(
                        label = "Sleep",
                        selected = sleep,
                        options = sleepOptions,
                        modifier = Modifier.weight(1f),
                        onSelect = { viewModel.sleep.value = it }
                    )

                    // Exercise Logic with Error State
                    Column(modifier = Modifier.weight(1f)) {
                        val exVal = exercise.toFloatOrNull() ?: 0f
                        val isExError = exVal > 14

                        NumberField(
                            label = "Exercise",
                            value = exercise,
                            suffix = "hrs/wk",
                            maxLength = 4,
                            isError = isExError, // Turns field red
                            modifier = Modifier.fillMaxWidth()
                        ) { viewModel.exercise.value = it }

                        if (isExError) {
                            Text(
                                text = "⚠️ > 2hrs/day?",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                            )
                        }
                    }
                }
            }

            /* ---------------- Vitals ---------------- */
            SectionCardSolid(title = "Heart & BP") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DropdownField("Sys BP", systolic, listOf("100", "110", "120", "130", "140", "150"), Modifier.weight(1f)) {
                        viewModel.systolic.value = it
                    }
                    DropdownField("Dia BP", diastolic, listOf("60", "70", "80", "90", "100"), Modifier.weight(1f)) {
                        viewModel.diastolic.value = it
                    }
                }
                NumberField("Heart Rate", heart, "bpm", maxLength = 3) { viewModel.heart.value = it }
            }

            /* ---------------- Lifestyle ---------------- */
            SectionCardSolid(title = "Lifestyle Habits") {
                NumberField("Calories Intake", calories, "kcal", maxLength = 5) { viewModel.calories.value = it }

                val alcoholOptions = (0..7).map { it.toString() }
                DropdownField(
                    label = "Alcohol (units/wk)",
                    selected = alcohol,
                    options = alcoholOptions,
                    onSelect = { viewModel.alcohol.value = it }
                )

                DropdownField("Smoker?", smoker, listOf("No", "Yes")) { viewModel.smoker.value = it }
                DropdownField("Diabetic?", diabetic, listOf("No", "Yes")) { viewModel.diabetic.value = it }
                DropdownField("Heart Disease History?", heartDisease, listOf("No", "Yes")) { viewModel.heartDisease.value = it }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

/* ------------------ REUSABLE COMPONENTS ------------------ */

@Composable
fun SectionCardSolid(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
fun NumberField(
    label: String,
    value: String,
    suffix: String = "",
    modifier: Modifier = Modifier.fillMaxWidth(),
    maxLength: Int = 10,
    isError: Boolean = false, // New parameter to trigger red visual state
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (input.length <= maxLength) {
                if (input.count { it == '.' } <= 1 && input.all { it.isDigit() || it == '.' }) {
                    onChange(input)
                }
            }
        },
        label = { Text(label) },
        suffix = { if(suffix.isNotEmpty()) Text(suffix) },
        singleLine = true,
        isError = isError, // Highlights the box in Red
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next
        ),
        modifier = modifier
    )
}

@Composable
fun DropdownField(
    label: String,
    selected: String,
    options: List<String>,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt, color = MaterialTheme.colorScheme.onSurface) },
                    onClick = {
                        expanded = false
                        onSelect(opt)
                    }
                )
            }
        }
    }
}