package com.gymwatch.core.domain.port

import com.gymwatch.core.domain.model.Counter
import com.gymwatch.core.domain.model.Profiles
import com.gymwatch.core.domain.model.ScreenLayout
import kotlinx.coroutines.flow.Flow

interface CounterRepositoryPort {
    val counter: Flow<Counter>
    suspend fun save(counter: Counter)
}

/**
 * The three profiles and the active one.
 *
 * Rest lengths live in here rather than in a settings port of their own,
 * because they belong to a profile: the rest you take between heavy sets is not
 * the rest you take between intervals. Only the *configured* lengths are
 * persisted, never a running countdown — restoring one after a reboot would
 * show a deadline that no longer means anything.
 */
interface ProfilesRepositoryPort {
    val profiles: Flow<Profiles>
    suspend fun save(profiles: Profiles)
}

interface ScreenLayoutRepositoryPort {
    val layout: Flow<ScreenLayout>
    suspend fun save(layout: ScreenLayout)
}
