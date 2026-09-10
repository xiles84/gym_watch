package com.gymwatch.adapters.driven.persistence

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.gymwatch.core.domain.model.Counter
import com.gymwatch.core.domain.port.CounterRepositoryPort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Only the count is stored. The counter always counts sets, so there is no
 * label to persist.
 */
internal class DataStoreCounterRepository(
    private val dataStore: DataStore<Preferences>,
) : CounterRepositoryPort {

    override val counter: Flow<Counter> = dataStore.data.map { prefs ->
        Counter(value = prefs[VALUE] ?: 0)
    }

    override suspend fun save(counter: Counter) {
        dataStore.edit { prefs -> prefs[VALUE] = counter.value }
    }

    private companion object {
        val VALUE = intPreferencesKey("counter_value")
    }
}
