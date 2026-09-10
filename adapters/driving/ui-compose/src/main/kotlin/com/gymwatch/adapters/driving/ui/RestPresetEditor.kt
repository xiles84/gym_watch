package com.gymwatch.adapters.driving.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Picker
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.rememberPickerState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Sets one rest length with two swipeable columns.
 *
 * Deliberately not a plus/minus stepper and deliberately not the rotating
 * bezel: this watch has no bezel that turns, and a column you flick is a much
 * bigger target than a small button. [Picker] also brings rotary snapping for
 * free via `PickerDefaults.rotarySnapBehavior`, so if the touch bezel does feed
 * rotary events it just works — without this screen depending on it.
 *
 * Seconds move in fives. Sixty stops would be precision nobody needs between
 * sets, and it would take several flicks to cross.
 */
@Composable
fun RestPresetEditor(
    title: String,
    initial: Duration,
    onConfirm: (Duration) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialSeconds = initial.inWholeSeconds.coerceAtLeast(0)

    val minuteState = rememberPickerState(
        initialNumberOfOptions = MINUTE_OPTIONS,
        initiallySelectedIndex = (initialSeconds / 60).toInt().coerceIn(0, MINUTE_OPTIONS - 1),
        // Wrapping 15 back round to 0 would be a nasty surprise on a bounded range.
        shouldRepeatOptions = false,
    )
    val secondState = rememberPickerState(
        initialNumberOfOptions = SECOND_OPTIONS,
        initiallySelectedIndex = ((initialSeconds % 60) / SECOND_STEP).toInt()
            .coerceIn(0, SECOND_OPTIONS - 1),
    )


    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title.uppercase(),
            color = GymColors.Muted,
            fontSize = 10.sp,
            letterSpacing = 1.5.sp,
        )

        Spacer(Modifier.height(6.dp))

        // Two plain Pickers rather than a PickerGroup: a group centres whichever
        // column is "selected" and renders the other read-only, which shunts the
        // pair off-centre and misaligns the rows. Here both columns stay live,
        // aligned, and flickable — you set minutes and seconds without first
        // tapping to choose a column.
        //
        // The height is essential: a Picker fills whatever it is given, so
        // unconstrained it eats the screen and pushes everything else off.
        Row(
            modifier = Modifier.height(92.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Picker(
                state = minuteState,
                contentDescription = { "${minuteState.selectedOptionIndex} minutes" },
                modifier = Modifier.width(50.dp),
                gradientColor = GymColors.PickerFade,
            ) { optionIndex ->
                PickerOption(
                    text = optionIndex.toString(),
                    highlighted = optionIndex == minuteState.selectedOptionIndex,
                )
            }

            Text(":", color = GymColors.Rest, fontSize = 22.sp)

            Picker(
                state = secondState,
                contentDescription = { "${secondState.selectedOptionIndex * SECOND_STEP} seconds" },
                modifier = Modifier.width(50.dp),
                gradientColor = GymColors.PickerFade,
            ) { optionIndex ->
                PickerOption(
                    text = "%02d".format(optionIndex * SECOND_STEP),
                    highlighted = optionIndex == secondState.selectedOptionIndex,
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Text("min · sec", color = GymColors.Dim, fontSize = 9.sp)
        Spacer(Modifier.height(8.dp))

        RoundButton(
            label = "✓",
            onClick = {
                val total = minuteState.selectedOptionIndex * 60 +
                    secondState.selectedOptionIndex * SECOND_STEP
                // The domain clamps to RestTimer.MIN/MAX, so 0:00 becomes the
                // 5s floor rather than an un-runnable timer.
                onConfirm(total.seconds)
            },
            size = 44,
            fontSize = 18,
            background = GymColors.Go,
            contentColor = GymColors.Background,
        )
    }
}

@Composable
private fun PickerOption(text: String, highlighted: Boolean) {
    Text(
        text = text,
        color = if (highlighted) GymColors.Rest else GymColors.Muted,
        fontSize = if (highlighted) 26.sp else 18.sp,
        fontWeight = if (highlighted) FontWeight.Medium else FontWeight.Normal,
    )
}

/** 0 to 15 minutes — 15 is RestTimer.MAX, so the column cannot exceed it. */
private const val MINUTE_OPTIONS = 16

/** 00, 05, ... 55. */
private const val SECOND_OPTIONS = 12
private const val SECOND_STEP = 5
