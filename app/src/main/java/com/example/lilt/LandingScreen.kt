package com.example.lilt

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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

    LaunchedEffect(key1 = true) {
        delay(3000L) // Wait for 3 seconds
        navController.navigate("energyPlayer") {
            popUpTo("splash") { inclusive = true }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splash_transition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500),
            repeatMode = RepeatMode.Reverse
        ), label = "splash_alpha"
    )

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.landingscreenbg),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.background
                        ),
                        startY = 0f
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Discover Your Next Favorite Song.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.secondary,
                style = AppTypography.titleLarge,
                modifier = Modifier.alpha(alpha) // Apply the pulsating alpha
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Personalized music recommendations, just for you.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                style = AppTypography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Made with ❤️ by Lilt",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                style = AppTypography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
