package com.example.wellmate

// core Android / Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

// ViewModel helpers
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel

// Compose UI
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.wellmate.ui.InteractivePredictorScreen
import com.example.wellmate.ui.LatestResultCard

// Coroutines
import kotlinx.coroutines.launch

// Your app packages
import com.example.wellmate.ui.theme.WellMateTheme
import com.example.wellmate.ui.LatestResultCard
import com.example.wellmate.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    // If you prefer Activity-scoped ViewModel instance instead of viewModel() inside Compose:
    // private val activityVm: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WellMateTheme {
                // Show the interactive screen (it uses viewModel() internally)
                InteractivePredictorScreen()
            }
        }
    }
}
