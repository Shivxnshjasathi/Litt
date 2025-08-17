package com.example.lilt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.lilt.ui.theme.LiltTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LiltTheme {
                val navController = rememberNavController()
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "landingScreen",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("energyPlayer") {
                            EnergyPlayerScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                            )
                        }
                        composable("landingScreen") {
                            SplashScreen(
                                modifier = Modifier
                                    .fillMaxSize(),
                                navController = navController
                            )
                        }

                    }
                }
            }
        }
    }
}