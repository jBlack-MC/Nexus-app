package com.example.nexus.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized design tokens for spacing and key layout dimensions across the Nexus app.
 */
object Spacing {
    /** 2.dp - Micro spacing */
    val xxs: Dp = 2.dp

    /** 4.dp - Extra small spacing (e.g. tight inner margins, minor offsets) */
    val xs: Dp = 4.dp

    /** 6.dp - Compact small spacing */
    val smCompact: Dp = 6.dp

    /** 8.dp - Small spacing (e.g. item gaps, button row spacing, minor padding) */
    val sm: Dp = 8.dp

    /** 10.dp - Compact medium spacing */
    val mdCompact: Dp = 10.dp

    /** 12.dp - Medium-small spacing (e.g. list item gaps, card internal padding) */
    val smd: Dp = 12.dp

    /** 16.dp - Standard/Medium outer container padding and card padding */
    val md: Dp = 16.dp

    /** 20.dp - Compact large spacing */
    val xlCompact: Dp = 20.dp

    /** 22.dp - Extra large spacing */
    val xxl: Dp = 22.dp

    /** 24.dp - Large outer container padding and section spacing */
    val lg: Dp = 24.dp

    /** 32.dp - Extra large spacing (e.g. hero section gaps, empty state padding) */
    val xl: Dp = 32.dp

    // Logo sizing design tokens
    /** Standard top-bar logo size (32.dp icon, 22sp text) used across main app screens */
    val TopBarLogoSize: Dp = 32.dp
    val TopBarLogoTextSize: Int = 22

    /** Auth-screen logo size (80.dp icon, 42sp text) unique to Login and Register screens */
    val AuthLogoSize: Dp = 80.dp
    val AuthLogoTextSize: Int = 42
}
