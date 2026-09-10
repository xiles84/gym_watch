package com.gymwatch.adapters.driven.platform

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.gymwatch.core.domain.model.Haptic
import com.gymwatch.core.domain.port.HapticsPort

/**
 * Wrist feedback. Patterns are short and distinct so they can be told apart
 * without looking at the watch — which is the whole point mid-set.
 *
 * No SDK_INT branch: minSdk is 33, so VibratorManager (API 31) always exists.
 */
class AndroidHaptics(context: Context) : HapticsPort {

    private val vibrator: Vibrator? =
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
            ?.defaultVibrator

    override fun play(haptic: Haptic) {
        val v = vibrator?.takeIf { it.hasVibrator() } ?: return
        val effect = when (haptic) {
            Haptic.TICK -> VibrationEffect.createOneShot(20, LIGHT)
            Haptic.CONFIRM -> VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE)
            // Rest is over: unmistakable, because you are not looking at it.
            Haptic.REST_OVER -> VibrationEffect.createWaveform(REST_PATTERN, -1)
        }
        v.vibrate(effect)
    }

    private companion object {
        const val LIGHT = 80
        val REST_PATTERN = longArrayOf(0, 250, 150, 250, 150, 400)
    }
}
