package com.gymwatch.core.domain.port

import com.gymwatch.core.domain.model.Counter
import com.gymwatch.core.domain.model.ScreenLayout
import com.gymwatch.core.domain.model.Skin
import com.gymwatch.core.domain.model.WorkoutSetup
import kotlinx.coroutines.flow.Flow

interface CounterRepositoryPort {
    val counter: Flow<Counter>
    suspend fun save(counter: Counter)
}

/**
 * The three workout shortcuts and the rest presets.
 *
 * Only the *configured* rest lengths are persisted, never a running countdown —
 * restoring one after a reboot would show a deadline that no longer means
 * anything.
 */
interface WorkoutSetupRepositoryPort {
    val setup: Flow<WorkoutSetup>
    suspend fun save(setup: WorkoutSetup)
}

interface ScreenLayoutRepositoryPort {
    val layout: Flow<ScreenLayout>
    suspend fun save(layout: ScreenLayout)
}

interface SkinRepositoryPort {
    val skin: Flow<Skin>
    suspend fun save(skin: Skin)
}
