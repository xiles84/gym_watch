package com.gymwatch.core.domain.port

import com.gymwatch.core.domain.model.Counter
import com.gymwatch.core.domain.model.RecentAudiobooks
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

/**
 * The recently listened audiobooks, shared by the phone and the watch.
 *
 * The phone writes it as books are played; the watch only reads it. [recent]
 * must emit the stored list first, even when that list is empty, because a
 * write is based on it (docs/LESSONS.md #10).
 */
interface RecentAudiobooksRepositoryPort {
    val recent: Flow<RecentAudiobooks>
    suspend fun save(recent: RecentAudiobooks)
}
