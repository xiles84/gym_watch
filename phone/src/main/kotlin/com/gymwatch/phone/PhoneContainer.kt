package com.gymwatch.phone

import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import com.gymwatch.adapters.driven.audible.AudibleSession
import com.gymwatch.adapters.driven.audible.AudibleSessionPlayer
import com.gymwatch.adapters.driven.audible.AudibleSessionWatcher
import com.gymwatch.adapters.driven.wearsync.DataLayerRecentAudiobooks
import com.gymwatch.core.application.AudiobooksUseCase
import com.gymwatch.core.application.ListeningTrackerUseCase
import com.gymwatch.core.domain.port.ClockPort
import com.gymwatch.core.domain.port.HapticsPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.time.Duration.Companion.milliseconds

/**
 * The phone app's composition root, the counterpart of the watch's
 * `AppContainer`. It reuses the same core and the same sync adapter, with
 * Audible's session in place of the watch's platform adapters.
 */
class PhoneContainer(context: Context) {

    /** Application-scoped: the tracker must outlive the settings screen. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Monotonic, like the watch's clock. Only used to time the one-minute threshold. */
    private val clock = ClockPort { SystemClock.elapsedRealtime().milliseconds }

    private val listener = ComponentName(context, AudiobookListenerService::class.java)

    private val recentStore = DataLayerRecentAudiobooks(context)

    val session = AudibleSession(context, listener)

    val watcher = AudibleSessionWatcher(context, listener)

    val tracker = ListeningTrackerUseCase(recentStore, session, clock, scope)

    /** The phone has nothing to buzz; the watch gives the feedback. */
    val audiobooks = AudiobooksUseCase(
        repository = recentStore,
        player = AudibleSessionPlayer(context, session),
        haptics = HapticsPort { },
        scope = scope,
    )
}
