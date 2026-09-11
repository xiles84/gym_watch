package com.gymwatch.adapters.driven.platform

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import com.gymwatch.core.domain.model.MediaApp
import com.gymwatch.core.domain.port.MediaAppsPort

/**
 * Opens Spotify or the watch's own media controls, and reads their launcher
 * icons so a shortcut looks like the app it opens.
 *
 * Opened through the app's launcher entry, like tapping it in the app list.
 * Unlike Samsung Health (docs/LESSONS.md #28), no deeper route is needed:
 * Spotify's watch app plays on the phone from its own screen, and the media
 * controller follows whatever the phone is playing.
 */
class MediaAppLauncher(private val context: Context) : MediaAppsPort {

    override suspend fun isInstalled(app: MediaApp): Boolean = launchIntent(app) != null

    override suspend fun open(app: MediaApp): Boolean {
        // NEW_TASK only because this is a real startActivity from a non-Activity
        // context — not a PendingIntent, which is where it breaks Recents (#17).
        val intent = launchIntent(app)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) ?: return false
        return runCatching { context.startActivity(intent) }
            .onFailure { Log.w(TAG, "Could not open ${app.name}", it) }
            .isSuccess
    }

    /** Null when the app is not installed. Loads a drawable, so call it off the main thread. */
    fun icon(app: MediaApp): Bitmap? = runCatching {
        context.packageManager.getApplicationIcon(packageOf(app)).toBitmap(ICON_PX, ICON_PX)
    }.getOrNull()

    private fun launchIntent(app: MediaApp): Intent? =
        runCatching { context.packageManager.getLaunchIntentForPackage(packageOf(app)) }.getOrNull()

    private companion object {
        const val TAG = "GymWatch"
        const val ICON_PX = 96

        fun packageOf(app: MediaApp): String = when (app) {
            MediaApp.SPOTIFY -> SPOTIFY_PACKAGE
            MediaApp.PHONE_CONTROLS -> MEDIA_CONTROLLER_PACKAGE
        }
    }
}

/** Spotify's Wear OS app uses the same package name as its phone app. */
internal const val SPOTIFY_PACKAGE = "com.spotify.music"

/** Samsung's "Media controller" on Galaxy Watch, which follows what the phone is playing. Read off the SM-L705F. */
internal const val MEDIA_CONTROLLER_PACKAGE = "com.samsung.android.mediacontroller"
