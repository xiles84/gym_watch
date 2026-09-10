package com.gymwatch.adapters.driving.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Picker
import androidx.wear.compose.material3.PickerGroup
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

    var selectedColumn by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title.uppercase(),
            color = GymColors.Muted,
            fontSize = 10.sp,
            letterSpacing = 1.5.sp,
        )

        Spacer(Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            PickerGroup(
                selectedPickerState = if (selectedColumn == 0) minuteState else secondState,
            ) {
                PickerGroupItem(
                    pickerState = minuteState,
                    selected = selectedColumn == 0,
                    onSelected = { selectedColumn = 0 },
                    modifier = Modifier.width(56.dp),
                    contentDescription = { "${minuteState.selectedOptionIndex} minutes" },
                ) { optionIndex, _ ->
                    PickerOption(
                        text = optionIndex.toString(),
                        highlighted = optionIndex == minuteState.selectedOptionIndex,
                    )
                }
                PickerGroupItem(
                    pickerState = secondState,
                    selected = selectedColumn == 1,
                    onSelected = { selectedColumn = 1 },
                    modifier = Modifier.width(56.dp),
                    contentDescription = { "${secondState.selectedOptionIndex * SECOND_STEP} seconds" },
                ) { optionIndex, _ ->
                    PickerOption(
                        text = "%02d".format(optionIndex * SECOND_STEP),
                        highlighted = optionIndex == secondState.selectedOptionIndex,
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text("swipe each column", color = GymColors.Dim, fontSize = 9.sp)
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
