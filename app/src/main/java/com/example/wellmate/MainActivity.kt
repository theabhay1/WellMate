package com.example.wellmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.wellmate.ui.theme.WellMateTheme
import com.example.wellmate.viewmodel.HealthPredictionViewModel
import com.example.wellmate.ui.NewHealthInputScreen
import com.example.wellmate.ui.NewResultScreen

class MainActivity : ComponentActivity() {

    // Always create ViewModel at Activity level
    private val healthVM by viewModels<HealthPredictionViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val userId = "default_user_001"   // You may replace later with real user login

        setContent {
            WellMateTheme {

                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "health_input"
                ) {

                    // 1️⃣ INPUT SCREEN (16 inputs)
                    composable("health_input") {
                        NewHealthInputScreen(
                            viewModel = healthVM,
                            userId = userId,
                            onResult = { navController.navigate("result") }
                        )
                    }

                    // 2️⃣ RESULT SCREEN
                    composable("result") {
                        NewResultScreen(
                            viewModel = healthVM,
                            onBack = {
                                navController.popBackStack()
                                // After back, status resets to idle to avoid instant navigation
                                healthVM.setStatus("idle")
                            }
                        )
                    }
                }
            }
        }
    }
}
