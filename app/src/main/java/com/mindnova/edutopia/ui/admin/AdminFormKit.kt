package com.mindnova.edutopia.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindnova.edutopia.core.components.ConfirmationDialog
import com.mindnova.edutopia.core.components.EdutopiaPrimaryButton
import com.mindnova.edutopia.core.components.ErrorStateView
import com.mindnova.edutopia.core.components.ResourceView
import com.mindnova.edutopia.core.components.SearchField
import com.mindnova.edutopia.core.components.SkeletonCard
import com.mindnova.edutopia.core.theme.BackgroundDark
import com.mindnova.edutopia.core.theme.SurfaceDarkCard
import com.mindnova.edutopia.core.theme.TextWhiteMuted
import com.mindnova.edutopia.core.theme.TextWhitePrimary
import com.mindnova.edutopia.core.utils.Resource

/**
 * Shared dialog shell for admin create/edit forms.
 */
@Composable
fun AdminEditorDialog(
    title: String,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    saveLabel: String = "Save",
    saveEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDarkCard,
        title = {
            Text(title, color = TextWhitePrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = saveEnabled) {
                Text(saveLabel, color = TextWhitePrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextWhiteMuted)
            }
        }
    )
}

/**
 * Shared list body for admin CRUD screens: search + states + rows.
 */
@Composable
fun <T> AdminCrudList(
    resource: Resource<List<T>>,
    searchHint: String?,
    query: String,
    onQueryChange: (String) -> Unit,
    emptyTitle: String,
    emptyDescription: String,
    rowContent: @Composable (T) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        if (searchHint != null) {
            SearchField(
                query = query,
                onQueryChange = onQueryChange,
                placeholder = searchHint,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
        ResourceView(
            resource = resource,
            loading = {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SkeletonCard()
                    SkeletonCard()
                    SkeletonCard()
                }
            },
            empty = {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(emptyTitle, color = TextWhitePrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            emptyDescription,
                            color = TextWhiteMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 40.dp)
                        )
                    }
                }
            },
            content = { items ->
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items.forEach { item ->
                        item { rowContent(item) }
                    }
                }
            }
        )
    }
}

@Composable
fun AdminDeleteConfirm(
    itemName: String,
    extraWarning: String? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialog(
        title = "Delete this item?",
        message = "This will permanently delete \"$itemName\"." +
            (extraWarning?.let { " $it" } ?: "") + " This cannot be undone.",
        confirmLabel = "Delete",
        destructive = true,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
