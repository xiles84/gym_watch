package com.gymwatch.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class RecentAudiobooksTest {

    private fun book(title: String, position: Int = 0) =
        Audiobook(title = title, length = 10.hours, position = position.minutes)

    private val RecentAudiobooks.titles get() = books.map { it.title }

    @Test
    fun `a listened book goes to the front`() {
        val recent = RecentAudiobooks.EMPTY.listened(book("A")).listened(book("B"))

        assertEquals(listOf("B", "A"), recent.titles)
    }

    @Test
    fun `listening again moves a book up instead of adding it twice`() {
        val recent = RecentAudiobooks.EMPTY
            .listened(book("A"))
            .listened(book("B"))
            .listened(book("A", position = 30))

        assertEquals(listOf("A", "B"), recent.titles)
        assertEquals(30.minutes, recent.books.first().position)
    }

    @Test
    fun `the same title in different case or spacing is the same book`() {
        val recent = RecentAudiobooks.EMPTY.listened(book("Mimic & Me")).listened(book(" mimic & me "))

        assertEquals(1, recent.books.size)
    }

    @Test
    fun `the oldest book drops off past ten`() {
        val recent = (1..11).fold(RecentAudiobooks.EMPTY) { list, n -> list.listened(book("Book $n")) }

        assertEquals(RecentAudiobooks.MAX, recent.books.size)
        assertEquals("Book 11", recent.books.first().title)
        assertTrue("Book 1" !in recent.titles)
    }

    @Test
    fun `progress updates a book in place`() {
        val recent = RecentAudiobooks.EMPTY
            .listened(book("A"))
            .listened(book("B"))
            .progressed(book("A", position = 45))

        assertEquals(listOf("B", "A"), recent.titles)
        assertEquals(45.minutes, recent.books[1].position)
    }

    @Test
    fun `a report missing the length or author keeps what was known`() {
        val known = Audiobook("A", author = "Author", length = 10.hours, position = 1.hours)
        val gappy = Audiobook("A", position = 2.hours)

        val listenedAgain = RecentAudiobooks.EMPTY.listened(known).listened(gappy).books.single()
        val progressed = RecentAudiobooks.EMPTY.listened(known).progressed(gappy).books.single()

        assertEquals(Audiobook("A", author = "Author", length = 10.hours, position = 2.hours), listenedAgain)
        assertEquals(listenedAgain, progressed)
    }

    @Test
    fun `progress on a book that is not on the list adds nothing`() {
        val recent = RecentAudiobooks.EMPTY.listened(book("A")).progressed(book("Z"))

        assertEquals(listOf("A"), recent.titles)
    }

    @Test
    fun `synced data loses duplicates and overflow`() {
        val incoming = listOf(book("A"), book("a")) + (1..12).map { book("Book $it") }

        val recent = RecentAudiobooks.of(incoming)

        assertEquals(RecentAudiobooks.MAX, recent.books.size)
        assertEquals(listOf("A", "Book 1"), recent.titles.take(2))
    }

    @Test
    fun `time left is the length minus the position, never negative`() {
        assertEquals(9.hours + 30.minutes, book("A", position = 30).remaining)
        assertEquals(10.hours, Audiobook("A", length = 10.hours).remaining)
        assertEquals(kotlin.time.Duration.ZERO, Audiobook("A", length = 1.hours, position = 2.hours).remaining)
        assertNull(Audiobook("A").remaining)
    }
}
