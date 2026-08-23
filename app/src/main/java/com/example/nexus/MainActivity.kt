package com.example.nexus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nexus.ui.DashboardScreen
import com.example.nexus.ui.LoginScreen
import com.example.nexus.ui.RegisterScreen
import com.example.nexus.ui.AuthViewModel
import com.example.nexus.ui.theme.NexusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NexusTheme {
                NexusAppContent()
            }
        }
    }
}

@Composable
fun NexusAppContent(authViewModel: AuthViewModel = viewModel()) {
    val uiState by authViewModel.uiState.collectAsState()
    var showRegister by remember { mutableStateOf(false) }

    when {
        uiState.isAuthenticated -> DashboardScreen()
        showRegister -> RegisterScreen(
            uiState = uiState,
            onRegister = { email, password, displayName ->
                authViewModel.register(email, password, displayName)
            },
            onNavigateToLogin = { showRegister = false }
        )
        else -> LoginScreen(
            uiState = uiState,
            onLogin = { email, password ->
                authViewModel.login(email, password)
            },
            onNavigateToRegister = { showRegister = true }
        )
    }
}