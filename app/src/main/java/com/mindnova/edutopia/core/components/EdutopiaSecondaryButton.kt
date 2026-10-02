package com.mindnova.edutopia.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindnova.edutopia.core.theme.BrandIndigo
import com.mindnova.edutopia.core.theme.TextWhitePrimary
import com.mindnova.edutopia.core.theme.TextWhiteSecondary

/** Outlined companion to the gradient primary button. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = 52.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.2.dp, BrandIndigo.copy(alpha = 0.55f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhiteSecondary)
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun DestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = 44.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.2.dp, com.mindnova.edutopia.core.theme.AccentRose.copy(alpha = 0.6f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = com.mindnova.edutopia.core.theme.AccentRose)
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
