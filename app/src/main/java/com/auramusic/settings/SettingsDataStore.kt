package com.auramusic.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.auroraDataStore by preferencesDataStore(name = "aurora_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore get() = context.auroraDataStore

    val recentSearches: Flow<List<String>> = dataStore.data.map { prefs ->
        (prefs[RECENT_SEARCHES] ?: emptySet()).toList()
    }

    val playbackSpeed: Flow<Float> = dataStore.data.map { prefs ->
        prefs[PLAYBACK_SPEED] ?: 1f
    }

    val sleepTimerMinutes: Flow<Int> = dataStore.data.map { prefs ->
        prefs[SLEEP_TIMER_MINUTES] ?: 0
    }

    val audioQuality: Flow<String> = dataStore.data.map { prefs ->
        prefs[AUDIO_QUALITY] ?: "AUTO"
    }

    val shuffle: Flow<Boolean> = dataStore.data.map { prefs -> prefs[SHUFFLE] ?: false }

    val repeatMode: Flow<Int> = dataStore.data.map { prefs -> prefs[REPEAT_MODE] ?: 0 }

    suspend fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        dataStore.edit { prefs ->
            val current = (prefs[RECENT_SEARCHES] ?: emptySet()).toMutableList()
            current.remove(trimmed)
            current.add(0, trimmed)
            prefs[RECENT_SEARCHES] = current.take(12).toSet()
        }
    }

    suspend fun clearRecentSearches() = dataStore.edit { it.remove(RECENT_SEARCHES) }

    suspend fun setPlaybackSpeed(speed: Float) = dataStore.edit { it[PLAYBACK_SPEED] = speed }

    suspend fun setSleepTimer(minutes: Int) = dataStore.edit { it[SLEEP_TIMER_MINUTES] = minutes }

    suspend fun setAudioQuality(quality: String) = dataStore.edit { it[AUDIO_QUALITY] = quality }

    suspend fun setShuffle(enabled: Boolean) = dataStore.edit { it[SHUFFLE] = enabled }

    suspend fun setRepeatMode(mode: Int) = dataStore.edit { it[REPEAT_MODE] = mode }

    private companion object {
        val RECENT_SEARCHES = stringSetPreferencesKey("recent_searches")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val SLEEP_TIMER_MINUTES = intPreferencesKey("sleep_timer_minutes")
        val AUDIO_QUALITY = stringPreferencesKey("audio_quality")
        val SHUFFLE = booleanPreferencesKey("shuffle")
        val REPEAT_MODE = intPreferencesKey("repeat_mode")
    }
}
