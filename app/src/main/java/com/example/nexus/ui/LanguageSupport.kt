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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.nexus.R

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
            label = { Text(stringResource(R.string.language_picker_label)) },
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

@Composable
fun LanguagePill(modifier: Modifier = Modifier) {
    val currentLanguage by com.example.nexus.settings.SettingsSession.language.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    val code = (supportedLanguages.firstOrNull { it.first == currentLanguage }?.first ?: "en").uppercase()
    Box(modifier) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(percent = 50))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = code,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            supportedLanguages.forEach { (lcode, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        com.example.nexus.settings.SettingsSession.setLanguage(lcode)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Auth UI copy for the in-app language [code].
 *
 * English resolves from res/values/strings.xml via [stringResource] so it participates in the
 * app's Android localization setup. isiZulu/Setswana stay as in-code bundles for now because the
 * pre-auth LanguagePicker drives SettingsSession.language directly — independent of the system
 * locale that [stringResource] observes — and no values-zu/values-tn variants exist yet.
 * TODO(i18n): move these bundles into values-zu/ and values-tn/ strings.xml once locale-aware
 * resource loading (createConfigurationContext) is wired up.
 */
@Composable
fun authCopy(code: String): AuthCopy = when (code) {
    "zu" -> isiZuluAuthCopy()
    "tn" -> setswanaAuthCopy()
    else -> AuthCopy(
        welcome = stringResource(R.string.auth_welcome),
        subtitle = stringResource(R.string.login_subtitle),
        email = stringResource(R.string.auth_email),
        password = stringResource(R.string.auth_password),
        signIn = stringResource(R.string.auth_sign_in),
        createAccount = stringResource(R.string.auth_create_account),
        registerSubtitle = stringResource(R.string.register_subtitle),
        displayName = stringResource(R.string.auth_display_name),
        signingIn = stringResource(R.string.auth_signing_in),
        creatingAccount = stringResource(R.string.auth_creating_account),
        needAccount = stringResource(R.string.login_prompt_register),
        haveAccount = stringResource(R.string.register_prompt_login)
    )
}

private fun isiZuluAuthCopy(): AuthCopy = AuthCopy(
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
private fun setswanaAuthCopy(): AuthCopy = AuthCopy(
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
