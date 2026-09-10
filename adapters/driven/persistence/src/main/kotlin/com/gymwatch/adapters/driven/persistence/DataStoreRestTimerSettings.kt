package com.gymwatch.adapters.driven.persistence

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.gymwatch.core.domain.model.RestTimer
import com.gymwatch.core.domain.port.RestTimerSettingsPort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Persists the configured rest length only.
 *
 * A running countdown is deliberately not stored: restoring one after a reboot
 * would show a deadline that no longer means anything.
 */
internal class DataStoreRestTimerSettings(
    private val dataStore: DataStore<Preferences>,
) : RestTimerSettingsPort {

    override val duration: Flow<Duration> = dataStore.data.map { prefs ->
        prefs[SECONDS]?.seconds ?: RestTimer.DEFAULT
    }

    override suspend fun save(duration: Duration) {
        dataStore.edit { prefs -> prefs[SECONDS] = duration.inWholeSeconds }
    }

    private companion object {
        val SECONDS = longPreferencesKey("rest_seconds")
    }
}
