package com.gymwatch.adapters.driven.persistence

import android.content.Context
import com.gymwatch.core.domain.port.CounterRepositoryPort
import com.gymwatch.core.domain.port.FavouritesRepositoryPort
import com.gymwatch.core.domain.port.RestTimerSettingsPort

/**
 * The persistence module's only public surface.
 *
 * The composition root gets ports back and never learns that DataStore is what
 * is behind them — which is what makes swapping the storage a one-module change.
 * It also guarantees the single DataStore instance is shared, since opening the
 * same file twice in a process throws.
 */
class PersistenceAdapters(context: Context) {
    private val store = context.applicationContext.gymDataStore

    val counters: CounterRepositoryPort = DataStoreCounterRepository(store)
    val favourites: FavouritesRepositoryPort = DataStoreFavouritesRepository(store)
    val restTimerSettings: RestTimerSettingsPort = DataStoreRestTimerSettings(store)
}
