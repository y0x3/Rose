package com.tiempo.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tiempo.app.domain.lifepanel.TimeScale
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.lifePanelStore by preferencesDataStore(name = "life_panel")

@Singleton
class ScalePreference @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val key = stringPreferencesKey("scale")

    val scale: Flow<TimeScale> = context.lifePanelStore.data.map { prefs ->
        prefs[key]?.let { runCatching { TimeScale.valueOf(it) }.getOrNull() } ?: TimeScale.WEEKS
    }

    suspend fun setScale(scale: TimeScale) {
        context.lifePanelStore.edit { it[key] = scale.name }
    }
}