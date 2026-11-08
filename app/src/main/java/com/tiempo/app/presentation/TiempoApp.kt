package com.tiempo.app.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiempo.app.presentation.components.LoadingScreen
import com.tiempo.app.presentation.navigation.MainScreen
import com.tiempo.app.presentation.onboarding.OnboardingScreen

@Composable
fun TiempoApp(
    appViewModel: AppViewModel = hiltViewModel()
) {
    val uiState by appViewModel.uiState.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (uiState) {
            AppUiState.Loading -> LoadingScreen()
            AppUiState.NeedsOnboarding -> OnboardingScreen()
            AppUiState.Ready -> MainScreen()
        }
    }
}
