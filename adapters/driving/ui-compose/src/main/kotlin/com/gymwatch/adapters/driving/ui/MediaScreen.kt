package com.gymwatch.adapters.driving.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.Text
import com.gymwatch.core.application.AudiobooksUseCase
import com.gymwatch.core.application.MediaShortcutsUseCase
import com.gymwatch.core.domain.model.Audiobook
import com.gymwatch.core.domain.model.MediaApp
import com.gymwatch.core.domain.model.PlayOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Where the media shortcut icons come from. Implemented by the composition root,
 * so this driving adapter never depends on the driven one that reads other apps.
 */
fun interface MediaIcons {
    /** May decode a drawable; only ever called off the main thread. */
    fun load(app: MediaApp): Bitmap?
}

/**
 * Spotify and the phone's media controls one tap away, then the books last
 * listened to in Audible on the phone. Tapping a book starts it on the phone.
 *
 * The list is the phone's: the phone app records each book after a minute of
 * listening and syncs the last ten here. A book takes Audible 15–20 seconds to
 * switch to, so the tapped row says so for that long instead of looking dead
 * (docs/LESSONS.md #32).
 *
 * [ScalingLazyColumn] because ten rows don't fit a round screen, and a plain
 * Column loses its first and last rows to the curve (#25).
 */
@Composable
fun MediaScreen(
    audiobooks: AudiobooksUseCase,
    shortcuts: MediaShortcutsUseCase,
    icons: MediaIcons,
    modifier: Modifier = Modifier,
) {
    val recent by audiobooks.recent.collectAsStateWithLifecycle()
    val installed by produceState(initialValue = emptySet<MediaApp>(), shortcuts) {
        value = shortcuts.installed()
    }
    val scope = rememberCoroutineScope()

    // What happened to each tapped book, by title. A row shows it in place of
    // its time left until it expires.
    val requests = remember { mutableStateMapOf<String, Request>() }

    ScalingLazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        // Left on, auto-centering opens the page with the shortcuts scrolled off.
        autoCentering = null,
    ) {
        if (installed.isNotEmpty()) {
            item { Label("MEDIA") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MediaApp.entries.filter { it in installed }.forEach { app ->
                        AppShortcut(app, icons) { scope.launch { shortcuts.open(app) } }
                    }
                }
            }
        }

        item { Label("AUDIOBOOKS") }

        if (recent.books.isEmpty()) {
            item {
                Text(
                    text = "Play a book in Audible on your phone, with the Gym Watch phone app set up",
                    color = GymColors.Dim,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }

        recent.books.forEach { book ->
            item(key = book.title) {
                val request = requests[book.title]
                BookRow(
                    book = book,
                    request = request,
                    onClick = {
                        if (request == Request.Asking) return@BookRow
                        requests[book.title] = Request.Asking
                        scope.launch { requests[book.title] = Request.Answered(audiobooks.play(book)) }
                    },
                )
                if (request is Request.Answered) {
                    LaunchedEffect(request) {
                        delay(request.outcome.shownFor())
                        requests.remove(book.title)
                    }
                }
            }
        }
    }
}

private sealed interface Request {
    data object Asking : Request
    data class Answered(val outcome: PlayOutcome) : Request
}

/** Long enough to cover Audible's switch when it worked; long enough to read when it didn't. */
private fun PlayOutcome.shownFor(): Duration = if (this == PlayOutcome.STARTING) 20.seconds else 6.seconds

@Composable
private fun Label(text: String) {
    Text(text, color = GymColors.Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
}

/** The app's own icon, untinted: it should look like what it opens. */
@Composable
private fun AppShortcut(app: MediaApp, icons: MediaIcons, onClick: () -> Unit) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, icons, app) {
        value = withContext(Dispatchers.IO) { icons.load(app)?.asImageBitmap() }
    }

    Column(modifier = Modifier.width(56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(GymColors.Surface, CircleShape)
                .border(1.dp, GymColors.Outline, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            val loaded = bitmap
            if (loaded != null) {
                Image(bitmap = loaded, contentDescription = app.title, modifier = Modifier.size(34.dp))
            } else {
                Text(app.title.take(1), color = GymColors.OnSurface, fontSize = 18.sp)
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(app.title, color = GymColors.OnSurface, fontSize = 10.sp)
    }
}

/** A 48dp-tall row at least: the whole row is the target, as the rest presets are. */
@Composable
private fun BookRow(book: Audiobook, request: Request?, onClick: () -> Unit) {
    val (status, highlighted) = when (request) {
        Request.Asking -> "asking the phone…" to true
        is Request.Answered -> request.outcome.message() to (request.outcome == PlayOutcome.STARTING)
        null -> book.remaining?.let { "${it.asHoursAndMinutes()} left" }.orEmpty() to false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GymColors.Surface, RoundedCornerShape(percent = 50))
            .border(1.dp, if (highlighted) GymColors.Rest else GymColors.Outline, RoundedCornerShape(percent = 50))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
    ) {
        Text(
            text = book.title,
            color = GymColors.OnSurface,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (status.isNotEmpty()) {
            Text(
                text = status,
                color = if (highlighted) GymColors.Rest else GymColors.Muted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun PlayOutcome.message(): String = when (this) {
    PlayOutcome.STARTING -> "starting on phone…"
    PlayOutcome.PHONE_UNREACHABLE -> "phone not reachable"
    PlayOutcome.PHONE_NEEDS_ACCESS -> "allow access in the phone app"
    PlayOutcome.PLAYER_UNAVAILABLE -> "open Audible on the phone"
}
