package com.example.nexus.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.nexus.settings.SettingsSession
import com.example.nexus.ui.components.InlineErrorState
import com.example.nexus.ui.components.NexusLogo
import com.example.nexus.ui.components.NexusPrimaryButton
import com.example.nexus.ui.settings.LanguagePicker
import com.example.nexus.ui.settings.authCopy
import com.example.nexus.ui.theme.Spacing

@Composable
fun LoginScreen(
    uiState: AuthUiState,
    onLogin: (String, String) -> Unit,
    onNavigateToRegister: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val language by SettingsSession.language.collectAsState()
    val copy = authCopy(language)

    Surface(
        modifier =
            Modifier
                .fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .testTag("login_content")
                    .padding(Spacing.lg),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NexusLogo(iconSize = Spacing.AuthLogoSize, textSize = Spacing.AuthLogoTextSize)
            LanguagePicker(
                selectedLanguage = language,
                onLanguageSelected = SettingsSession::setLanguage,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(Spacing.xl))

            Text(
                copy.welcome,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start),
            )
            Text(
                copy.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start),
            )
            Spacer(modifier = Modifier.height(Spacing.lg))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(copy.email) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
            )

            Spacer(modifier = Modifier.height(Spacing.smd))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(copy.password) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                keyboardActions =
                    KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            onLogin(email.trim(), password)
                        },
                    ),
            )

            if (!uiState.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(Spacing.smd))
                InlineErrorState(message = uiState.errorMessage)
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            NexusPrimaryButton(
                text = if (uiState.isLoading) copy.signingIn else copy.signIn,
                onClick = {
                    focusManager.clearFocus()
                    onLogin(email.trim(), password)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && email.isNotBlank() && password.isNotBlank(),
                isLoading = uiState.isLoading,
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            TextButton(
                onClick = onNavigateToRegister,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(copy.needAccount)
            }
        }
    }
}
