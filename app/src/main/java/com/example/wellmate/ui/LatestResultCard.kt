package com.example.wellmate.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import com.example.wellmate.data.HealthScore
import com.example.wellmate.viewmodel.ProfileViewModel

@Composable
fun LatestResultCard(userId: String = "demo", vm: ProfileViewModel) {
    val latestState = remember { mutableStateOf<HealthScore?>(null) }

    // Collect the Flow inside a LaunchedEffect
    LaunchedEffect(userId) {
        vm.getLatestForUserFlow(userId).collectLatest { latestState.value = it }
    }

    val hs = latestState.value
    Column(modifier = Modifier.padding(12.dp)) {
        if (hs == null) {
            Text("No saved result yet", style = MaterialTheme.typography.bodyLarge)
        } else {
            Text("Score: ${hs.score}", style = MaterialTheme.typography.headlineSmall)
            Text("Category: ${hs.category}", style = MaterialTheme.typography.bodyMedium)
            Text("Confidence: ${hs.confidence}", style = MaterialTheme.typography.bodySmall)
            Text("Recommendation JSON:", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            Text(hs.recommendationJson, style = MaterialTheme.typography.bodySmall)
        }
    }
}
