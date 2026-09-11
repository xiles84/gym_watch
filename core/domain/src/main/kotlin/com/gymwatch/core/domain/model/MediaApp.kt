package com.gymwatch.core.domain.model

/** Other apps on the watch the media screen opens with one tap. */
enum class MediaApp(val title: String) {
    SPOTIFY("Spotify"),

    /** The watch's own controls for whatever is playing on the phone. */
    PHONE_CONTROLS("Phone"),
}
