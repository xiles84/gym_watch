package com.gymwatch.phone

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymwatch.core.domain.model.Audiobook
import com.gymwatch.core.domain.model.CaptureOutcome
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * The phone app's only screen: switch on notification access, and see the list
 * the watch shows.
 *
 * Everything else happens without it. The listener service tracks Audible and
 * the request service starts books.
 */
class PhoneActivity : ComponentActivity() {

    private val container by lazy { (application as PhoneApplication).container }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            MaterialTheme(
                colorScheme = if (isSystemInDarkTheme()) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context),
            ) {
                CompanionScreen(onAllowAccess = ::openAccessSettings)
            }
        }
    }

    @Composable
    private fun CompanionScreen(onAllowAccess: () -> Unit) {
        val recent by container.audiobooks.recent.collectAsStateWithLifecycle()
        var hasAccess by remember { mutableStateOf(container.session.hasAccess()) }
        val snackbar = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        var capturing by remember { mutableStateOf(false) }

        val capture: () -> Unit = {
            if (!capturing) {
                capturing = true
                scope.launch {
                    val message = if (!container.session.hasAccess()) {
                        "Allow notification access first, so the app can see Audible."
                    } else {
                        runCatching { container.tracker.capture() }
                            .map { it.message() }
                            .getOrElse { "Couldn't save the list. Try again." }
                    }
                    capturing = false
                    snackbar.showSnackbar(message)
                }
            }
        }

        // Access is granted in Settings, so check again on every return.
        LifecycleResumeEffect(Unit) {
            hasAccess = container.session.hasAccess()
            onPauseOrDispose { }
        }

        Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text("Gym Watch", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Starts your Audible books on this phone from the watch.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                if (!hasAccess) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Notification access needed", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "It's how this app sees what Audible is playing, and how it starts a " +
                                        "book. It doesn't read your notifications.",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Button(onClick = onAllowAccess) { Text("Allow access") }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Recent audiobooks",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        // Doesn't wait for a minute of listening: whatever Audible
                        // has loaded, playing or paused, goes on top now.
                        FilledTonalButton(onClick = capture) {
                            Text(if (capturing) "Capturing…" else "Capture")
                        }
                    }
                    Text(
                        "Capture puts the book loaded in Audible at the top of the list.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                if (recent.books.isEmpty()) {
                    item {
                        Text(
                            "Play a book in Audible. The one loaded now appears first, and each book after " +
                                "it once it has played for a minute.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                items(recent.books, key = { it.title }) { book ->
                    ListItem(
                        headlineContent = { Text(book.title) },
                        supportingContent = { Text(book.subtitle()) },
                    )
                }
            }
        }
    }

    /** Straight to this app's switch where the phone supports it, the whole list otherwise. */
    private fun openAccessSettings() {
        val component = ComponentName(this, AudiobookListenerService::class.java).flattenToString()
        val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component)
        try {
            startActivity(detail)
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }
}

private fun CaptureOutcome.message(): String = when (this) {
    is CaptureOutcome.Added -> "Added ${book.title} to the top"
    is CaptureOutcome.MovedToTop -> "Moved ${book.title} to the top"
    CaptureOutcome.NothingLoaded -> "Nothing is loaded in Audible. Open a book there first."
}

private fun Audiobook.subtitle(): String =
    listOfNotNull(author.takeIf { it.isNotBlank() }, remaining?.let { "${it.hoursAndMinutes()} left" })
        .joinToString(" · ")

private fun Duration.hoursAndMinutes(): String {
    val hours = inWholeHours
    val minutes = inWholeMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
