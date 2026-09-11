package com.gymwatch.core.application

import com.gymwatch.core.domain.model.Haptic
import com.gymwatch.core.domain.model.MediaApp
import com.gymwatch.core.domain.port.HapticsPort
import com.gymwatch.core.domain.port.MediaAppsPort

/** One tap into Spotify, or into the watch's controls for what the phone is playing. */
class MediaShortcutsUseCase(
    private val apps: MediaAppsPort,
    private val haptics: HapticsPort,
) {
    suspend fun installed(): Set<MediaApp> =
        MediaApp.entries.filter { apps.isInstalled(it) }.toSet()

    suspend fun open(app: MediaApp): Boolean {
        val opened = apps.open(app)
        if (opened) haptics.play(Haptic.CONFIRM)
        return opened
    }
}
