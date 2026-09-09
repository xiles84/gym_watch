package com.gymwatch.core.domain.port

import com.gymwatch.core.domain.model.Counter
import com.gymwatch.core.domain.model.Favourites
import com.gymwatch.core.domain.model.RestTimer
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

interface CounterRepositoryPort {
    val counter: Flow<Counter>
    suspend fun save(counter: Counter)
}

interface FavouritesRepositoryPort {
    val favourites: Flow<Favourites>
    suspend fun save(favourites: Favourites)
}

/**
 * Only the *configured length* is persisted, not a running countdown — a rest
 * timer that survives a reboot would be lying about when it started.
 */
interface RestTimerSettingsPort {
    val duration: Flow<Duration>
    suspend fun save(duration: Duration)

    companion object {
        val FALLBACK = RestTimer.DEFAULT
    }
}
