package com.mindnova.edutopia.ui.auth

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mindnova.Edutopia.R
import com.mindnova.edutopia.core.components.EdutopiaPrimaryButton
import com.mindnova.edutopia.core.components.EdutopiaTextField
import com.mindnova.edutopia.core.navigation.Screen
import com.mindnova.edutopia.core.theme.AccentCyan
import com.mindnova.edutopia.core.theme.BackgroundDark
import com.mindnova.edutopia.core.theme.BrandIndigo
import com.mindnova.edutopia.core.theme.SurfaceDarkCard
import com.mindnova.edutopia.core.theme.TextWhiteMuted
import com.mindnova.edutopia.core.theme.TextWhitePrimary
import com.mindnova.edutopia.core.theme.TextWhiteSecondary
import com.mindnova.edutopia.core.utils.Constants
import com.mindnova.edutopia.data.repository.AuthRepository
import com.mindnova.edutopia.data.repository.UserRepository
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    navController: NavController,
    authRepo: AuthRepository = remember { AuthRepository() },
    userRepo: UserRepository = remember { UserRepository() }
) {
    val coroutineScope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetStatusMessage by remember { mutableStateOf<String?>(null) }

    fun handleLogin() {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Please enter both email and password."
            return
        }
        isLoading = true
        errorMessage = null

        coroutineScope.launch {
            val result = authRepo.login(email, password)
            isLoading = false
            result.onSuccess { user ->
                val profileResult = userRepo.getUser(user.uid)
                val profile = profileResult.getOrNull()

                if (profile == null || profile.name.isBlank() || profile.studentClass.isBlank()) {
                    navController.navigate(Screen.ProfileSetup.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                } else if (profile.role == Constants.ROLE_SUPER_ADMIN || profile.role == Constants.ROLE_ADMIN) {
                    navController.navigate(Screen.AdminDashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                } else {
                    userRepo.updateLastActiveAndStreak(user.uid)
                    navController.navigate(Screen.StudentHome.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            }.onFailure { error ->
                errorMessage = error.localizedMessage ?: "Login failed. Please check your credentials."
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF131127),
                        BackgroundDark,
                        Color.Black
                    )
                )
            )
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Image(
                painter = painterResource(id = R.drawable.ic_edutopia_logo),
                contentDescription = "Edutopia Logo",
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome Back",
                color = TextWhitePrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Sign in to continue your JEE preparation",
                color = TextWhiteSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            EdutopiaTextField(
                value = email,
                onValueChange = { email = it; errorMessage = null },
                placeholder = "Enter your email",
                label = "Email Address",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = BrandIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            EdutopiaTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                placeholder = "Enter your password",
                label = "Password",
                isPassword = true,
                errorMessage = errorMessage,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = BrandIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Forgot Password?",
                    color = AccentCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        resetEmail = email
                        resetStatusMessage = null
                        showForgotPasswordDialog = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            EdutopiaPrimaryButton(
                text = "Sign In",
                onClick = { handleLogin() },
                isLoading = isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Don't have an account?",
                    color = TextWhiteMuted,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Sign Up",
                    color = AccentCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        navController.navigate(Screen.Signup.route)
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            containerColor = SurfaceDarkCard,
            title = {
                Text(
                    text = "Reset Password",
                    color = TextWhitePrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter your registered email to receive a password reset link.",
                        color = TextWhiteSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    EdutopiaTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        placeholder = "student@example.com",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    if (resetStatusMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = resetStatusMessage!!,
                            color = AccentCyan,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (resetEmail.isNotBlank()) {
                            coroutineScope.launch {
                                authRepo.sendPasswordReset(resetEmail)
                                    .onSuccess {
                                        resetStatusMessage = "Password reset link sent to $resetEmail"
                                    }
                                    .onFailure {
                                        resetStatusMessage = it.localizedMessage ?: "Failed to send reset link."
                                    }
                            }
                        }
                    }
                ) {
                    Text("Send Link", color = AccentCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("Close", color = TextWhiteMuted)
                }
            }
        )
    }
}
