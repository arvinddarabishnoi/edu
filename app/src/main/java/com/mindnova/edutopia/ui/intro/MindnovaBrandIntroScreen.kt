package com.mindnova.edutopia.ui.intro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mindnova.Edutopia.R
import com.mindnova.edutopia.core.navigation.Screen
import com.mindnova.edutopia.core.theme.AccentCyan
import com.mindnova.edutopia.core.theme.BackgroundDark
import com.mindnova.edutopia.core.theme.BrandIndigo
import com.mindnova.edutopia.core.theme.BrandPurple
import com.mindnova.edutopia.core.theme.TextWhitePrimary
import com.mindnova.edutopia.core.theme.TextWhiteSecondary
import com.mindnova.edutopia.core.utils.Constants
import com.mindnova.edutopia.data.repository.AdminRepository
import com.mindnova.edutopia.data.repository.AuthRepository
import com.mindnova.edutopia.data.repository.UserRepository
import kotlinx.coroutines.delay

@Composable
fun MindnovaBrandIntroScreen(
    navController: NavController,
    authRepo: AuthRepository = remember { AuthRepository() },
    userRepo: UserRepository = remember { UserRepository() },
    adminRepo: AdminRepository = remember { AdminRepository() }
) {
    val scale = remember { Animatable(0.7f) }
    val alpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )

    LaunchedEffect(Unit) {
        // Run brand intro animation
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600)
        )
        textAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600)
        )

        delay(800) // Brief branded hold

        val currentUser = authRepo.currentUser
        if (currentUser == null) {
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.BrandIntro.route) { inclusive = true }
            }
        } else {
            val userResult = userRepo.getUser(currentUser.uid)
            val user = userResult.getOrNull()

            if (user == null || user.name.isBlank() || user.studentClass.isBlank()) {
                navController.navigate(Screen.ProfileSetup.route) {
                    popUpTo(Screen.BrandIntro.route) { inclusive = true }
                }
            } else if (user.role == Constants.ROLE_SUPER_ADMIN || user.role == Constants.ROLE_ADMIN) {
                navController.navigate(Screen.AdminDashboard.route) {
                    popUpTo(Screen.BrandIntro.route) { inclusive = true }
                }
            } else {
                // Update streak & last active timestamp
                userRepo.updateLastActiveAndStreak(currentUser.uid)
                navController.navigate(Screen.StudentHome.route) {
                    popUpTo(Screen.BrandIntro.route) { inclusive = true }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1E1B4B),
                        BackgroundDark,
                        Color.Black
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(scale.value * glowScale)
                    .alpha(alpha.value)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_mindnova_logo),
                    contentDescription = "MINDNOVA Logo",
                    modifier = Modifier.size(110.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "MINDNOVA",
                color = TextWhitePrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "PRESENTS EDUTOPIA",
                color = AccentCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.5.sp,
                modifier = Modifier.alpha(textAlpha.value)
            )
        }
    }
}
