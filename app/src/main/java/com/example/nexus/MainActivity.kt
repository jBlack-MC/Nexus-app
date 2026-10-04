package com.example.nexus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.nexus.settings.SettingsSession
import com.example.nexus.settings.ThemeMode
import com.example.nexus.ui.navigation.NexusNavHost
import com.example.nexus.ui.theme.NexusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by SettingsSession.themeMode.collectAsState()
            val darkTheme =
                when (themeMode) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                }
            val language by SettingsSession.language.collectAsState()
            val localizedContext = androidx.compose.runtime.remember(language) {
                val config = android.content.res.Configuration(resources.configuration)
                config.setLocale(java.util.Locale.forLanguageTag(language))
                createConfigurationContext(config)
            }
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalContext provides localizedContext,
                androidx.compose.ui.platform.LocalConfiguration provides localizedContext.resources.configuration,
            ) {
                NexusTheme(darkTheme = darkTheme) { NexusNavHost() }
            }
        }
    }
}
