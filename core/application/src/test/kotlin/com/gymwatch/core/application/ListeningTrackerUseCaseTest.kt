package com.gymwatch.core.application

import com.gymwatch.core.application.fake.FakeNowPlaying
import com.gymwatch.core.application.fake.InMemoryRecentAudiobooksRepository
import com.gymwatch.core.application.fake.SchedulerClock
import com.gymwatch.core.domain.model.Audiobook
import com.gymwatch.core.domain.model.CaptureOutcome
import com.gymwatch.core.domain.model.NowPlaying
import com.gymwatch.core.domain.model.RecentAudiobooks
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ListeningTrackerUseCaseTest {

    private val mimic = Audiobook("Mimic & Me", "Cassius Lange", length = 17.hours, position = 11.hours)
    private val faire = Audiobook("The Case of the Felonious Faire", "Drew Hayes", length = 8.hours, position = 4.hours)

    private fun TestScope.tracker(
        repository: InMemoryRecentAudiobooksRepository = seeded(),
        player: FakeNowPlaying = FakeNowPlaying(),
    ) = ListeningTrackerUseCase(repository, player, SchedulerClock(testScheduler), backgroundScope)

    /** A list that already has a book, so the first-book seeding stays out of the way. */
    private fun seeded() =
        InMemoryRecentAudiobooksRepository(RecentAudiobooks.EMPTY.listened(Audiobook("For the Emperor")))

    private val InMemoryRecentAudiobooksRepository.titles
        get() = current.books.map { it.title }

    @Test
    fun `on an empty list the loaded book goes in at once, even paused`() = runTest {
        val repository = InMemoryRecentAudiobooksRepository()
        val tracker = tracker(repository)

        tracker.onPlayback(NowPlaying(mimic, isPlaying = false))
        runCurrent()

        assertEquals(listOf("Mimic & Me"), repository.titles)
    }

    @Test
    fun `a book counts after a minute of playing`() = runTest {
        val repository = seeded()
        val tracker = tracker(repository)

        tracker.onPlayback(NowPlaying(mimic, isPlaying = true))
        advanceTimeBy(59.seconds)
        runCurrent()
        assertEquals(listOf("For the Emperor"), repository.titles)

        advanceTimeBy(1.seconds)
        runCurrent()
        assertEquals(listOf("Mimic & Me", "For the Emperor"), repository.titles)
    }

    @Test
    fun `a book played for a few seconds never counts`() = runTest {
        val repository = seeded()
        val tracker = tracker(repository)

        tracker.onPlayback(NowPlaying(faire, isPlaying = true))
        advanceTimeBy(10.seconds)
        tracker.onPlayback(NowPlaying(faire, isPlaying = false))
        advanceTimeBy(10.minutes)
        runCurrent()

        assertEquals(listOf("For the Emperor"), repository.titles)
    }

    @Test
    fun `pauses do not reset the minute`() = runTest {
        val repository = seeded()
        val tracker = tracker(repository)

        tracker.onPlayback(NowPlaying(mimic, isPlaying = true))
        advanceTimeBy(40.seconds)
        tracker.onPlayback(NowPlaying(mimic, isPlaying = false))
        advanceTimeBy(5.minutes)
        tracker.onPlayback(NowPlaying(mimic, isPlaying = true))
        advanceTimeBy(19.seconds)
        runCurrent()
        assertEquals(listOf("For the Emperor"), repository.titles)

        advanceTimeBy(1.seconds)
        runCurrent()
        assertEquals(listOf("Mimic & Me", "For the Emperor"), repository.titles)
    }

    @Test
    fun `switching books starts the minute over`() = runTest {
        val repository = seeded()
        val tracker = tracker(repository)

        tracker.onPlayback(NowPlaying(mimic, isPlaying = true))
        advanceTimeBy(50.seconds)
        tracker.onPlayback(NowPlaying(faire, isPlaying = true))
        advanceTimeBy(50.seconds)
        runCurrent()
        assertEquals(listOf("For the Emperor"), repository.titles)

        advanceTimeBy(10.seconds)
        runCurrent()
        assertEquals(listOf("The Case of the Felonious Faire", "For the Emperor"), repository.titles)
    }

    @Test
    fun `a newly counted book goes above the ones already listed`() = runTest {
        val repository = InMemoryRecentAudiobooksRepository(RecentAudiobooks.EMPTY.listened(mimic))
        val tracker = tracker(repository)

        tracker.onPlayback(NowPlaying(faire, isPlaying = true))
        advanceTimeBy(1.minutes)
        runCurrent()

        assertEquals(listOf("The Case of the Felonious Faire", "Mimic & Me"), repository.titles)
    }

    @Test
    fun `pausing a counted book records its position without moving it`() = runTest {
        val repository = seeded()
        val tracker = tracker(repository)

        tracker.onPlayback(NowPlaying(mimic, isPlaying = true))
        advanceTimeBy(1.minutes)
        runCurrent()
        // Another book counted since, so Mimic is second.
        repository.save(repository.current.listened(faire))

        tracker.onPlayback(NowPlaying(mimic.copy(position = 12.hours), isPlaying = false))
        runCurrent()

        assertEquals(listOf("The Case of the Felonious Faire", "Mimic & Me", "For the Emperor"), repository.titles)
        assertEquals(12.hours, repository.current.books[1].position)
    }

    @Test
    fun `leaving a counted book keeps the last position it reached`() = runTest {
        val repository = seeded()
        val tracker = tracker(repository)

        tracker.onPlayback(NowPlaying(mimic, isPlaying = true))
        advanceTimeBy(1.minutes)
        runCurrent()
        tracker.onPlayback(NowPlaying(mimic.copy(position = 11.hours + 30.minutes), isPlaying = true))
        tracker.onPlayback(null)
        runCurrent()

        assertEquals(11.hours + 30.minutes, repository.current.books.first().position)
    }

    @Test
    fun `capturing a book that is not listed puts it on top`() = runTest {
        val repository = seeded()
        val tracker = tracker(repository, FakeNowPlaying(NowPlaying(faire, isPlaying = false)))

        val outcome = tracker.capture()

        assertEquals(CaptureOutcome.Added(faire), outcome)
        assertEquals(listOf("The Case of the Felonious Faire", "For the Emperor"), repository.titles)
    }

    @Test
    fun `capturing a listed book moves it to the top`() = runTest {
        val repository = InMemoryRecentAudiobooksRepository(
            RecentAudiobooks.EMPTY.listened(mimic).listened(Audiobook("For the Emperor")),
        )
        val loaded = mimic.copy(position = 12.hours)
        val tracker = tracker(repository, FakeNowPlaying(NowPlaying(loaded, isPlaying = true)))

        val outcome = tracker.capture()

        assertEquals(CaptureOutcome.MovedToTop(loaded), outcome)
        assertEquals(listOf("Mimic & Me", "For the Emperor"), repository.titles)
        assertEquals(12.hours, repository.current.books.first().position)
    }

    @Test
    fun `capturing into a full list drops the oldest book`() = runTest {
        val full = (1..RecentAudiobooks.MAX).fold(RecentAudiobooks.EMPTY) { list, n -> list.listened(Audiobook("Book $n")) }
        val repository = InMemoryRecentAudiobooksRepository(full)
        val tracker = tracker(repository, FakeNowPlaying(NowPlaying(faire, isPlaying = false)))

        tracker.capture()

        assertEquals(RecentAudiobooks.MAX, repository.current.books.size)
        assertEquals("The Case of the Felonious Faire", repository.titles.first())
        assertEquals("Book 2", repository.titles.last())
    }

    @Test
    fun `capturing with nothing loaded changes nothing`() = runTest {
        val repository = seeded()
        val tracker = tracker(repository, FakeNowPlaying(loaded = null))

        assertEquals(CaptureOutcome.NothingLoaded, tracker.capture())
        assertNull(repository.saved)
    }

    @Test
    fun `a captured book's first minute does not move it up again`() = runTest {
        val repository = seeded()
        val playing = NowPlaying(faire, isPlaying = true)
        val tracker = tracker(repository, FakeNowPlaying(playing))

        tracker.onPlayback(playing)
        tracker.capture()
        // Another book goes on top meanwhile.
        repository.save(repository.current.listened(mimic))
        advanceTimeBy(2.minutes)
        runCurrent()

        assertEquals(listOf("Mimic & Me", "The Case of the Felonious Faire", "For the Emperor"), repository.titles)
    }

    @Test
    fun `nothing is written while a book is still short of a minute`() = runTest {
        val repository = seeded()
        val tracker = tracker(repository)

        tracker.onPlayback(NowPlaying(mimic, isPlaying = true))
        tracker.onPlayback(NowPlaying(mimic.copy(position = 11.hours + 1.minutes), isPlaying = false))
        tracker.onPlayback(null)
        runCurrent()

        assertNull(repository.saved)
    }
}
