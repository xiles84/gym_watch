package com.gymwatch.adapters.driving.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Text
import com.gymwatch.core.application.RestTimerUseCase

/**
 * Three rest lengths, one tap each.
 *
 * There is no free adjustment on this screen on purpose: this watch has no
 * rotating bezel, so the old "turn to set" interaction never worked here. Every
 * length is now a target you can hit without looking (docs/LESSONS.md #24).
 */
@Composable
fun RestTimerScreen(
    useCase: RestTimerUseCase,
    onEditPreset: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val timer by useCase.state.collectAsStateWithLifecycle()
    val presets by useCase.presets.collectAsStateWithLifecycle()
    val tick by rememberTick(active = timer.isRunning, periodMs = 200L)

    // Reading `tick` schedules the redraw; the value is discarded. Remaining
    // time always comes from the clock, never from the tick.
    @Suppress("UNUSED_EXPRESSION") tick

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

        if (timer.isRunning) {
            RunningRest(
                remainingLabel = useCase.remainingNow().asRestLabel(),
                progress = useCase.progressNow(),
                onCancel = useCase::cancel,
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("REST", color = GymColors.Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    presets.durations.forEachIndexed { index, duration ->
                        PresetButton(
                            label = duration.asRestLabel(),
                            onClick = { useCase.start(index) },
                            onLongClick = { onEditPreset(index) },
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    "tap to start · hold to edit",
                    color = GymColors.Dim,
                    fontSize = 9.sp,
                )
            }
        }
    }
}

@Composable
private fun RunningRest(
    remainingLabel: String,
    progress: Float,
    onCancel: () -> Unit,
) {
    Canvas(Modifier.fillMaxSize().padding(6.dp)) {
        val stroke = 8f
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(
            color = GymColors.Surface,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = stroke),
        )
        drawArc(
            color = GymColors.Rest,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = stroke),
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("REST", color = GymColors.Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
        Text(
            text = remainingLabel,
            color = GymColors.Rest,
            fontSize = 38.sp,
            fontWeight = FontWeight.Medium,
        )
        Text("buzzes at zero", color = GymColors.Dim, fontSize = 10.sp)

        Spacer(Modifier.height(10.dp))

        RoundButton(
            label = "✕",
            onClick = onCancel,
            background = GymColors.Surface,
            contentColor = GymColors.OnSurface,
        )
    }
}

/**
 * 52dp so it stays hittable with a sweaty thumb, and long-pressable so editing
 * never happens by accident mid-set.
 */
@Composable
private fun PresetButton(
    label: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .background(GymColors.Surface, CircleShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = GymColors.Rest,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
    }
}
