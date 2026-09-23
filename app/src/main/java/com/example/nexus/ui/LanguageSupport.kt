package com.example.nexus.ui

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

val supportedLanguages = listOf("en" to "English", "zu" to "isiZulu", "tn" to "Setswana")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePicker(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val label = supportedLanguages.firstOrNull { it.first == selectedLanguage }?.second ?: "English"
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Language") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().then(modifier)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            supportedLanguages.forEach { (code, language) ->
                DropdownMenuItem(
                    text = { Text(language) },
                    onClick = {
                        onLanguageSelected(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun CompactLanguageMenu(modifier: Modifier = Modifier) {
    val currentLanguage by com.example.nexus.settings.SettingsSession.language.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    val label = supportedLanguages.firstOrNull { it.first == currentLanguage }?.second ?: "English"
    androidx.compose.foundation.layout.Box(modifier) {
        TextButton(onClick = { expanded = true }) { Text(label) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            supportedLanguages.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        com.example.nexus.settings.SettingsSession.setLanguage(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

fun getAuthCopy(code: String): AuthCopy = when (code) {
    "zu" -> AuthCopy(
        welcome = "Siyakwamukela",
        subtitle = "Ngena ukuze uqhubeke ku-Nexus",
        email = "I-imeyili",
        password = "Iphasiwedi",
        signIn = "Ngena",
        createAccount = "Dala i-akhawunti",
        registerSubtitle = "Qala iphrofayela yakho ye-Nexus",
        displayName = "Igama lokubonisa",
        signingIn = "Kungenwa…",
        creatingAccount = "Kudalwa i-akhawunti…",
        needAccount = "Udinga i-akhawunti? Yenza",
        haveAccount = "Une-akhawunti? Ngena"
    )
    "tn" -> AuthCopy(
        welcome = "Re a go amogela",
        subtitle = "Tsena go tswelela mo Nexus",
        email = "Imeile",
        password = "Khunololamorago",
        signIn = "Tsena",
        createAccount = "Tlhama akhaonto",
        registerSubtitle = "Tlhamo profaele ya gago ya Nexus",
        displayName = "Leina la go bontšha",
        signingIn = "Go tsena…",
        creatingAccount = "Go tlhama akhaonto…",
        needAccount = "O tlhoka akhaonto? Tlhama",
        haveAccount = "O na le akhaonto? Tsena"
    )
    else -> AuthCopy(
        welcome = "Welcome Back",
        subtitle = "Sign in to continue to Nexus",
        email = "Email",
        password = "Password",
        signIn = "Sign In",
        createAccount = "Create Account",
        registerSubtitle = "Get started with your Nexus profile",
        displayName = "Display Name",
        signingIn = "Signing in…",
        creatingAccount = "Creating account…",
        needAccount = "Need an account? Register",
        haveAccount = "Already have an account? Login"
    )
}

data class AuthCopy(
    val welcome: String,
    val subtitle: String,
    val email: String,
    val password: String,
    val signIn: String,
    val createAccount: String,
    val registerSubtitle: String,
    val displayName: String,
    val signingIn: String,
    val creatingAccount: String,
    val needAccount: String,
    val haveAccount: String
)
