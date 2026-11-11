package com.tiempo.app.service.assistant

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AssistantMode { KEYWORDS, LOCAL, CLOUD }

@Singleton
class AssistantSettings @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("assistant_settings", Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(load())
    val mode: StateFlow<AssistantMode> = _mode.asStateFlow()

    fun setMode(mode: AssistantMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
        _mode.value = mode
    }

    private fun load(): AssistantMode =
        runCatching { AssistantMode.valueOf(prefs.getString(KEY_MODE, null) ?: "") }
            .getOrDefault(AssistantMode.KEYWORDS)

    private companion object {
        const val KEY_MODE = "mode"
    }
}