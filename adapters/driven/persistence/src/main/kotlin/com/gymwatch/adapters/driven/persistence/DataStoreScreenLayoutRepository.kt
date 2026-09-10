package com.gymwatch.adapters.driven.persistence

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gymwatch.core.domain.model.AppScreen
import com.gymwatch.core.domain.model.ScreenLayout
import com.gymwatch.core.domain.port.ScreenLayoutRepositoryPort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Which screens are shown and in what order.
 *
 * Names are decoded through [ScreenLayout.of], which appends any screen the
 * stored order does not mention — so a screen added in a later version appears
 * for existing installs instead of silently never showing up.
 */
internal class DataStoreScreenLayoutRepository(
    private val dataStore: DataStore<Preferences>,
) : ScreenLayoutRepositoryPort {

    override val layout: Flow<ScreenLayout> = dataStore.data.map { prefs ->
        val order = prefs[ORDER]?.split(SEPARATOR)?.mapNotNull(::decode)
            ?: return@map ScreenLayout.DEFAULT
        val hidden = prefs[HIDDEN]?.split(SEPARATOR)?.mapNotNull(::decode)?.toSet().orEmpty()
        ScreenLayout.of(order, hidden)
    }

    override suspend fun save(layout: ScreenLayout) {
        dataStore.edit { prefs ->
            prefs[ORDER] = layout.order.joinToString(SEPARATOR) { it.name }
            prefs[HIDDEN] = layout.hidden.joinToString(SEPARATOR) { it.name }
        }
    }

    private fun decode(name: String): AppScreen? =
        AppScreen.entries.firstOrNull { it.name == name }

    private companion object {
        val ORDER = stringPreferencesKey("screen_order")
        val HIDDEN = stringPreferencesKey("screen_hidden")
        const val SEPARATOR = ","
    }
}
