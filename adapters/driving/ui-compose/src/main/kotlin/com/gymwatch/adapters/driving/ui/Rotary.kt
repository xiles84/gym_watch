package com.gymwatch.adapters.driving.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import kotlin.math.absoluteValue
import kotlin.math.sign

/**
 * Rotating bezel / crown as a stepper.
 *
 * Two things are easy to get wrong here:
 *  - the modifier must sit on something *focusable*, and focus has to be
 *    requested, or no events ever arrive;
 *  - raw events are a pixel delta, not a detent. Stepping on every event makes
 *    the counter fly. We accumulate and only emit once past a threshold.
 */
@Composable
internal fun Modifier.rotaryStepper(
    thresholdPx: Float = 40f,
    onStep: (Int) -> Unit,
): Modifier {
    val focusRequester = remember { FocusRequester() }
    val accumulated = remember { floatArrayOf(0f) }

    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    return this
        .onRotaryScrollEvent { event ->
            accumulated[0] += event.verticalScrollPixels
            var emitted = false
            while (accumulated[0].absoluteValue >= thresholdPx) {
                val direction = accumulated[0].sign.toInt()
                accumulated[0] -= direction * thresholdPx
                onStep(direction)
                emitted = true
            }
            emitted
        }
        .focusRequester(focusRequester)
        .focusable()
}
