package com.mindnova.edutopia.ui.intro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mindnova.edutopia.R
import com.mindnova.edutopia.core.components.EdutopiaPrimaryButton
import com.mindnova.edutopia.core.navigation.Screen
import com.mindnova.edutopia.core.theme.AccentCyan
import com.mindnova.edutopia.core.theme.BackgroundDark
import com.mindnova.edutopia.core.theme.TextWhiteMuted
import com.mindnova.edutopia.core.theme.TextWhitePrimary
import com.mindnova.edutopia.core.theme.TextWhiteSecondary

@Composable
fun MindnovaBrandIntroScreen(
    navController: NavController,
    viewModel: BrandIntroViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
        scale.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        alpha.animateTo(1f, tween(600))
        textAlpha.animateTo(1f, tween(600))
    }

    LaunchedEffect(state.destination) {
        when (state.destination) {
            IntroDestination.Login -> navController.navigate(Screen.Login.route) {
                popUpTo(Screen.BrandIntro.route) { inclusive = true }
            }
            IntroDestination.ProfileSetup -> navController.navigate(Screen.ProfileSetup.route) {
                popUpTo(Screen.BrandIntro.route) { inclusive = true }
            }
            IntroDestination.StudentHome -> navController.navigate(Screen.StudentHome.route) {
                popUpTo(Screen.BrandIntro.route) { inclusive = true }
            }
            IntroDestination.AdminDashboard -> navController.navigate(Screen.AdminDashboard.route) {
                popUpTo(Screen.BrandIntro.route) { inclusive = true }
            }
            IntroDestination.None -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1E1B4B), BackgroundDark, Color.Black)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (state.error != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(32.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Connection problem",
                    color = TextWhitePrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = state.error!!,
                    color = TextWhiteMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                EdutopiaPrimaryButton(
                    text = "Retry",
                    onClick = { viewModel.resolveRoute() },
                    modifier = Modifier.width(180.dp),
                    height = 46.dp
                )
            }
        } else {
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

                if (!state.animationDone) {
                    Spacer(modifier = Modifier.height(40.dp))
                    Text(
                        text = "Restoring your session…",
                        color = TextWhiteSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.alpha(textAlpha.value)
                    )
                }
            }
        }
    }
}
