package com.tiempo.app.service.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tiempo.app.MainActivity
import com.tiempo.app.domain.model.AlertType
import com.tiempo.app.domain.model.Goal
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_SIMPLE = "goals_simple"
        const val CHANNEL_MESSAGE = "goals_message"
        const val ACTION_DONE = "com.tiempo.app.action.GOAL_DONE"
        const val EXTRA_GOAL_ID = "goal_id"
        const val EXTRA_EPOCH_DAY = "epoch_day"
    }

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SIMPLE,
                "Alerta simple",
                NotificationManager.IMPORTANCE_DEFAULT   // suena, sin globo emergente
            ).apply { description = "Solo un sonido, sin mensaje emergente." }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MESSAGE,
                "Sonido con mensaje",
                NotificationManager.IMPORTANCE_HIGH      // suena y muestra globo
            ).apply { description = "Suena y muestra el texto de la meta." }
        )
    }

    @SuppressLint("MissingPermission")
    fun show(goal: Goal, date: LocalDate) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val notificationId = goal.id.toInt()

        val openApp = PendingIntent.getActivity(
            context,
            notificationId * 3,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent(context, GoalActionReceiver::class.java).apply {
            action = ACTION_DONE
            putExtra(EXTRA_GOAL_ID, goal.id)
            putExtra(EXTRA_EPOCH_DAY, date.toEpochDay())
        }
        val donePending = PendingIntent.getBroadcast(
            context,
            notificationId * 3 + 1,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isSimple = goal.alertType == AlertType.SIMPLE
        val channel = if (isSimple) CHANNEL_SIMPLE else CHANNEL_MESSAGE

        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(goal.title)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(0, "Hecho ✓", donePending)

        if (isSimple) {
            builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
        } else {
            builder.setPriority(NotificationCompat.PRIORITY_HIGH)
            goal.note?.takeIf { it.isNotBlank() }?.let {
                builder.setContentText(it)
                builder.setStyle(NotificationCompat.BigTextStyle().bigText(it))
            }
        }

        manager.notify(notificationId, builder.build())
    }

    fun cancel(goalId: Long) {
        NotificationManagerCompat.from(context).cancel(goalId.toInt())
    }
}