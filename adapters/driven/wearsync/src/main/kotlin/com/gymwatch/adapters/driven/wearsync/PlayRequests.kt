package com.gymwatch.adapters.driven.wearsync

import com.gymwatch.core.domain.model.PlayOutcome

/** The phone's end of a play request from the watch: what came in, and how to answer. */
object PlayRequests {
    const val PATH = AudiobookWire.PLAY_PATH

    fun titleOf(request: ByteArray): String = request.toString(Charsets.UTF_8)

    fun answer(outcome: PlayOutcome): ByteArray = AudiobookWire.encode(outcome)
}
