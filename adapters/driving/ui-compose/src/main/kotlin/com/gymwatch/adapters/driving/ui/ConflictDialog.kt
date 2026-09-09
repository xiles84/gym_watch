package com.gymwatch.adapters.driving.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.gymwatch.core.domain.model.ExerciseKind

/**
 * Health Services allows exactly one exercise device-wide. If Samsung Health is
 * already tracking, starting ours ends theirs — so we ask rather than doing it
 * silently. Losing a workout you thought was recording is not a recoverable
 * mistake.
 */
@Composable
fun ConflictDialog(
    kind: ExerciseKind,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().background(GymColors.Background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("⚠", color = GymColors.Rest, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Another app is already tracking a workout.",
                color = GymColors.OnSurface,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Starting " + kind.displayName + " will end it.",
                color = GymColors.Muted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.width(74.dp)) {
                    PillButton(label = "Cancel", onClick = onCancel)
                }
                Box(Modifier.width(74.dp)) {
                    PillButton(
                        label = "Start",
                        onClick = onConfirm,
                        background = GymColors.Go,
                        contentColor = GymColors.Background,
                    )
                }
            }
        }
    }
}

/** Shown when this watch cannot track the exercise the user picked. */
@Composable
fun UnsupportedNotice(kind: ExerciseKind, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().background(GymColors.Background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "This watch cannot track " + kind.displayName + ".",
                color = GymColors.OnSurface,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Box(Modifier.width(90.dp)) {
                PillButton(label = "OK", onClick = onDismiss)
            }
        }
    }
}
