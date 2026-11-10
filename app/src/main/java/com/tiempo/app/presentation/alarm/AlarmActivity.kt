package com.tiempo.app.presentation.alarm

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiempo.app.service.alarm.AlarmService
import com.tiempo.app.service.alarm.AlarmState
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            MaterialTheme {
                AlarmScreen(onAction = { startService(AlarmService.action(this, it)) })
            }
        }
    }
}

@Composable
private fun AlarmScreen(onAction: (String) -> Unit) {
    val info by AlarmState.current.collectAsStateWithLifecycle()
    val activity = androidx.compose.ui.platform.LocalContext.current as ComponentActivity

    // Cuando la alarma se apaga (desde aquí o desde la notificación), se cierra la pantalla
    LaunchedEffect(info) {
        if (info == null) activity.finish()
    }

    val now = remember { LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF101418)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("⏰", fontSize = 64.sp)
            Spacer(Modifier.height(8.dp))
            Text(now, fontSize = 56.sp, fontWeight = FontWeight.Light, color = Color.White)
            Spacer(Modifier.height(24.dp))
            Text(
                text = info?.title.orEmpty(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            info?.note?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = Color(0xFFB0B8C0), textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(48.dp))

            Button(
                onClick = { onAction(AlarmService.ACTION_DONE) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text("Hecho ✓", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = { onAction(AlarmService.ACTION_SNOOZE) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Posponer 10 min", color = Color.White)
            }

            TextButton(onClick = { onAction(AlarmService.ACTION_SKIP) }) {
                Text("Omitir hoy", color = Color(0xFFB0B8C0))
            }
        }
    }
}