package com.mindnova.edutopia.core.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mindnova.edutopia.core.theme.AccentRose
import com.mindnova.edutopia.core.theme.BackgroundDark
import com.mindnova.edutopia.core.theme.BrandIndigo
import com.mindnova.edutopia.core.theme.SurfaceBorderDark
import com.mindnova.edutopia.core.theme.SurfaceDarkCard
import com.mindnova.edutopia.core.theme.SurfaceDarkVariant
import com.mindnova.edutopia.core.theme.TextWhiteMuted
import com.mindnova.edutopia.core.theme.TextWhitePrimary
import com.mindnova.edutopia.core.theme.TextWhiteSecondary

/** Standard page top bar with back button and optional actions. */
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(BackgroundDark)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Navigate back",
                    tint = TextWhitePrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            Spacer(Modifier.width(8.dp))
        }
        Column(modifier = Modifier.weight(1f).animateContentSize()) {
            Text(
                text = title,
                color = TextWhitePrimary,
                fontSize = if (onBack == null) 24.sp else 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = if (onBack == null) 2 else 1
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = TextWhiteSecondary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            content = actions
        )
    }
}

@Composable
fun NotificationButton(
    onClick: () -> Unit,
    hasUnread: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        IconButton(onClick = onClick) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = TextWhiteSecondary,
                modifier = Modifier.size(26.dp)
            )
        }
        if (hasUnread) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(AccentRose)
            )
        }
    }
}

@Composable
fun ProfileAvatar(
    name: String,
    photoUrl: String = "",
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 44.dp
) {
    val initials = name.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")
        .ifBlank { "?" }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(BrandIndigo.copy(alpha = 0.25f))
            .border(1.5.dp, BrandIndigo.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (photoUrl.isNotBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Profile photo of $name",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = initials,
                color = TextWhitePrimary,
                fontSize = (size.value / 2.6f).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Section title with an optional trailing action ("See All"). */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = TextWhitePrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun SubjectChip(
    subject: String,
    color: Color,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val background = if (selected) color.copy(alpha = 0.25f) else color.copy(alpha = 0.10f)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .border(1.dp, color.copy(alpha = if (selected) 0.7f else 0.3f), RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = subject,
                color = if (selected) TextWhitePrimary else TextWhiteSecondary,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextWhitePrimary,
    caption: String? = null
) {
    EdutopiaCard(
        modifier = modifier,
        backgroundColor = SurfaceDarkCard
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = value,
                color = valueColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                color = TextWhiteMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
            if (caption != null) {
                Text(
                    text = caption,
                    color = TextWhiteSecondary,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** Skeleton placeholder shown while a section is loading. */
@Composable
fun SkeletonCard(
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 84.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceDarkVariant)
    )
}

@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDarkVariant)
            .border(1.dp, SurfaceBorderDark, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = TextWhiteMuted,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(placeholder, color = TextWhiteMuted, fontSize = 14.sp)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = TextWhitePrimary, fontSize = 14.sp),
                cursorBrush = SolidColor(BrandIndigo),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (query.isNotEmpty()) {
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.size(20.dp).clearAndSetSemantics { }
            ) {
                Icon(Icons.Default.Close, "Clear search", tint = TextWhiteMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String = "Confirm",
    dismissLabel: String = "Cancel",
    destructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDarkCard,
        title = {
            Text(title, color = TextWhitePrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(message, color = TextWhiteSecondary, fontSize = 14.sp, lineHeight = 20.sp)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmLabel,
                    color = if (destructive) AccentRose else MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissLabel, color = TextWhiteMuted)
            }
        }
    )
}
