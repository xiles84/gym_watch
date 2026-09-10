package com.gymwatch.adapters.driven.persistence

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gymwatch.core.domain.model.Counter
import com.gymwatch.core.domain.port.CounterRepositoryPort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class DataStoreCounterRepository(
    private val dataStore: DataStore<Preferences>,
) : CounterRepositoryPort {

    override val counter: Flow<Counter> = dataStore.data.map { prefs ->
        Counter(
            value = prefs[VALUE] ?: 0,
            label = prefs[LABEL] ?: Counter.DEFAULT_LABEL,
        )
    }

    override suspend fun save(counter: Counter) {
        dataStore.edit { prefs ->
            prefs[VALUE] = counter.value
            prefs[LABEL] = counter.label
        }
    }

    private companion object {
        val VALUE = intPreferencesKey("counter_value")
        val LABEL = stringPreferencesKey("counter_label")
    }
}
