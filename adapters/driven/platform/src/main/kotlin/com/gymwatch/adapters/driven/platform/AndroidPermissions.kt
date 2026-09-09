package com.gymwatch.adapters.driven.platform

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.gymwatch.core.domain.model.HealthPermission
import com.gymwatch.core.domain.port.PermissionsPort

/**
 * Hides the API-36 health permission split from the rest of the app.
 *
 * Below API 36 heart rate is guarded by BODY_SENSORS; from 36 onward it is the
 * granular android.permission.health.READ_HEART_RATE. Declaring both in the
 * manifest is not enough — you must also *request* the right one at runtime for
 * the level you are on. See docs/LESSONS.md #5.
 */
class AndroidPermissions(private val context: Context) : PermissionsPort {

    override suspend fun granted(permission: HealthPermission): Boolean =
        manifestNames(permission).all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    /**
     * Actually showing the dialog needs an Activity, so the UI layer drives the
     * launcher. This reports whether anything is still outstanding.
     */
    override suspend fun requestAll(): Boolean =
        HealthPermission.entries.all { granted(it) }

    fun outstanding(): List<String> =
        HealthPermission.entries
            .flatMap(::manifestNames)
            .distinct()
            .filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }

    private fun manifestNames(permission: HealthPermission): List<String> =
        when (permission) {
            HealthPermission.ACTIVITY_RECOGNITION ->
                listOf("android.permission.ACTIVITY_RECOGNITION")

            HealthPermission.HEART_RATE ->
                if (Build.VERSION.SDK_INT >= API_GRANULAR_HEALTH) {
                    listOf("android.permission.health.READ_HEART_RATE")
                } else {
                    listOf("android.permission.BODY_SENSORS")
                }
        }

    private companion object {
        /** Wear OS 6 / Android 16. */
        const val API_GRANULAR_HEALTH = 36
    }
}
