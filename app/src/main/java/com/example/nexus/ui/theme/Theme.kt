package com.example.nexus.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme =
    darkColorScheme(
        primary = Purple80,
        secondary = BrandGreen,
        tertiary = Pink80,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = Color(0xFF6141B5),
        secondary = Color(0xFF236B54),
        tertiary = Pink40,
    )

@Composable
fun NexusTheme(darkTheme: Boolean = isSystemInDarkTheme(), dynamicColor: Boolean = true, content: @Composable () -> Unit,) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    val finalColorScheme =
        colorScheme.copy(
            background = if (darkTheme) Color(0xFF12131B) else Color(0xFFF7F7FC),
            onBackground = if (darkTheme) Color(0xFFEEEEEE) else Color(0xFF111111),
            surface = if (darkTheme) Color(0xFF191A24) else Color(0xFFFFFFFF),
            onSurface = if (darkTheme) Color(0xFFEEEEEE) else Color(0xFF111111),
            surfaceVariant = if (darkTheme) Color(0xFF202024) else Color(0xFFF0F0F4),
            onSurfaceVariant = if (darkTheme) Color(0xFFCCCCCC) else Color(0xFF444444),
        )

    MaterialTheme(
        colorScheme = finalColorScheme,
        typography = Typography,
        shapes = androidx.compose.material3.Shapes(
            small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        ),
        content = content,
    )
}
