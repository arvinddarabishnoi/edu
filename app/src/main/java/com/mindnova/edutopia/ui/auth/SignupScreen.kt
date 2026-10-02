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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mindnova.edutopia.core.components.EdutopiaPrimaryButton
import com.mindnova.edutopia.core.components.EdutopiaTextField
import com.mindnova.edutopia.core.navigation.Screen
import com.mindnova.edutopia.core.theme.AccentCyan
import com.mindnova.edutopia.core.theme.AccentRose
import com.mindnova.edutopia.core.theme.BackgroundDark
import com.mindnova.edutopia.core.theme.BrandIndigo
import com.mindnova.edutopia.core.theme.TextWhiteMuted
import com.mindnova.edutopia.core.theme.TextWhitePrimary
import com.mindnova.edutopia.core.theme.TextWhiteSecondary

@Composable
fun SignupScreen(
    navController: NavController,
    viewModel: SignupViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.accountCreated) {
        if (state.accountCreated) {
            navController.navigate(Screen.StudentHome.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
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
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Create Account",
                color = TextWhitePrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Start your JEE preparation journey",
                color = TextWhiteSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            EdutopiaTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                placeholder = "Enter your full name",
                label = "Full Name",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                leadingIcon = {
                    Icon(Icons.Default.Person, null, tint = BrandIndigo, modifier = Modifier.size(20.dp))
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            EdutopiaTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                placeholder = "Enter your email",
                label = "Email Address",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                leadingIcon = {
                    Icon(Icons.Default.Email, null, tint = BrandIndigo, modifier = Modifier.size(20.dp))
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Class / Category",
                color = TextWhiteSecondary,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Class 11", "Class 12", "Dropper").forEach { option ->
                    val selected = state.studentClass == option
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .background(
                                if (selected) BrandIndigo else Color(0xFF1E293B),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.onClassChange(option) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option,
                            color = if (selected) TextWhitePrimary else TextWhiteMuted,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            EdutopiaTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                placeholder = "Create password (min 6 characters)",
                label = "Password",
                isPassword = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                leadingIcon = {
                    Icon(Icons.Default.Lock, null, tint = BrandIndigo, modifier = Modifier.size(20.dp))
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            EdutopiaTextField(
                value = state.confirmPassword,
                onValueChange = viewModel::onConfirmChange,
                placeholder = "Confirm your password",
                label = "Confirm Password",
                isPassword = true,
                errorMessage = state.error,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (!state.isSubmitting) viewModel.signup() }),
                leadingIcon = {
                    Icon(Icons.Default.Lock, null, tint = BrandIndigo, modifier = Modifier.size(20.dp))
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            EdutopiaPrimaryButton(
                text = "Create Account",
                onClick = { viewModel.signup() },
                isLoading = state.isSubmitting,
                enabled = !state.isSubmitting
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Already have an account?", color = TextWhiteMuted, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Sign In",
                    color = AccentCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        navController.popBackStack()
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
