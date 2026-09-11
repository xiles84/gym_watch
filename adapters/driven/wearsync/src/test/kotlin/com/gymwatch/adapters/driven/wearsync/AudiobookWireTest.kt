package com.gymwatch.adapters.driven.wearsync

import com.gymwatch.core.domain.model.Audiobook
import com.gymwatch.core.domain.model.PlayOutcome
import com.gymwatch.core.domain.model.RecentAudiobooks
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class AudiobookWireTest {

    @Test
    fun `a list survives the round trip, unknown lengths included`() {
        val recent = RecentAudiobooks.EMPTY
            .listened(Audiobook("For the Emperor", "Sandy Mitchell"))
            .listened(Audiobook("Mimic & Me", "Cassius Lange", length = 17.hours, position = 11.hours + 38.minutes))

        assertEquals(recent, AudiobookWire.decode(AudiobookWire.encode(recent)))
    }

    @Test
    fun `short or blank columns lose only what is missing`() {
        val columns = AudiobookWire.Columns(
            titles = listOf("A", " ", "C"),
            authors = listOf("Author A"),
            lengths = listOf(3_600_000L),
            positions = emptyList(),
        )

        val books = AudiobookWire.decode(columns).books

        assertEquals(listOf("A", "C"), books.map { it.title })
        assertEquals(Audiobook("A", "Author A", length = 1.hours), books[0])
        assertEquals(Audiobook("C"), books[1])
    }

    @Test
    fun `outcomes round trip, and no answer means the phone was not reached`() {
        PlayOutcome.entries.forEach { assertEquals(it, AudiobookWire.decodeOutcome(AudiobookWire.encode(it))) }
        assertEquals(PlayOutcome.PHONE_UNREACHABLE, AudiobookWire.decodeOutcome(null))
    }
}
