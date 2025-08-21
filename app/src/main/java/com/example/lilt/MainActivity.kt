package com.example.lilt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.lilt.ui.theme.LiltTheme
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {

    // Get a reference to the AuthViewModel
    private val authViewModel: AuthViewModel by viewModels()

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
                        composable("mainScreen") {
                            MainScreen()
                        }

                        // Route for the initial splash screen
                        composable("landingScreen") {
                            SplashScreen(
                                modifier = Modifier.fillMaxSize(),
                                navController = navController
                            )
                        }

                        // Add the new route for the Authentication screen
                        composable("auth") {
                            AuthScreen(
                                modifier = Modifier.fillMaxSize(),
                                navController = navController,
                                authViewModel = authViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}
