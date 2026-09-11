package com.gymwatch.core.domain.model

/**
 * The books listened to most recently, newest first, at most [MAX].
 *
 * Built up one play at a time on the phone and mirrored to the watch. Nothing
 * else can fill it: Audible will not share its library (docs/LESSONS.md #32).
 */
data class RecentAudiobooks(val books: List<Audiobook> = emptyList()) {

    /**
     * [book] was really listened to: it moves to the front, replacing the older
     * entry for the same title, and the oldest book past [MAX] drops off.
     */
    fun listened(book: Audiobook): RecentAudiobooks {
        val known = books.firstOrNull { it.isSameBookAs(book) }
        return RecentAudiobooks((listOf(book.filledFrom(known)) + books.filterNot { it.isSameBookAs(book) }).take(MAX))
    }

    /**
     * A book already on the list moved on — paused further in, say. It keeps
     * its place, because progress is not a new listen. A book that is not on
     * the list is ignored rather than added: only [listened] decides that.
     */
    fun progressed(book: Audiobook): RecentAudiobooks =
        RecentAudiobooks(books.map { if (it.isSameBookAs(book)) book.filledFrom(it) else it })

    /**
     * Audible's session doesn't always carry everything. While a book is still
     * loading, its length can be missing. A gap in the new report keeps what was
     * already known, rather than wiping the watch's "time left" until the book
     * is played again.
     */
    private fun Audiobook.filledFrom(known: Audiobook?): Audiobook {
        if (known == null) return this
        return copy(
            author = author.ifBlank { known.author },
            length = length ?: known.length,
            position = position ?: known.position,
        )
    }

    companion object {
        const val MAX = 10

        val EMPTY = RecentAudiobooks()

        /** The only way stored or synced data should enter: duplicates and overflow are dropped. */
        fun of(books: List<Audiobook>): RecentAudiobooks =
            RecentAudiobooks(
                books.fold(emptyList<Audiobook>()) { kept, book ->
                    if (kept.any { it.isSameBookAs(book) }) kept else kept + book
                }.take(MAX),
            )
    }
}
