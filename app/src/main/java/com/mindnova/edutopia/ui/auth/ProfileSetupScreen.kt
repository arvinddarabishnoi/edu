package com.mindnova.edutopia.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mindnova.edutopia.core.components.EdutopiaPrimaryButton
import com.mindnova.edutopia.core.components.EdutopiaTextField
import com.mindnova.edutopia.core.navigation.Screen
import com.mindnova.edutopia.core.theme.AccentRose
import com.mindnova.edutopia.core.theme.BackgroundDark
import com.mindnova.edutopia.core.theme.BrandIndigo
import com.mindnova.edutopia.core.theme.TextWhitePrimary
import com.mindnova.edutopia.core.theme.TextWhiteSecondary

@Composable
fun ProfileSetupScreen(
    navController: NavController,
    viewModel: ProfileSetupViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) {
            navController.navigate(Screen.StudentHome.route) {
                popUpTo(Screen.BrandIntro.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF131127), BackgroundDark, Color.Black)
                )
            )
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Complete Your Profile",
                color = TextWhitePrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tell us about yourself to personalize your JEE journey",
                color = TextWhiteSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            EdutopiaTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                placeholder = "Enter your full name",
                label = "Full Name"
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Select your class",
                color = TextWhiteSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf("Class 11", "Class 12", "Dropper").forEach { option ->
                    val selected = state.studentClass == option
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .background(
                                if (selected) BrandIndigo else Color(0xFF1E293B),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { viewModel.onClassChange(option) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option,
                            color = if (selected) TextWhitePrimary else TextWhiteSecondary,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            if (state.error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = state.error!!,
                    color = AccentRose,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            EdutopiaPrimaryButton(
                text = "Continue to Edutopia",
                onClick = { viewModel.save() },
                isLoading = state.isSaving,
                enabled = !state.isSaving
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
