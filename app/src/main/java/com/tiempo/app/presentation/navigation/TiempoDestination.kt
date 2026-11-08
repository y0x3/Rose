package com.tiempo.app.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.tiempo.app.R

enum class TiempoDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector
) {
    HOME("home", R.string.nav_home, Icons.Outlined.Home),
    LIFE_PANEL("life_panel", R.string.nav_life_panel, Icons.Outlined.HourglassBottom),
    GOALS("goals", R.string.nav_goals, Icons.Outlined.Flag),
    HISTORY("history", R.string.nav_history, Icons.Outlined.History),
    SETTINGS("settings", R.string.nav_settings, Icons.Outlined.Settings);

    companion object {
        val startDestination: TiempoDestination = HOME
    }
}
