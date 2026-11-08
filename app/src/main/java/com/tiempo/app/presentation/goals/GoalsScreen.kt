package com.tiempo.app.presentation.goals

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tiempo.app.R
import com.tiempo.app.presentation.components.PlaceholderScreen

@Composable
fun GoalsScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.nav_goals),
        description = stringResource(R.string.placeholder_goals),
        icon = Icons.Outlined.Flag,
        modifier = modifier
    )
}
