package com.gymwatch.adapters.driven.persistence

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gymwatch.core.domain.model.ExerciseKind
import com.gymwatch.core.domain.model.Favourites
import com.gymwatch.core.domain.port.FavouritesRepositoryPort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class DataStoreFavouritesRepository(
    private val dataStore: DataStore<Preferences>,
) : FavouritesRepositoryPort {

    override val favourites: Flow<Favourites> = dataStore.data.map { prefs ->
        val stored = prefs[KINDS] ?: return@map Favourites.DEFAULT
        // Names are decoded defensively: an enum constant removed in a later
        // version must not crash the app on someone's stored preferences.
        val kinds = stored.split(SEPARATOR)
            .mapNotNull { name -> ExerciseKind.entries.firstOrNull { it.name == name } }
        if (kinds.isEmpty()) Favourites.DEFAULT else Favourites.of(kinds)
    }

    override suspend fun save(favourites: Favourites) {
        dataStore.edit { prefs ->
            prefs[KINDS] = favourites.kinds.joinToString(SEPARATOR) { it.name }
        }
    }

    private companion object {
        val KINDS = stringPreferencesKey("favourite_kinds")
        const val SEPARATOR = ","
    }
}
