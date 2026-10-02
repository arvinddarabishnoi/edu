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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mindnova.edutopia.R
import com.mindnova.edutopia.core.components.EdutopiaPrimaryButton
import com.mindnova.edutopia.core.components.EdutopiaTextField
import com.mindnova.edutopia.core.navigation.Screen
import com.mindnova.edutopia.core.theme.AccentCyan
import com.mindnova.edutopia.core.theme.AccentEmerald
import com.mindnova.edutopia.core.theme.AccentRose
import com.mindnova.edutopia.core.theme.BackgroundDark
import com.mindnova.edutopia.core.theme.BrandIndigo
import com.mindnova.edutopia.core.theme.SurfaceDarkCard
import com.mindnova.edutopia.core.theme.TextWhiteMuted
import com.mindnova.edutopia.core.theme.TextWhitePrimary
import com.mindnova.edutopia.core.theme.TextWhiteSecondary

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    // One-shot navigation on successful login.
    LaunchedEffect(state.destination) {
        when (state.destination) {
            LoginDestination.ProfileSetup -> navController.navigate(Screen.ProfileSetup.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
            LoginDestination.StudentHome -> navController.navigate(Screen.StudentHome.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
            LoginDestination.AdminDashboard -> navController.navigate(Screen.AdminDashboard.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
            LoginDestination.None -> Unit
        }
    }

    LoginScreenContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::login,
        onForgotClick = {
            viewModel.openResetDialog()
            showForgotPasswordDialog = true
        },
        onSignupClick = { navController.navigate(Screen.Signup.route) },
        dialogOpen = showForgotPasswordDialog,
        onDismissDialog = { showForgotPasswordDialog = false },
        onResetEmailChange = viewModel::onResetEmailChange,
        onSendReset = viewModel::sendPasswordReset
    )
}

/**
 * Stateless login UI. Rendered by instrumented tests with fixed states so the
 * presentation is verifiable without Firebase.
 */
@Composable
internal fun LoginScreenContent(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onForgotClick: () -> Unit,
    onSignupClick: () -> Unit,
    dialogOpen: Boolean,
    onDismissDialog: () -> Unit,
    onResetEmailChange: (String) -> Unit,
    onSendReset: () -> Unit
) {
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
                value = state.email,
                onValueChange = onEmailChange,
                placeholder = "Enter your email",
                label = "Email Address",
                errorMessage = state.emailError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
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
                value = state.password,
                onValueChange = onPasswordChange,
                placeholder = "Enter your password",
                label = "Password",
                isPassword = true,
                errorMessage = state.passwordError ?: state.generalError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { if (!state.isSubmitting) onSubmit() }
                ),
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
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .clickable(onClick = onForgotClick)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            EdutopiaPrimaryButton(
                text = "Sign In",
                onClick = onSubmit,
                isLoading = state.isSubmitting,
                enabled = !state.isSubmitting
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    modifier = Modifier.clickable(onClick = onSignupClick)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (dialogOpen) {
        AlertDialog(
            onDismissRequest = onDismissDialog,
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
                        value = state.resetEmail,
                        onValueChange = onResetEmailChange,
                        placeholder = "student@example.com",
                        errorMessage = state.resetError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    if (state.resetMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                tint = AccentEmerald,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = state.resetMessage!!,
                                color = AccentEmerald,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = onSendReset,
                    enabled = !state.isResettingPassword
                ) {
                    if (state.isResettingPassword) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = AccentCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Send Link", color = AccentCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDialog) {
                    Text("Close", color = TextWhiteMuted)
                }
            }
        )
    }
}
