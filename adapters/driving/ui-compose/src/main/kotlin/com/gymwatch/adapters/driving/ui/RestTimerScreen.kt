package com.gymwatch.adapters.driving.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.gymwatch.core.domain.model.ResetOutcome
import kotlin.time.Duration

/**
 * Three rest lengths, one tap each; then the countdown; then the alarm.
 *
 * There is no free adjustment on this screen on purpose: this watch has no
 * rotating bezel, so the old "turn to set" interaction never worked here. Every
 * length is a target you can hit without looking (docs/LESSONS.md #24).
 *
 * ↺ asks before cancelling a countdown in progress. At zero it does not ask:
 * the alarm keeps buzzing until reset, and silencing it is the only thing left
 * to do. If zero arrives while the question is open, the question goes away.
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
    // time and the switch to the alarm always come from the clock.
    @Suppress("UNUSED_EXPRESSION") tick
    val alarming = useCase.isAlarmingNow()
    val counting = useCase.needsResetConfirmationNow()

    var confirmingReset by remember { mutableStateOf(false) }
    // Without this, a question left open when zero arrived would pop up again
    // the moment the next countdown started.
    LaunchedEffect(counting) {
        if (!counting) confirmingReset = false
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            alarming -> RestAlarm(onReset = { useCase.requestReset() })

            counting -> RunningRest(
                remainingLabel = useCase.remainingNow().asRestLabel(),
                progress = useCase.progressNow(),
                onReset = {
                    if (useCase.requestReset() == ResetOutcome.NEEDS_CONFIRMATION) confirmingReset = true
                },
            )

            else -> Column(
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

    ConfirmResetDialog(
        visible = confirmingReset && counting,
        title = "Reset rest?",
        detail = "${useCase.remainingNow().asRestLabel()} left",
        onConfirm = {
            useCase.confirmReset()
            confirmingReset = false
        },
        onDismiss = { confirmingReset = false },
    )
}

@Composable
private fun RunningRest(
    remainingLabel: String,
    progress: Float,
    onReset: () -> Unit,
) {
    RestRing(progress)

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
            label = "↺",
            onClick = onReset,
            background = GymColors.Surface,
            contentColor = GymColors.OnSurface,
        )
    }
}

/**
 * Zero. Stays on screen and keeps buzzing — the use case repeats the haptic —
 * until reset, because one buzz is easy to miss. The button is bigger and in the
 * rest colour: it is the only thing on the screen worth pressing.
 */
@Composable
private fun RestAlarm(onReset: () -> Unit) {
    RestRing(progress = 1f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("REST OVER", color = GymColors.Rest, fontSize = 10.sp, letterSpacing = 1.5.sp)
        Text(
            text = Duration.ZERO.asRestLabel(),
            color = GymColors.Rest,
            fontSize = 38.sp,
            fontWeight = FontWeight.Medium,
        )

        Spacer(Modifier.height(8.dp))

        RoundButton(
            label = "↺",
            onClick = onReset,
            size = 56,
            fontSize = 22,
            background = GymColors.Rest,
            contentColor = GymColors.Background,
        )

        Spacer(Modifier.height(6.dp))
        Text("tap to stop", color = GymColors.Dim, fontSize = 9.sp)
    }
}

@Composable
private fun RestRing(progress: Float) {
    // A Canvas block is not a composable scope, so the palette is read here
    // and closed over rather than looked up per draw.
    val palette = LocalPalette.current

    Canvas(Modifier.fillMaxSize().padding(6.dp)) {
        val stroke = 8f
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(
            color = palette.surface,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = stroke),
        )
        drawArc(
            color = palette.rest,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = stroke),
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
