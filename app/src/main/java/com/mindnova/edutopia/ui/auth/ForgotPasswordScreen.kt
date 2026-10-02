package com.mindnova.edutopia.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.mindnova.edutopia.core.components.AppTopBar
import com.mindnova.edutopia.core.components.EdutopiaPrimaryButton
import com.mindnova.edutopia.core.components.EdutopiaTextField
import com.mindnova.edutopia.core.theme.AccentEmerald
import com.mindnova.edutopia.core.theme.BackgroundDark
import com.mindnova.edutopia.core.theme.TextWhiteMuted
import com.mindnova.edutopia.core.theme.TextWhiteSecondary

/**
 * Standalone password-reset route (registered as Screen.ForgotPassword) in
 * addition to the inline dialog on the login screen. Same validated VM logic.
 */
@Composable
fun ForgotPasswordScreen(
    navController: NavController,
    viewModel: LoginViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF131127), BackgroundDark, Color.Black)
                )
            )
            .imePadding()
    ) {
        AppTopBar(title = "Reset Password", onBack = { navController.popBackStack() })
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                "Enter the email tied to your student account and we'll send a reset link.",
                color = TextWhiteSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(20.dp))
            EdutopiaTextField(
                value = state.resetEmail,
                onValueChange = viewModel::onResetEmailChange,
                placeholder = "student@example.com",
                label = "Email",
                errorMessage = state.resetError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                )
            )
            if (state.resetMessage != null) {
                Spacer(Modifier.height(10.dp))
                Text(state.resetMessage!!, color = AccentEmerald, fontSize = 13.sp)
            }
            Spacer(Modifier.height(22.dp))
            EdutopiaPrimaryButton(
                text = "Send reset link",
                onClick = { viewModel.sendPasswordReset() },
                isLoading = state.isResettingPassword,
                enabled = !state.isResettingPassword
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "Check spam if it doesn't arrive in a couple of minutes.",
                color = TextWhiteMuted,
                fontSize = 11.sp
            )
        }
    }
}
