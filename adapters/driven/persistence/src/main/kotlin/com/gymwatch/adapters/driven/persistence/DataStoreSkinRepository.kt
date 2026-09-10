package com.gymwatch.adapters.driven.persistence

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gymwatch.core.domain.model.Skin
import com.gymwatch.core.domain.port.SkinRepositoryPort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The selected colour scheme, stored by enum name.
 *
 * The name rather than the ordinal, so reordering the enum cannot silently
 * repaint someone's watch, and [Skin.of] turns anything unrecognised back into
 * the default instead of throwing.
 */
internal class DataStoreSkinRepository(
    private val dataStore: DataStore<Preferences>,
) : SkinRepositoryPort {

    override val skin: Flow<Skin> = dataStore.data.map { prefs -> Skin.of(prefs[SKIN]) }

    override suspend fun save(skin: Skin) {
        dataStore.edit { prefs -> prefs[SKIN] = skin.name }
    }

    private companion object {
        val SKIN = stringPreferencesKey("skin")
    }
}
