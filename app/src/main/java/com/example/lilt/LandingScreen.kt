package com.example.lilt

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.lilt.ui.theme.AppTypography
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(modifier: Modifier = Modifier, navController: NavController) {

    // This LaunchedEffect will run once when the composable is first displayed.
    // After a 3-second delay, it will navigate to the next screen.
    LaunchedEffect(key1 = true) {
        delay(3000L) // Wait for 3 seconds
        navController.navigate("energyPlayer") {
            // Pop the splash screen from the back stack so the user can't navigate back to it.
            // Make sure your splash screen's route in the NavHost is "splash".
            popUpTo("splash") { inclusive = true }
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.landingscreenbg),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(0.05f),
                            MaterialTheme.colorScheme.background.copy(0.35f),
                            MaterialTheme.colorScheme.background.copy(0.55f),
                            MaterialTheme.colorScheme.background.copy(0.80f)
                        ),
                        startY = 2f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )
        // Centered Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center // Content is now centered
        ) {
            Text(
                text = "Enjoy Your Favorite Music.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.secondary,
                style = AppTypography.titleLarge
            )
Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Made with ❤️ by Lilt Team",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.secondary,
                style = AppTypography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)

            )
        }
    }
}
