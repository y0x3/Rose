package com.tiempo.app.service.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.tiempo.app.domain.model.CompletionStatus
import com.tiempo.app.domain.model.Goal
import com.tiempo.app.domain.repository.GoalRepository
import com.tiempo.app.domain.usecase.CompleteGoalUseCase
import com.tiempo.app.presentation.alarm.AlarmActivity
import com.tiempo.app.service.scheduling.GoalAlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AlarmService : Service() {

    companion object {
        const val ACTION_START = "com.tiempo.app.alarm.START"
        const val ACTION_DONE = "com.tiempo.app.alarm.DONE"
        const val ACTION_SNOOZE = "com.tiempo.app.alarm.SNOOZE"
        const val ACTION_SKIP = "com.tiempo.app.alarm.SKIP"

        private const val EXTRA_GOAL_ID = "goal_id"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_NOTE = "note"
        private const val EXTRA_EPOCH_DAY = "epoch_day"
        private const val EXTRA_SOUND = "sound"

        private const val CHANNEL_ALARM = "goals_alarm"
        private const val NOTIFICATION_ID = 9001
        private const val SNOOZE_MINUTES = 10

        /** Por seguridad la alarma se corta sola a los 10 minutos. Cámbialo si quieres. */
        private const val MAX_RING_MS = 10 * 60 * 1000L

        fun start(context: Context, goal: Goal, date: LocalDate) {
            val intent = Intent(context, AlarmService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_GOAL_ID, goal.id)
                .putExtra(EXTRA_TITLE, goal.title)
                .putExtra(EXTRA_NOTE, goal.note)
                .putExtra(EXTRA_EPOCH_DAY, date.toEpochDay())
                .putExtra(EXTRA_SOUND, goal.soundUri)
            ContextCompat.startForegroundService(context, intent)
        }

        fun action(context: Context, action: String): Intent =
            Intent(context, AlarmService::class.java).setAction(action)
    }

    @Inject lateinit var completeGoal: CompleteGoalUseCase
    @Inject lateinit var repository: GoalRepository
    @Inject lateinit var scheduler: GoalAlarmScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable { stopAlarm() }

    private var player: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startAlarm(intent)
            ACTION_DONE -> resolve(Resolution.DONE)
            ACTION_SNOOZE -> resolve(Resolution.SNOOZE)
            ACTION_SKIP -> resolve(Resolution.SKIP)
            else -> stopAlarm()
        }
        return START_NOT_STICKY
    }

    // ---------------------------------------------------------------- inicio

    private fun startAlarm(intent: Intent) {
        val info = AlarmInfo(
            goalId = intent.getLongExtra(EXTRA_GOAL_ID, -1L),
            title = intent.getStringExtra(EXTRA_TITLE).orEmpty(),
            note = intent.getStringExtra(EXTRA_NOTE),
            epochDay = intent.getLongExtra(EXTRA_EPOCH_DAY, 0L)
        )
        if (info.goalId < 0) {
            stopSelf()
            return
        }

        // Si ya sonaba otra alarma, se reemplaza por esta
        releasePlayer()
        AlarmState.current.value = info

        createChannel()
        val notification = buildNotification(info)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startSound(intent.getStringExtra(EXTRA_SOUND))
        startVibration()

        handler.removeCallbacks(timeout)
        handler.postDelayed(timeout, MAX_RING_MS)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ALARM,
            "Alarmas",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alarmas que suenan hasta que las apagas."
            setSound(null, null)          // el sonido lo reproduce el servicio
            enableVibration(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(info: AlarmInfo): android.app.Notification {
        val fullScreen = PendingIntent.getActivity(
            this,
            1,
            Intent(this, AlarmActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        fun servicePending(action: String, code: Int) = PendingIntent.getService(
            this,
            code,
            action(this, action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ALARM)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(info.title)
            .setContentText(info.note?.takeIf { it.isNotBlank() } ?: "Alarma")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(fullScreen)
            .setFullScreenIntent(fullScreen, true)
            .addAction(0, "Hecho ✓", servicePending(ACTION_DONE, 2))
            .addAction(0, "Posponer $SNOOZE_MINUTES min", servicePending(ACTION_SNOOZE, 3))
            .build()
    }

    // ---------------------------------------------------------------- sonido

    private fun startSound(soundUri: String?) {
        val candidates = listOfNotNull(
            soundUri?.let { Uri.parse(it) },
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        )
        for (uri in candidates) {
            val ok = runCatching {
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(this@AlarmService, uri)
                    isLooping = true
                    prepare()
                    start()
                }
                player = mp
            }.isSuccess
            if (ok) return
        }
    }

    @Suppress("DEPRECATION")
    private fun startVibration() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        val pattern = longArrayOf(0, 800, 600)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            vibrator.vibrate(pattern, 0)
        }
    }

    // ----------------------------------------------------------- resolución

    private enum class Resolution { DONE, SNOOZE, SKIP }

    private fun resolve(resolution: Resolution) {
        val info = AlarmState.current.value
        if (info == null) {
            stopAlarm()
            return
        }
        scope.launch {
            val date = LocalDate.ofEpochDay(info.epochDay)
            when (resolution) {
                Resolution.DONE -> {
                    completeGoal(info.goalId, date)
                    scheduler.cancelSnooze(info.goalId)
                }
                Resolution.SKIP -> {
                    repository.setCompletion(info.goalId, date, CompletionStatus.SKIPPED)
                    scheduler.cancelSnooze(info.goalId)
                }
                Resolution.SNOOZE -> scheduler.snooze(info.goalId, SNOOZE_MINUTES)
            }
            stopAlarm()
        }
    }

    private fun stopAlarm() {
        handler.removeCallbacks(timeout)
        releasePlayer()
        AlarmState.current.value = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    @Suppress("DEPRECATION")
    private fun releasePlayer() {
        runCatching { player?.stop() }
        player?.release()
        player = null
        (getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.cancel()
    }

    override fun onDestroy() {
        handler.removeCallbacks(timeout)
        releasePlayer()
        scope.cancel()
        super.onDestroy()
    }
}