package com.gymwatch.adapters.driven.platform

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.util.LruCache
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.gymwatch.core.domain.model.ExerciseKind

/**
 * Samsung Health's own exercise icons, read out of Samsung Health at runtime.
 *
 * Nothing is copied into this app. The drawables are looked up in Samsung
 * Health's resources, so they always match the installed version and are never
 * ours to ship. They are white glyphs on transparent, which is what lets the UI
 * tint them to the current skin.
 *
 * If a name disappears in a Samsung Health update, or Samsung Health is not
 * installed, the result is null and the UI falls back to the exercise's initial.
 */
class SamsungHealthIcons(private val context: Context) {

    private val cache = LruCache<ExerciseKind, Bitmap>(CACHE_ENTRIES)

    /** Decodes a resource on a miss, so call it off the main thread. */
    fun bitmap(kind: ExerciseKind): Bitmap? {
        cache.get(kind)?.let { return it }
        val loaded = runCatching { load(kind) }
            .onFailure { Log.w(TAG, "No Samsung Health icon for ${kind.name}", it) }
            .getOrNull()
            ?: return null
        cache.put(kind, loaded)
        return loaded
    }

    // Looked up by name on purpose: the numeric id belongs to another app and is
    // reassigned on every Samsung Health build, so the name is the only stable
    // handle there is.
    @SuppressLint("DiscouragedApi")
    private fun load(kind: ExerciseKind): Bitmap? {
        val resources = context.packageManager.getResourcesForApplication(SAMSUNG_HEALTH_PACKAGE)
        val id = resources.getIdentifier(kind.samsung().icon, "drawable", SAMSUNG_HEALTH_PACKAGE)
        if (id == 0) return null
        return ResourcesCompat.getDrawable(resources, id, null)?.toBitmap()
    }

    private companion object {
        const val TAG = "GymWatch"

        /** A picker screen shows about seven rows; this covers a scroll either way. */
        const val CACHE_ENTRIES = 32
    }
}
