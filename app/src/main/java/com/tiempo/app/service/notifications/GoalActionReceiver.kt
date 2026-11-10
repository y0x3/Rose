package com.tiempo.app.service.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tiempo.app.domain.usecase.CompleteGoalUseCase
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GoalActionReceiver : BroadcastReceiver() {

    @Inject lateinit var completeGoal: CompleteGoalUseCase
    @Inject lateinit var notifier: GoalNotifier

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != GoalNotifier.ACTION_DONE) return

        val goalId = intent.getLongExtra(GoalNotifier.EXTRA_GOAL_ID, -1L)
        val epochDay = intent.getLongExtra(GoalNotifier.EXTRA_EPOCH_DAY, Long.MIN_VALUE)
        if (goalId < 0 || epochDay == Long.MIN_VALUE) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                completeGoal(goalId, LocalDate.ofEpochDay(epochDay))
                notifier.cancel(goalId)
            } finally {
                pending.finish()
            }
        }
    }
}