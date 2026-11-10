package com.tiempo.app.service.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tiempo.app.domain.model.AlertType
import com.tiempo.app.domain.model.Goal
import com.tiempo.app.domain.model.occursOn
import com.tiempo.app.domain.repository.GoalRepository
import com.tiempo.app.service.alarm.AlarmService
import com.tiempo.app.service.scheduling.GoalAlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GoalAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: GoalRepository
    @Inject lateinit var notifier: GoalNotifier
    @Inject lateinit var scheduler: GoalAlarmScheduler
    @Inject lateinit var clock: Clock

    override fun onReceive(context: Context, intent: Intent) {
        val goalId = intent.getLongExtra(GoalAlarmScheduler.EXTRA_GOAL_ID, -1L)
        if (goalId < 0) return
        val isSnooze = intent.getBooleanExtra(GoalAlarmScheduler.EXTRA_SNOOZE, false)

        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val goal = repository.getGoal(goalId) ?: return@launch
                val today = LocalDate.now(clock)

                // Un aviso pospuesto suena aunque la meta no toque hoy (ya tocaba cuando se pospuso)
                if (!goal.archived && (isSnooze || goal.occursOn(today))) {
                    val alreadyHandled = repository.observeCompletions(today, today).first()
                        .any { it.goalId == goalId }
                    if (!alreadyHandled) fire(context, goal, today)
                }

                // Deja programado el siguiente aviso
                scheduler.schedule(goal)
            } finally {
                pending.finish()
            }
        }
    }

    private fun fire(context: Context, goal: Goal, date: LocalDate) {
        if (goal.alertType == AlertType.ALARM) {
            try {
                AlarmService.start(context, goal, date)
            } catch (e: Exception) {
                // Si Android no deja iniciar el servicio, al menos avisa con notificación
                notifier.show(goal, date)
            }
        } else {
            notifier.show(goal, date)
        }
    }
}