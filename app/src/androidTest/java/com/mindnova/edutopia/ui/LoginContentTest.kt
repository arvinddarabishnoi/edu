package com.mindnova.edutopia.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mindnova.edutopia.ui.auth.LoginScreenContent
import com.mindnova.edutopia.ui.auth.LoginUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun render(
        state: LoginUiState,
        dialogOpen: Boolean = false,
        sink: MutableList<String> = mutableListOf()
    ) {
        composeRule.setContent {
            LoginScreenContent(
                state = state,
                onEmailChange = { sink.add("email:$it") },
                onPasswordChange = { sink.add("pass:$it") },
                onSubmit = { sink.add("submit") },
                onForgotClick = { sink.add("forgot") },
                onSignupClick = { sink.add("signup") },
                dialogOpen = dialogOpen,
                onDismissDialog = { sink.add("dismiss") },
                onResetEmailChange = {},
                onSendReset = { sink.add("reset") }
            )
        }
    }

    @Test
    fun loginScreenShowsCoreElements() {
        render(LoginUiState(email = "a@b.co", password = "secret1"))
        composeRule.onNodeWithText("Welcome Back").assertExists()
        composeRule.onNodeWithText("Sign In").assertExists()
        composeRule.onNodeWithText("Forgot Password?").assertExists()
        composeRule.onNodeWithText("Sign Up").assertExists()
    }

    @Test
    fun submitButtonTriggersCallback() {
        val sink = mutableListOf<String>()
        render(LoginUiState(email = "a@b.co", password = "secret1"), sink = sink)
        composeRule.onNodeWithText("Sign In").performClick()
        assertEquals(listOf("submit"), sink)
    }

    @Test
    fun submitIsReplacedBySpinnerWhileLoading() {
        val sink = mutableListOf<String>()
        render(LoginUiState(isSubmitting = true), sink = sink)
        // The gradient button renders a spinner instead of its label while busy,
        // so a duplicate tap on "Sign In" is impossible mid-request.
        composeRule.onNodeWithText("Sign In").assertDoesNotExist()
        composeRule.waitUntil(timeoutMillis = 500) { sink.isEmpty() }
        assertEquals(0, sink.size)
    }

    @Test
    fun errorIsDisplayedUnderPassword() {
        render(LoginUiState(generalError = "Incorrect email or password."))
        composeRule.onNodeWithText("Incorrect email or password.").assertExists()
    }

    @Test
    fun resetDialogShowsEmailAndSendAction() {
        val sink = mutableListOf<String>()
        render(
            LoginUiState(
                resetEmail = "a@b.co",
                resetMessage = "Password reset link sent to a@b.co."
            ),
            dialogOpen = true,
            sink = sink
        )
        composeRule.onNodeWithText("Reset Password").assertExists()
        composeRule.onNodeWithText("Password reset link sent to a@b.co.").assertExists()
        composeRule.onNodeWithText("Send Link").performClick()
        assertEquals(listOf("reset"), sink)
    }
}
