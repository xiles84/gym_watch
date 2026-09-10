package com.gymwatch.adapters.driving.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Text
import com.gymwatch.core.application.RestTimerUseCase
import com.gymwatch.core.domain.model.RestTimer

@Composable
fun RestTimerScreen(
    useCase: RestTimerUseCase,
    modifier: Modifier = Modifier,
) {
    val timer by useCase.state.collectAsStateWithLifecycle()
    val tick by rememberTick(active = timer.isRunning, periodMs = 200L)

    @Suppress("UNUSED_EXPRESSION") tick
    val remaining = useCase.remainingNow()
    val progress = useCase.progressNow()

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

        if (timer.isRunning) {
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
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.rotaryStepper { direction ->
                useCase.adjustBy(RestTimer.STEP * direction)
            },
        ) {
            Text("REST", color = GymColors.Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
            Text(
                text = (if (timer.isRunning) remaining else timer.duration).asRestLabel(),
                color = GymColors.Rest,
                fontSize = 38.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = if (timer.isRunning) "buzzes at zero" else "↻ bezel to set",
                color = GymColors.Dim,
                fontSize = 10.sp,
            )

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RoundButton(
                    label = if (timer.isRunning) "✕" else "▶",
                    onClick = useCase::toggle,
                    background = if (timer.isRunning) GymColors.Surface else GymColors.Rest,
                    contentColor = if (timer.isRunning) GymColors.OnSurface else GymColors.Background,
                )
            }
        }
    }
}
