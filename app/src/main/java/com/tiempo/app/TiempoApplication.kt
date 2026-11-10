package com.tiempo.app

import android.app.Application
import com.tiempo.app.service.scheduling.GoalAlarmScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class TiempoApplication : Application() {

    @Inject lateinit var goalAlarmScheduler: GoalAlarmScheduler

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.Default).launch {
            goalAlarmScheduler.rescheduleAll()
        }
    }
}