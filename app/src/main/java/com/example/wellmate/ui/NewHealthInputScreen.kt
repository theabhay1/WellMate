package com.example.wellmate.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.wellmate.viewmodel.HealthPredictionViewModel

// ---------------- VALIDATION ----------------
fun isValid(value: String, min: Float, max: Float): Boolean {
    val num = value.toFloatOrNull() ?: return false
    return num in min..max
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewHealthInputScreen(
    viewModel: HealthPredictionViewModel,
    userId: String,
    onResult: () -> Unit
) {

    val scrollState = rememberScrollState()

    // Bind ViewModel states to UI
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

    // Navigate once score is ready
    LaunchedEffect(status) {
        if (shouldNavigate && status == "ok") {
            shouldNavigate = false
            onResult()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {

                            // VALIDATION
                            val valid =
                                isValid(age, 1f, 120f) &&
                                        isValid(height, 50f, 250f) &&
                                        isValid(weight, 20f, 300f) &&
                                        isValid(steps, 0f, 50000f) &&
                                        isValid(sleep, 0f, 18f) &&
                                        isValid(calories, 500f, 8000f) &&
                                        isValid(heart, 30f, 200f) &&
                                        isValid(exercise, 0f, 50f) &&
                                        isValid(alcohol, 0f, 200f)

                            if (!valid) {
                                viewModel.setStatus("error: invalid_input")
                                return@Button
                            }

                            shouldNavigate = true
                            viewModel.predictAndSave(userId)
                        }
                    ) {
                        Text("Calculate Health Score")
                    }
                }
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            // ---------------- PERSONAL DETAILS ----------------
            SectionHeader("Personal Details")
            DropdownField("Gender", gender, listOf("Male", "Female")) {
                viewModel.gender.value = it
            }
            NumericField("Age", age, { viewModel.age.value = it }, 1f, 120f)

            // ---------------- BODY METRICS ----------------
            SectionHeader("Body Metrics")
            NumericField("Height (cm)", height, { viewModel.height.value = it }, 50f, 250f)
            NumericField("Weight (kg)", weight, { viewModel.weight.value = it }, 20f, 300f)

            // ---------------- DAILY ACTIVITY ----------------
            SectionHeader("Daily Activity")
            NumericField("Daily Steps", steps, { viewModel.steps.value = it }, 0f, 50000f)
            NumericField("Exercise Hours/Week", exercise, { viewModel.exercise.value = it }, 0f, 50f)
            NumericField("Sleep (Hours)", sleep, { viewModel.sleep.value = it }, 0f, 18f)

            // ---------------- VITALS ----------------
            SectionHeader("Vitals")
            DropdownField("Systolic BP (mmHg)", systolic,
                listOf("90–110", "110–130", "130–150", "150–170", "170+")
            ) { viewModel.systolic.value = it }

            DropdownField("Diastolic BP (mmHg)", diastolic,
                listOf("60–75", "75–90", "90–105", "105–120", "120+")
            ) { viewModel.diastolic.value = it }

            NumericField("Heart Rate (bpm)", heart, { viewModel.heart.value = it }, 30f, 200f)

            // ---------------- LIFESTYLE HABITS ----------------
            SectionHeader("Lifestyle Habits")
            NumericField("Calories Intake Per Day", calories, { viewModel.calories.value = it }, 500f, 8000f)
            NumericField("Alcohol/Week (drinks)", alcohol, { viewModel.alcohol.value = it }, 0f, 200f)

            DropdownField("Smoker", smoker, listOf("Yes", "No")) {
                viewModel.smoker.value = it
            }
            DropdownField("Diabetic", diabetic, listOf("Yes", "No")) {
                viewModel.diabetic.value = it
            }
            DropdownField("Heart Disease", heartDisease, listOf("Yes", "No")) {
                viewModel.heartDisease.value = it
            }

            // STATUS
            if (status.startsWith("error"))
                Text("⚠ Invalid inputs. Please check red fields.", color = Color.Red)

            if (status == "running")
                Text("Calculating...", color = MaterialTheme.colorScheme.primary)

            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}

// ---------------- SECTION HEADER ----------------
@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

// ---------------- NUMERIC FIELD ----------------
@Composable
fun NumericField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    min: Float,
    max: Float
) {
    val isError = value.isNotBlank() && !isValid(value, min, max)

    Column(modifier = Modifier.fillMaxWidth()) {

        OutlinedTextField(
            value = value,
            onValueChange = { onChange(it) },
            label = { Text(label) },
            singleLine = true,
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        if (isError) {
            Text(
                text = "Enter a value between $min and $max",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// ---------------- DROPDOWN FIELD ----------------
@Composable
fun DropdownField(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }
        )

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    }
                )
            }
        }
    }
}
