package com.tiempo.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.tiempo.app.presentation.goals.GoalsScreen
import com.tiempo.app.presentation.history.HistoryScreen
import com.tiempo.app.presentation.home.HomeScreen
import com.tiempo.app.presentation.lifepanel.LifePanelScreen
import com.tiempo.app.presentation.settings.SettingsScreen

@Composable
fun TiempoNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = TiempoDestination.startDestination.route,
        modifier = modifier
    ) {
        composable(TiempoDestination.HOME.route) {
            HomeScreen()
        }
        composable(TiempoDestination.LIFE_PANEL.route) {
            LifePanelScreen()
        }
        composable(TiempoDestination.GOALS.route) {
            GoalsScreen()
        }
        composable(TiempoDestination.HISTORY.route) {
            HistoryScreen()
        }
        composable(TiempoDestination.SETTINGS.route) {
            SettingsScreen()
        }
    }
}
