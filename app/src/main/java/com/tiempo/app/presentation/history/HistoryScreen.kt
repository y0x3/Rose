package com.tiempo.app.presentation.history

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tiempo.app.R
import com.tiempo.app.presentation.components.PlaceholderScreen

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.nav_history),
        description = stringResource(R.string.placeholder_history),
        icon = Icons.Outlined.History,
        modifier = modifier
    )
}
