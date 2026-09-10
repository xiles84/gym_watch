package com.gymwatch.adapters.driving.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.gymwatch.core.domain.model.CounterLabel
import com.gymwatch.core.domain.model.ExerciseKind
import com.gymwatch.core.domain.model.WorkoutProfile

/**
 * Edits one profile: which workout it is, its three rest lengths, and what the
 * counter counts.
 *
 * Everything is chosen from a list or a wheel. There is no text entry anywhere,
 * because typing on a watch means a keyboard or dictation and neither works
 * with chalk on your hands.
 *
 * Sub-editors are kept internal so the caller only ever sees a finished
 * [WorkoutProfile].
 */
@Composable
fun ProfileEditor(
    initial: WorkoutProfile,
    onConfirm: (WorkoutProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember(initial) { mutableStateOf(initial) }
    var pickingKind by remember { mutableStateOf(false) }
    var editingPreset by remember { mutableStateOf<Int?>(null) }

    val preset = editingPreset

    when {
        pickingKind -> KindPicker(
            selected = draft.kind,
            onPick = { kind ->
                draft = draft.copy(kind = kind)
                pickingKind = false
            },
            modifier = modifier,
        )

        preset != null -> RestPresetEditor(
            title = "rest ${preset + 1}",
            initial = draft.restPresets[preset],
            onConfirm = { duration ->
                draft = draft.copy(restPresets = draft.restPresets.withDurationAt(preset, duration))
                editingPreset = null
            },
            modifier = modifier,
        )

        else -> Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("EDIT", color = GymColors.Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(8.dp))

            SettingRow(
                label = "workout",
                value = draft.kind.glyph + " " + draft.kind.displayName,
                onClick = { pickingKind = true },
            )

            Spacer(Modifier.height(8.dp))
            Text("REST", color = GymColors.Muted, fontSize = 9.sp, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                draft.restPresets.durations.forEachIndexed { index, duration ->
                    Box(
                        modifier = Modifier
                            .background(GymColors.Surface, RoundedCornerShape(percent = 50))
                            .clickable { editingPreset = index }
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                    ) {
                        Text(duration.asRestLabel(), color = GymColors.Rest, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            SettingRow(
                label = "counter",
                value = draft.counterLabel.name,
                // Four options: cycling in place beats another sub-screen.
                onClick = {
                    val next = CounterLabel.entries[
                        (draft.counterLabel.ordinal + 1) % CounterLabel.entries.size,
                    ]
                    draft = draft.copy(counterLabel = next)
                },
            )

            Spacer(Modifier.height(12.dp))

            RoundButton(
                label = "✓",
                onClick = { onConfirm(draft) },
                size = 44,
                fontSize = 18,
                background = GymColors.Go,
                contentColor = GymColors.Background,
            )
        }
    }
}

@Composable
private fun SettingRow(label: String, value: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GymColors.Surface, RoundedCornerShape(percent = 50))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = GymColors.Dim, fontSize = 10.sp)
            Text(value, color = GymColors.OnSurface, fontSize = 12.sp)
        }
    }
}

/** Every kind this app knows. The watch no longer limits the list, because we
 *  are not asking it to track anything — the kind is a label and a set of rest
 *  times, so nothing here can be "unsupported". */
@Composable
private fun KindPicker(
    selected: ExerciseKind,
    onPick: (ExerciseKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("WORKOUT", color = GymColors.Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(6.dp))

        ExerciseKind.entries.forEach { kind ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .background(
                        if (kind == selected) GymColors.Rest else GymColors.Surface,
                        RoundedCornerShape(percent = 50),
                    )
                    .clickable { onPick(kind) }
                    .padding(vertical = 9.dp, horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = kind.glyph + "  " + kind.displayName,
                    color = if (kind == selected) GymColors.Background else GymColors.OnSurface,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
