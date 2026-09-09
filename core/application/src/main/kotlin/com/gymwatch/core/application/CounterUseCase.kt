package com.gymwatch.core.application

import com.gymwatch.core.domain.model.Counter
import com.gymwatch.core.domain.model.Haptic
import com.gymwatch.core.domain.port.CounterRepositoryPort
import com.gymwatch.core.domain.port.HapticsPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CounterUseCase(
    private val repository: CounterRepositoryPort,
    private val haptics: HapticsPort,
    private val scope: CoroutineScope,
) {
    /**
     * For display only. It starts at a fabricated zero and is replaced by the
     * stored value on the first emission — so it must never be the basis of a
     * write. See [mutate].
     */
    val state: StateFlow<Counter> =
        repository.counter.stateIn(scope, SharingStarted.Eagerly, Counter())

    fun increment() = mutate(Haptic.TICK) { it.increment() }

    fun decrement() = mutate(Haptic.TICK) { it.decrement() }

    /** Rotary bezel input arrives as a signed step. */
    fun stepBy(delta: Int) = mutate(Haptic.TICK) { it.stepBy(delta) }

    fun reset() = mutate(Haptic.CONFIRM) { it.reset() }

    fun setLabel(label: String) = mutate(null) { it.withLabel(label) }

    /**
     * Reads the *repository*, not [state], before transforming.
     *
     * Reading the cached StateFlow would use its seeded default whenever a tap
     * lands before the first stored value has been collected — pressing "+" a
     * beat after launch would write 1 over a saved 12. Always transform the
     * value the store actually holds.
     */
    private fun mutate(haptic: Haptic?, transform: (Counter) -> Counter) {
        scope.launch {
            val current = repository.counter.first()
            val next = transform(current)
            if (next == current) return@launch   // at a bound: no write, no buzz
            haptic?.let(haptics::play)
            repository.save(next)
        }
    }
}
