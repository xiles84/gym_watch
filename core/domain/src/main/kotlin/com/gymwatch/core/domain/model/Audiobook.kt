package com.gymwatch.core.domain.model

import kotlin.time.Duration

/**
 * A book as the phone's audiobook player describes it while playing.
 *
 * There is no library behind this. Audible refuses to let another app browse
 * its catalogue, so a book is only known once it has been played on the phone
 * (docs/LESSONS.md #32). The title is also how a book is started again: the
 * only route in is a search by title on Audible's playback session.
 *
 * @param length the whole book, when the player reports it.
 * @param position how far in, as of the last time it was recorded.
 */
data class Audiobook(
    val title: String,
    val author: String = "",
    val length: Duration? = null,
    val position: Duration? = null,
) {
    /** What is left to listen to, or null if the player never said how long the book is. */
    val remaining: Duration?
        get() {
            val total = length ?: return null
            val done = position ?: Duration.ZERO
            return (total - done).coerceAtLeast(Duration.ZERO)
        }

    /**
     * Two plays of the same book can differ in position, and Audible's title
     * can come back with different spacing or case. A book is its title.
     */
    fun isSameBookAs(other: Audiobook): Boolean = key == other.key

    private val key: String get() = title.trim().lowercase()
}

/** The book loaded in the phone's player right now, and whether it is playing. */
data class NowPlaying(val book: Audiobook, val isPlaying: Boolean)

/** What a capture of the loaded book did to the recent list. */
sealed interface CaptureOutcome {
    /** The book wasn't listed. It's on top now, and the oldest went if the list was full. */
    data class Added(val book: Audiobook) : CaptureOutcome

    /** The book was already listed, and is on top now (it may have been already). */
    data class MovedToTop(val book: Audiobook) : CaptureOutcome

    /** Nothing to capture: no book loaded, or the player can't be seen. */
    data object NothingLoaded : CaptureOutcome
}

/**
 * What became of a request to start a book on the phone.
 *
 * [STARTING] means the phone's player accepted the command, not that audio is
 * already playing. Audible takes 15–20 seconds to switch books (#32).
 */
enum class PlayOutcome {
    STARTING,

    /** No phone in reach, or the phone app is not installed. */
    PHONE_UNREACHABLE,

    /** The phone app has not been given notification access, so it cannot see the player. */
    PHONE_NEEDS_ACCESS,

    /** Audible is not running on the phone and could not be started. */
    PLAYER_UNAVAILABLE,
}
