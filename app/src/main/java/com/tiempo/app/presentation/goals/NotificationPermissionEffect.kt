package com.tiempo.app.presentation.goals

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun NotificationPermissionEffect() {
    val context = LocalContext.current

    // 1) Permiso de notificaciones (Android 13+)
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* si lo rechaza, simplemente no verá avisos */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // 2) Alarmas exactas (Android 12+)
    var showExactDialog by remember { mutableStateOf(needsExactAlarmPermission(context)) }
    if (showExactDialog) {
        AlertDialog(
            onDismissRequest = { showExactDialog = false },
            title = { Text("Permitir alarmas exactas") },
            text = {
                Text(
                    "Para que los avisos suenen a la hora exacta, activa " +
                            "\"Alarmas y recordatorios\" para esta app."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showExactDialog = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                }) { Text("Abrir ajustes") }
            },
            dismissButton = {
                TextButton(onClick = { showExactDialog = false }) { Text("Ahora no") }
            }
        )
    }
}

private fun needsExactAlarmPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    return !alarmManager.canScheduleExactAlarms()
}