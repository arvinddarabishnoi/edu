package com.mindnova.edutopia.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Centralized spacing rhythm for the Edutopia design system.
 * Derived from the reference designs: generous 20dp page gutters,
 * 12-16dp gutters between cards, 8-10dp rhythm inside cards.
 */
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp

    /** Standard horizontal page padding. */
    val pagePadding = xl

    /** Vertical gap between major sections. */
    val sectionGap = 20.dp

    /** Gap between sibling cards. */
    val cardGap = md

    /** Internal padding of standard cards. */
    val cardPadding = lg

    /** Corner radius used by most cards. */
    val cardCorner = 18.dp

    /** Bottom bar clearance so content never hides behind the floating nav. */
    val bottomBarClearance = 96.dp
}

/** Standard corner shapes aligned with the spacing scale. */
object Radii {
    val card = RoundedCornerShape(18.dp)
    val section = RoundedCornerShape(22.dp)
    val chip = RoundedCornerShape(10.dp)
    val button = RoundedCornerShape(14.dp)
    val hero = RoundedCornerShape(24.dp)
}
