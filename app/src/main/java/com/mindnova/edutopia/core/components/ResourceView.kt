package com.mindnova.edutopia.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.mindnova.edutopia.core.theme.AccentAmber
import com.mindnova.edutopia.core.theme.TextWhiteSecondary
import com.mindnova.edutopia.core.utils.Resource

/**
 * Renders a [Resource] as Loading / Success / Empty / Error with a retry action.
 * Errors are never collapsed into an empty state.
 */
@Composable
fun <T> ResourceView(
    resource: Resource<T>,
    modifier: Modifier = Modifier,
    loadingMessage: String = "Loading…",
    emptyTitle: String = "Nothing here yet",
    emptyDescription: String = "Content will appear once it is added.",
    onRetry: (() -> Unit)? = null,
    loading: @Composable (() -> Unit)? = null,
    empty: @Composable (() -> Unit)? = null,
    content: @Composable (T) -> Unit
) {
    when (resource) {
        is Resource.Loading -> {
            if (loading != null) loading() else LoadingStateView(message = loadingMessage, modifier = modifier)
        }
        is Resource.Success -> content(resource.data)
        is Resource.Empty -> {
            if (empty != null) {
                empty()
            } else {
                EmptyStateView(title = emptyTitle, description = emptyDescription, modifier = modifier)
            }
        }
        is Resource.Error -> {
            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                ErrorStateView(
                    title = "Couldn't load this section",
                    errorMessage = resource.message,
                    onRetry = onRetry
                )
            }
        }
    }
}

/**
 * Inline error banner used inside already-rendered content (e.g. after a
 * refresh fails while stale data is still shown).
 */
@Composable
fun InlineErrorBanner(message: String, modifier: Modifier = Modifier) {
    Text(
        text = "⚠ $message",
        color = AccentAmber,
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

@Composable
fun InlineNotice(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        color = TextWhiteSecondary,
        fontSize = 12.sp,
        modifier = modifier
    )
}
