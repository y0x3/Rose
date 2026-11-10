package com.tiempo.app.service.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.tiempo.app.domain.model.Goal
import com.tiempo.app.domain.model.nextOccurrence
import com.tiempo.app.domain.repository.GoalRepository
import com.tiempo.app.service.notifications.GoalAlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class GoalAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: GoalRepository,
    private val clock: Clock
) {
    companion object {
        const val EXTRA_GOAL_ID = "goal_id"
        const val EXTRA_SNOOZE = "snooze"
        private const val SNOOZE_CODE_BASE = 100_000_000
    }

    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /** Programa el próximo aviso de la meta (cancela el anterior si existía). */
    fun schedule(goal: Goal) {
        cancel(goal.id)
        if (goal.archived) return

        val next = goal.nextOccurrence(LocalDateTime.now(clock)) ?: return
        val triggerAt = next.atZone(clock.zone).toInstant().toEpochMilli()
        setAlarm(triggerAt, regularIntent(goal.id))
    }

    /** Vuelve a avisar dentro de [minutes] minutos (alarma pospuesta). */
    fun snooze(goalId: Long, minutes: Int = 10) {
        val triggerAt = clock.millis() + minutes * 60_000L
        setAlarm(triggerAt, snoozeIntent(goalId))
    }

    fun cancelSnooze(goalId: Long) {
        val pending = snoozeIntent(goalId)
        alarmManager.cancel(pending)
        pending.cancel()
    }

    /** Cancela el aviso regular y también un posponer pendiente. */
    fun cancel(goalId: Long) {
        val pending = regularIntent(goalId)
        alarmManager.cancel(pending)
        pending.cancel()
        cancelSnooze(goalId)
    }

    /** Se usa al abrir la app y al reiniciar el teléfono. */
    suspend fun rescheduleAll() {
        repository.observeGoals().first().forEach { schedule(it) }
    }

    private fun setAlarm(triggerAt: Long, pending: PendingIntent) {
        try {
            if (canScheduleExact()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    private fun regularIntent(goalId: Long): PendingIntent {
        val intent = Intent(context, GoalAlarmReceiver::class.java)
            .putExtra(EXTRA_GOAL_ID, goalId)
        return PendingIntent.getBroadcast(
            context,
            goalId.toInt() * 3 + 2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun snoozeIntent(goalId: Long): PendingIntent {
        val intent = Intent(context, GoalAlarmReceiver::class.java)
            .putExtra(EXTRA_GOAL_ID, goalId)
            .putExtra(EXTRA_SNOOZE, true)
        return PendingIntent.getBroadcast(
            context,
            SNOOZE_CODE_BASE + goalId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}