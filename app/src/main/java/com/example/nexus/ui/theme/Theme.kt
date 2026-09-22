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

private val DarkColorScheme = darkColorScheme(
    primary = BrandPurple,
    secondary = BrandGreen,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = BrandPurple,
    secondary = BrandGreen,
    tertiary = Pink40
)

@Composable
fun NexusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val finalColorScheme = colorScheme.copy(
        surface = if (darkTheme) Color(0xFF121214) else Color(0xFFFFFFFF),
        onSurface = if (darkTheme) Color(0xFFEEEEEE) else Color(0xFF111111),
        surfaceVariant = if (darkTheme) Color(0xFF202024) else Color(0xFFF0F0F4),
        onSurfaceVariant = if (darkTheme) Color(0xFFCCCCCC) else Color(0xFF444444)
    )

    MaterialTheme(
        colorScheme = finalColorScheme,
        typography = Typography,
        content = content
    )
}
