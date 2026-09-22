package com.example.nexus.ui

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

fun AuthCopy.language(code: String): AuthCopy = when (code) {
    "zu" -> AuthCopy("Siyakwamukela", "Ngena ukuze uqhubeke ku-Nexus", "I-imeyili", "Iphasiwedi", "Ngena", "Dala i-akhawunti")
    "tn" -> AuthCopy("Re a go amogela", "Tsena go tswelela mo Nexus", "Imeile", "Khunololamorago", "Tsena", "Tlhama akhaonto")
    else -> AuthCopy("Welcome Back", "Sign in to continue to Nexus", "Email", "Password", "Sign In", "Create Account")
}

data class AuthCopy(
    val welcome: String,
    val subtitle: String,
    val email: String,
    val password: String,
    val signIn: String,
    val createAccount: String
)
