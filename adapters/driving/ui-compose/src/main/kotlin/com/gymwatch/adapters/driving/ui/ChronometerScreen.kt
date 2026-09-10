package com.gymwatch.adapters.driving.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Text
import com.gymwatch.core.application.ChronometerUseCase
import com.gymwatch.core.domain.model.ResetOutcome

/**
 * Count-up clock. ↺ resets a paused chronometer at once and asks first while
 * one is running, so a stray tap cannot wipe a set being timed.
 */
@Composable
fun ChronometerScreen(
    useCase: ChronometerUseCase,
    modifier: Modifier = Modifier,
) {
    val chrono by useCase.state.collectAsStateWithLifecycle()
    val tick by rememberTick(active = chrono.isRunning)

    // Reading `tick` here is what schedules the redraw; the value itself is
    // discarded. Elapsed time always comes from the clock, never from the tick.
    @Suppress("UNUSED_EXPRESSION") tick
    val elapsed = useCase.elapsedNow()
    val lap = useCase.currentLapNow()

    var confirmingReset by remember { mutableStateOf(false) }
    // A question that no longer applies must not reappear the next time it does.
    LaunchedEffect(chrono.needsResetConfirmation) {
        if (!chrono.needsResetConfirmation) confirmingReset = false
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("CHRONO", color = GymColors.Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
        Text(
            text = elapsed.asClock(),
            color = GymColors.Chrono,
            fontSize = 40.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = if (chrono.laps.isEmpty()) " " else "lap ${chrono.laps.size + 1} · ${lap.asClock()}",
            color = GymColors.Dim,
            fontSize = 11.sp,
        )

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RoundButton(
                label = if (chrono.isRunning) "II" else "▶",
                onClick = useCase::toggle,
                background = if (chrono.isRunning) GymColors.Surface else GymColors.Go,
                contentColor = if (chrono.isRunning) GymColors.OnSurface else GymColors.Background,
            )
            RoundButton(
                label = "⚑",
                onClick = useCase::lap,
                size = 40,
                fontSize = 14,
                contentColor = if (chrono.isRunning) GymColors.OnSurface else GymColors.Dim,
            )
            RoundButton(
                label = "↺",
                onClick = {
                    if (useCase.requestReset() == ResetOutcome.NEEDS_CONFIRMATION) confirmingReset = true
                },
                size = 40,
                fontSize = 14,
            )
        }
    }

    ConfirmResetDialog(
        visible = confirmingReset && chrono.needsResetConfirmation,
        title = "Reset chrono?",
        detail = "${elapsed.asClock()} is still running",
        onConfirm = {
            useCase.confirmReset()
            confirmingReset = false
        },
        onDismiss = { confirmingReset = false },
    )
}
