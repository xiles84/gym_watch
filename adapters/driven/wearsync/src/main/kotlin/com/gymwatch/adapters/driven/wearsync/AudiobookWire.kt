package com.gymwatch.adapters.driven.wearsync

import com.gymwatch.core.domain.model.Audiobook
import com.gymwatch.core.domain.model.PlayOutcome
import com.gymwatch.core.domain.model.RecentAudiobooks
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * How audiobooks travel between phone and watch. Kept free of Data Layer types
 * so the encoding is tested on the JVM.
 *
 * A book is one index across parallel columns rather than a nested map: a
 * `DataMap` holds string lists and long arrays natively. An unknown length or
 * position is [UNKNOWN], because a long array has no null.
 */
internal object AudiobookWire {
    /** Both paths must match the phone app's manifest filter. */
    const val RECENT_PATH = "/gymwatch/audiobooks/recent"
    const val PLAY_PATH = "/gymwatch/audiobooks/play"

    const val TITLES = "titles"
    const val AUTHORS = "authors"
    const val LENGTHS = "lengths_ms"
    const val POSITIONS = "positions_ms"

    private const val UNKNOWN = -1L

    data class Columns(
        val titles: List<String>,
        val authors: List<String>,
        val lengths: List<Long>,
        val positions: List<Long>,
    )

    fun encode(recent: RecentAudiobooks): Columns = Columns(
        titles = recent.books.map { it.title },
        authors = recent.books.map { it.author },
        lengths = recent.books.map { it.length.toWire() },
        positions = recent.books.map { it.position.toWire() },
    )

    /** Tolerates columns of different lengths: a book needs its title, the rest is optional. */
    fun decode(columns: Columns): RecentAudiobooks = RecentAudiobooks.of(
        columns.titles.mapIndexedNotNull { i, title ->
            title.takeIf { it.isNotBlank() }?.let {
                Audiobook(
                    title = it,
                    author = columns.authors.getOrNull(i).orEmpty(),
                    length = columns.lengths.getOrNull(i).fromWire(),
                    position = columns.positions.getOrNull(i).fromWire(),
                )
            }
        },
    )

    fun encode(outcome: PlayOutcome): ByteArray = outcome.name.toByteArray(Charsets.UTF_8)

    /** An answer this build doesn't know came from a newer phone app; it still got there. */
    fun decodeOutcome(bytes: ByteArray?): PlayOutcome {
        val name = bytes?.toString(Charsets.UTF_8) ?: return PlayOutcome.PHONE_UNREACHABLE
        return PlayOutcome.entries.firstOrNull { it.name == name } ?: PlayOutcome.STARTING
    }

    private fun Duration?.toWire(): Long = this?.inWholeMilliseconds ?: UNKNOWN

    private fun Long?.fromWire(): Duration? = this?.takeIf { it >= 0 }?.milliseconds
}
