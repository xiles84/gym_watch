package com.gymwatch.adapters.driven.persistence

import android.content.Context
import com.gymwatch.core.domain.port.CounterRepositoryPort
import com.gymwatch.core.domain.port.ScreenLayoutRepositoryPort
import com.gymwatch.core.domain.port.SkinRepositoryPort
import com.gymwatch.core.domain.port.WorkoutSetupRepositoryPort

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
    val workoutSetup: WorkoutSetupRepositoryPort = DataStoreWorkoutSetupRepository(store)
    val screenLayout: ScreenLayoutRepositoryPort = DataStoreScreenLayoutRepository(store)
    val skins: SkinRepositoryPort = DataStoreSkinRepository(store)
}
