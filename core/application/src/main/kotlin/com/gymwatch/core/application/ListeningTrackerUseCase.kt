package com.gymwatch.core.application

import com.gymwatch.core.domain.model.Audiobook
import com.gymwatch.core.domain.model.CaptureOutcome
import com.gymwatch.core.domain.model.NowPlaying
import com.gymwatch.core.domain.model.RecentAudiobooks
import com.gymwatch.core.domain.port.ClockPort
import com.gymwatch.core.domain.port.NowPlayingPort
import com.gymwatch.core.domain.port.RecentAudiobooksRepositoryPort
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Runs on the phone. Watches what the audiobook player reports and keeps the
 * recent-books list that the watch shows.
 *
 * A book only counts once it has played for [COUNTS_AFTER] in total. Pauses
 * don't reset the count, so a book paused twice in its first minute still gets
 * in. Without the threshold, a store preview or a book tapped by mistake would
 * push a real one off a list of ten.
 *
 * Two exceptions skip the minute. On an empty list, the first book seen goes in
 * at once (see [seedIfEmpty]). And [capture] puts the loaded book on top when
 * the user asks.
 *
 * After that, a pause or a switch to another book records how far in it got,
 * so the watch's "time left" stays close. Progress never changes a book's place
 * in the list.
 *
 * Events are handled one at a time, in arrival order, by a single coroutine.
 * Player callbacks and the threshold timer then never race each other. The
 * timer is a plain timeout: while audio plays, the phone stays awake. And if
 * the timer is late, the book is only recorded a little late.
 */
class ListeningTrackerUseCase(
    private val repository: RecentAudiobooksRepositoryPort,
    private val nowPlaying: NowPlayingPort,
    private val clock: ClockPort,
    scope: CoroutineScope,
) {
    private val events = Channel<Event>(Channel.UNLIMITED)

    init {
        scope.launch {
            var listening: Listening? = null
            while (true) {
                val wait = listening?.untilCounted(clock.elapsed())
                val event = if (wait == null) {
                    events.receive()
                } else {
                    withTimeoutOrNull(wait) { events.receive() } ?: Event.ThresholdReached
                }
                listening = try {
                    handle(listening, event)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // A failed write, say with Play services away, loses this
                    // one event. It must not end the loop and take all later
                    // tracking with it.
                    (event as? Event.Capture)?.reply?.completeExceptionally(e)
                    listening
                }
            }
        }
    }

    /** The player changed: a new book, play, pause, or [nowPlaying] null when the player went away. */
    fun onPlayback(nowPlaying: NowPlaying?) {
        events.trySend(Event.Player(nowPlaying))
    }

    /**
     * The user says the loaded book belongs on the list, now: it goes to the top,
     * with no minute of listening needed, and the oldest drops off a full list.
     * Paused counts too.
     *
     * Goes through the same queue as the player's events, so a capture and an
     * automatic write can't read the same list and overwrite each other.
     */
    suspend fun capture(): CaptureOutcome {
        val reply = CompletableDeferred<CaptureOutcome>()
        events.send(Event.Capture(reply))
        return reply.await()
    }

    private suspend fun handle(listening: Listening?, event: Event): Listening? {
        val now = clock.elapsed()
        return when (event) {
            Event.ThresholdReached -> listening?.let { countIfDue(it, now) }

            is Event.Capture -> capture(listening, event.reply, now)

            is Event.Player -> {
                val playing = event.nowPlaying
                if (playing == null || listening == null || !listening.book.isSameBookAs(playing.book)) {
                    // Leaving a book: keep the last position it reached.
                    if (listening?.counted == true) update { it.progressed(listening.book) }
                    playing?.let { seedIfEmpty(Listening.start(it, now)) }
                } else {
                    val next = listening.advance(playing, now)
                    val counted = countIfDue(next, now)
                    if (listening.counted && !playing.isPlaying) update { it.progressed(playing.book) }
                    counted
                }
            }
        }
    }

    private suspend fun capture(
        listening: Listening?,
        reply: CompletableDeferred<CaptureOutcome>,
        now: Duration,
    ): Listening? {
        val playing = nowPlaying.current()
        if (playing == null) {
            reply.complete(CaptureOutcome.NothingLoaded)
            return listening
        }

        val sameBook = listening != null && listening.book.isSameBookAs(playing.book)
        // Leaving a counted book, as a player switch would: keep its position.
        // Written before the capture reads the list, or the capture would undo it.
        if (!sameBook && listening?.counted == true) update { it.progressed(listening.book) }

        val before = repository.recent.first()
        val listed = before.books.any { it.isSameBookAs(playing.book) }
        val after = before.listened(playing.book)
        if (after != before) repository.save(after)
        reply.complete(
            if (listed) CaptureOutcome.MovedToTop(playing.book) else CaptureOutcome.Added(playing.book),
        )

        // It counts now, so its first minute doesn't move it up a second time.
        return if (listening != null && sameBook) listening.copy(book = playing.book, counted = true)
        else Listening.start(playing, now).copy(counted = true)
    }

    /**
     * A fresh install has an empty list and no history to fill it from. The
     * book already loaded in Audible is the one being listened to, so it goes
     * in straight away and the watch has something to show from the start.
     */
    private suspend fun seedIfEmpty(listening: Listening): Listening {
        if (repository.recent.first().books.isNotEmpty()) return listening
        update { it.listened(listening.book) }
        return listening.copy(counted = true)
    }

    private suspend fun countIfDue(listening: Listening, now: Duration): Listening {
        if (listening.counted || listening.played(now) < COUNTS_AFTER) return listening
        update { it.listened(listening.book) }
        return listening.copy(counted = true)
    }

    /** Reads the stored list, not a cached one, before writing (docs/LESSONS.md #10). */
    private suspend fun update(transform: (RecentAudiobooks) -> RecentAudiobooks) {
        val current = repository.recent.first()
        val next = transform(current)
        if (next != current) repository.save(next)
    }

    private sealed interface Event {
        data class Player(val nowPlaying: NowPlaying?) : Event
        data object ThresholdReached : Event
        class Capture(val reply: CompletableDeferred<CaptureOutcome>) : Event
    }

    /**
     * One book in the player, and how long it has really played: [playedBefore]
     * from earlier stretches, plus the current one if [playingSince] is set.
     */
    private data class Listening(
        val book: Audiobook,
        val playedBefore: Duration,
        val playingSince: Duration?,
        val counted: Boolean,
    ) {
        fun played(now: Duration): Duration =
            playedBefore + (playingSince?.let { now - it } ?: Duration.ZERO)

        /** How long until this book counts, or null if nothing will change without a new event. */
        fun untilCounted(now: Duration): Duration? =
            if (counted || playingSince == null) null
            else (COUNTS_AFTER - played(now)).coerceAtLeast(Duration.ZERO)

        fun advance(playing: NowPlaying, now: Duration): Listening = copy(
            book = playing.book,
            playedBefore = played(now),
            playingSince = if (playing.isPlaying) now else null,
        )

        companion object {
            fun start(playing: NowPlaying, now: Duration) = Listening(
                book = playing.book,
                playedBefore = Duration.ZERO,
                playingSince = if (playing.isPlaying) now else null,
                counted = false,
            )
        }
    }

    companion object {
        val COUNTS_AFTER: Duration = 60.seconds
    }
}
