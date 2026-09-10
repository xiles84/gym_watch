package com.gymwatch.core.domain.model

/**
 * The swipeable screens, in the order they can be reordered into.
 *
 * Settings is deliberately absent: it is appended by the UI as the last page
 * and can never be hidden, because hiding the way back into settings is a trap
 * with no escape short of clearing app data.
 */
enum class AppScreen(val title: String) {
    CHRONOMETER("Chrono"),
    REST_TIMER("Rest"),
    COUNTER("Counter"),
    PROFILES("Profiles"),
}

/**
 * Which screens are shown, and in what order.
 *
 * [order] always holds every [AppScreen] — hiding is tracked separately in
 * [hidden] rather than by removing entries, so a screen turned off and on again
 * comes back where the user left it instead of jumping to the end.
 */
data class ScreenLayout(
    val order: List<AppScreen>,
    val hidden: Set<AppScreen> = emptySet(),
) {
    /**
     * Never empty. `toggle` already refuses to hide the last screen, but stored
     * data could still say otherwise, and a pager with zero pages crashes.
     */
    val visible: List<AppScreen>
        get() = order.filterNot { it in hidden }.ifEmpty { listOf(order.first()) }

    fun isVisible(screen: AppScreen): Boolean = screen !in hidden

    /** Hiding the last visible screen is refused, not clamped later. */
    fun toggle(screen: AppScreen): ScreenLayout = when {
        screen in hidden -> copy(hidden = hidden - screen)
        visible.size <= 1 -> this
        else -> copy(hidden = hidden + screen)
    }

    /** Moves one screen by [delta] places; a move off either end is a no-op. */
    fun move(screen: AppScreen, delta: Int): ScreenLayout {
        val from = order.indexOf(screen)
        if (from < 0) return this
        val to = from + delta
        if (to !in order.indices) return this
        return copy(
            order = order.toMutableList().also {
                it.removeAt(from)
                it.add(to, screen)
            },
        )
    }

    companion object {
        val DEFAULT = ScreenLayout(AppScreen.entries.toList())

        /**
         * The only way persisted data should enter. Any screen missing from a
         * stored order — because it was added in a later version — is appended
         * rather than silently dropped.
         */
        fun of(order: List<AppScreen>, hidden: Set<AppScreen>): ScreenLayout {
            val complete = order.distinct() + AppScreen.entries.filterNot { it in order }
            return ScreenLayout(complete, hidden.intersect(AppScreen.entries.toSet()))
        }
    }
}
