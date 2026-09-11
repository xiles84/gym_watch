package com.gymwatch.phone

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.wearable.WearableListenerService
import com.gymwatch.adapters.driven.wearsync.PlayRequests
import com.gymwatch.core.domain.model.Audiobook
import com.gymwatch.core.domain.model.PlayOutcome
import kotlinx.coroutines.launch

/**
 * Answers the watch's "play this book". Google Play services starts this
 * service for the request, so the app doesn't have to be running.
 *
 * The answer is what the phone could do: [PlayOutcome.STARTING] once Audible
 * has the command, or why not. It never waits for the audio itself, which takes
 * Audible 15–20 seconds (docs/LESSONS.md #32).
 */
class PlayRequestService : WearableListenerService() {

    private val container by lazy { (application as PhoneApplication).container }

    override fun onRequest(nodeId: String, path: String, request: ByteArray): Task<ByteArray>? {
        if (path != PlayRequests.PATH) return null

        val answer = TaskCompletionSource<ByteArray>()
        container.scope.launch {
            val outcome = runCatching { container.audiobooks.play(Audiobook(PlayRequests.titleOf(request))) }
                .onFailure { Log.w(TAG, "Could not start the requested book", it) }
                .getOrDefault(PlayOutcome.PLAYER_UNAVAILABLE)
            answer.setResult(PlayRequests.answer(outcome))
        }
        return answer.task
    }

    private companion object {
        const val TAG = "GymWatch"
    }
}
