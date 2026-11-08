package com.tiempo.app.presentation.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tiempo.app.R
import com.tiempo.app.presentation.components.PlaceholderScreen

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.nav_settings),
        description = stringResource(R.string.placeholder_settings),
        icon = Icons.Outlined.Settings,
        modifier = modifier
    )
}
