package com.example.wellmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.wellmate.ui.NewHealthInputScreen
import com.example.wellmate.ui.NewResultScreen
import com.example.wellmate.ui.theme.WellMateTheme
import com.example.wellmate.viewmodel.HealthPredictionViewModel

class MainActivity : ComponentActivity() {

    private val healthVM by viewModels<HealthPredictionViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val userId = "default_user_001"

        setContent {
            WellMateTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "health_input"
                ) {
                    composable("health_input") {
                        NewHealthInputScreen(
                            viewModel = healthVM,
                            userId = userId,
                            onResult = { navController.navigate("result") }
                        )
                    }

                    composable("result") {
                        NewResultScreen(
                            viewModel = healthVM,
                            onBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}