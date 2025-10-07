package com.example.wellmate.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.example.wellmate.viewmodel.ProfileViewModel
import androidx.compose.ui.Alignment
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractivePredictorScreen(profileViewModel: ProfileViewModel = viewModel()) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    // input states (strings so user can type)
    var age by remember { mutableStateOf("23") }
    var height by remember { mutableStateOf("170") }
    var weight by remember { mutableStateOf("79") }
    var activity by remember { mutableStateOf("2") }
    var sleep by remember { mutableStateOf("6") }
    var dietScore by remember { mutableStateOf("6") }
    var dietType by remember { mutableStateOf("vegetarian") } // "vegetarian" or "non_vegetarian"
    var goal by remember { mutableStateOf("lose") } // "lose", "gain", "maintain"

    // VM exposeds
    val status by profileViewModel.status.collectAsState()
    val lastSaved by profileViewModel.lastSaved.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("AI Health & Fitness Demo") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text("Enter your details", style = MaterialTheme.typography.titleMedium)

            // Numeric fields
            OutlinedTextField(
                value = age,
                onValueChange = { age = it.filterDigits() },
                label = { Text("Age (years)") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = height,
                onValueChange = { height = it.filterDigits() },
                label = { Text("Height (cm)") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it.filterFloat() },
                label = { Text("Weight (kg)") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = activity,
                onValueChange = { activity = it.filterDigits() },
                label = { Text("Activity (0 sedentary .. 4 very active)") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = sleep,
                onValueChange = { sleep = it.filterFloat() },
                label = { Text("Sleep hours (e.g. 7.5)") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = dietScore,
                onValueChange = { dietScore = it.filterDigits() },
                label = { Text("Diet score (0..10)") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Simple text fields
            OutlinedTextField(
                value = dietType,
                onValueChange = { dietType = it },
                label = { Text("Diet type (vegetarian/non_vegetarian)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = goal,
                onValueChange = { goal = it },
                label = { Text("Goal (lose/gain/maintain)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        // basic validation
                        val a = age.toIntOrNull()
                        val h = height.toFloatOrNull()
                        val w = weight.toFloatOrNull()
                        val act = activity.toIntOrNull()
                        val sl = sleep.toFloatOrNull()
                        val ds = dietScore.toIntOrNull()
                        if (a == null || h == null || w == null || act == null || sl == null || ds == null) {
                            snackbarHostState.showSnackbar("Please fill all numeric fields correctly")
                            return@launch
                        }

                        isLoading = true
                        try {
                            profileViewModel.computeAndSaveSuspend(
                                userId = "demo",
                                sex = 0,
                                age = a,
                                heightCm = h,
                                weightKg = w,
                                activity = act,
                                sleep = sl,
                                dietScore = ds,
                                hasDiabetes = 0,
                                hasHtn = 0,
                                smoker = 0,
                                goal = goal,
                                dietType = dietType
                            )
                            snackbarHostState.showSnackbar("Prediction saved. See recommendation below.")
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Error: ${e.message}")
                        } finally {
                            isLoading = false
                        }
                    }
                }
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Computing...")
                } else {
                    Text("Run Health Prediction")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Show status and immediate saved item from VM
            Text("Status: $status", style = MaterialTheme.typography.bodySmall)

            if (lastSaved == null) {
                Text("No saved result yet.", style = MaterialTheme.typography.bodyMedium)
            } else {
                // friendly recommendation card using the saved HealthScore
                RecommendationCard(lastSaved!!.score, lastSaved!!.category, lastSaved!!.recommendationJson)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("What the test score means:", style = MaterialTheme.typography.titleSmall)
            ScoreLegend()
        }
    }
}

// small helper composable to render recommendation JSON (basic formatting)
@Composable
fun RecommendationCard(score: Int, category: String, recommendationJson: String) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Risk score: $score", style = MaterialTheme.typography.headlineSmall)
            Text("Category: $category", style = MaterialTheme.typography.titleMedium)
            Text("Recommendations:", style = MaterialTheme.typography.titleSmall)
            // We stored recommendationJson as a compact JSON string earlier.
            // For quick demo, show it raw (in production parse into objects).
            Text(recommendationJson, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun ScoreLegend() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("0–19: Low risk — keep current healthy habits; maintain balanced diet and regular exercise.")
        Text("20–39: Moderate risk — reduce processed foods, increase daily activity to 30–45 mins, improve sleep.")
        Text("40–59: Elevated risk — consider structured exercise + reduce calories moderately; consult practitioner for persistent issues.")
        Text("60–79: High risk — medical check recommended; focus on cardio + resistance training, controlled diet, monitor BP/glucose.")
        Text("80–100: Very High — urgent medical advice recommended; risk factors present; stop smoking, controlled medication, specialist consult.")
    }
}

/** Utility helpers to restrict input chars (very small) **/
private fun String.filterDigits(): String = this.filter { it.isDigit() }
private fun String.filterFloat(): String = this.filter { it.isDigit() || it == '.' }
