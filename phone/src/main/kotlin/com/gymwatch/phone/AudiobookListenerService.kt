package com.gymwatch.phone

import android.service.notification.NotificationListenerService
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Exists for the access it grants, not for notifications: while the user has it
 * switched on, this app can see Audible's playback session.
 *
 * The system binds it whenever access is on, after a reboot too, so the
 * tracking needs no foreground service and no app launch. Sessions can only be
 * listed while connected, so watching starts and stops with the connection.
 */
class AudiobookListenerService : NotificationListenerService() {

    private val container by lazy { (application as PhoneApplication).container }

    private var watching: Job? = null

    override fun onListenerConnected() {
        watching?.cancel()
        watching = container.scope.launch {
            container.watcher.nowPlaying().collect(container.tracker::onPlayback)
        }
    }

    override fun onListenerDisconnected() {
        stopWatching()
    }

    override fun onDestroy() {
        stopWatching()
        super.onDestroy()
    }

    private fun stopWatching() {
        watching?.cancel()
        watching = null
        container.tracker.onPlayback(null)
    }
}
