package com.gymwatch.adapters.driving.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.gymwatch.core.domain.model.WorkoutSnapshot
import com.gymwatch.core.domain.model.WorkoutState

/** Shown while Health Services is recording. Metrics are null until sensors settle. */
@Composable
fun WorkoutSessionScreen(
    snapshot: WorkoutSnapshot,
    onPauseResume: () -> Unit,
    onEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val paused = snapshot.state == WorkoutState.PAUSED

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = snapshot.kind.glyph + " " + snapshot.kind.displayName.uppercase(),
            color = GymColors.Muted,
            fontSize = 10.sp,
            letterSpacing = 1.2.sp,
        )
        Text(
            text = snapshot.activeDuration.asClock(),
            color = if (paused) GymColors.Muted else GymColors.OnSurface,
            fontSize = 34.sp,
            fontWeight = FontWeight.Medium,
        )

        Spacer(Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Metric(snapshot.heartRateBpm?.toString() ?: "--", "bpm", GymColors.Danger)
            Metric(snapshot.calories?.toString() ?: "--", "kcal", GymColors.OnSurface)
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RoundButton(
                label = if (paused) "▶" else "II",
                onClick = onPauseResume,
                size = 44,
                fontSize = 16,
            )
            RoundButton(
                label = "■",
                onClick = onEnd,
                size = 44,
                fontSize = 14,
                background = GymColors.StopBackground,
                contentColor = GymColors.Danger,
            )
        }

        if (snapshot.state == WorkoutState.PREPARING) {
            Spacer(Modifier.height(6.dp))
            Text("warming up sensors", color = GymColors.Dim, fontSize = 9.sp)
        }
    }
}

@Composable
private fun Metric(value: String, unit: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Text(" " + unit, color = GymColors.Dim, fontSize = 9.sp)
    }
}
