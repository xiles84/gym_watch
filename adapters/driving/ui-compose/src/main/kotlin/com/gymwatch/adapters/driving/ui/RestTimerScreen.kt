package com.gymwatch.adapters.driving.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.Stable
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
 * ↺ restarts the same length from full; ■ stops and goes back to the presets.
 * Both ask before throwing away a countdown in progress. At zero neither asks:
 * the alarm keeps buzzing until one of them is pressed. If zero arrives while
 * the question is open, the question goes away.
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
    val questions = rememberRestQuestions(useCase, counting)

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Backdrop(
            when {
                alarming -> WallpaperSlot.REST_OVER
                counting -> WallpaperSlot.REST_RUNNING
                else -> WallpaperSlot.REST
            },
        )

        when {
            alarming -> RestAlarm(
                onRestart = { useCase.requestRestart() },
                onStop = { useCase.requestStop() },
            )

            counting -> RunningRest(
                remainingLabel = useCase.remainingNow().asRestLabel(),
                progress = useCase.progressNow(),
                onRestart = questions::restart,
                onStop = questions::stop,
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

    RestQuestionDialog(questions, useCase, counting)
}

/** Which of the two buttons the open question is about. */
internal enum class RestQuestion(val title: String) {
    RESTART("Restart rest?"),
    STOP("Stop rest?"),
}

/**
 * The question ↺ and ■ ask mid-countdown. Shared by every screen that runs the
 * rest timer, so the rules for when to ask cannot drift between them.
 */
@Stable
internal class RestQuestions(private val useCase: RestTimerUseCase) {
    var confirming by mutableStateOf(false)

    /** Kept after the dialog closes so its title does not change mid-exit. */
    var question by mutableStateOf(RestQuestion.STOP)
        private set

    fun restart() = ask(useCase.requestRestart(), RestQuestion.RESTART)

    fun stop() = ask(useCase.requestStop(), RestQuestion.STOP)

    fun confirm() {
        when (question) {
            RestQuestion.RESTART -> useCase.confirmRestart()
            RestQuestion.STOP -> useCase.confirmStop()
        }
        confirming = false
    }

    private fun ask(outcome: ResetOutcome, about: RestQuestion) {
        if (outcome == ResetOutcome.NEEDS_CONFIRMATION) {
            question = about
            confirming = true
        }
    }
}

@Composable
internal fun rememberRestQuestions(useCase: RestTimerUseCase, counting: Boolean): RestQuestions {
    val questions = remember(useCase) { RestQuestions(useCase) }
    // Without this, a question left open when zero arrived would pop up again
    // the moment the next countdown started.
    LaunchedEffect(counting) {
        if (!counting) questions.confirming = false
    }
    return questions
}

@Composable
internal fun RestQuestionDialog(questions: RestQuestions, useCase: RestTimerUseCase, counting: Boolean) {
    ConfirmResetDialog(
        visible = questions.confirming && counting,
        title = questions.question.title,
        detail = "${useCase.remainingNow().asRestLabel()} left",
        onConfirm = questions::confirm,
        onDismiss = { questions.confirming = false },
    )
}

@Composable
private fun RunningRest(
    remainingLabel: String,
    progress: Float,
    onRestart: () -> Unit,
    onStop: () -> Unit,
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

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RoundButton(label = "↺", onClick = onRestart)
            RoundButton(label = "■", onClick = onStop, fontSize = 14)
        }
    }
}

/**
 * Zero. Stays on screen and keeps buzzing — the use case repeats the haptic —
 * until restarted or stopped, because one buzz is easy to miss. ■ is the bigger
 * button and in the rest colour: silencing it is the usual answer. ↺ beside it
 * goes straight into another rest of the same length.
 */
@Composable
private fun RestAlarm(onRestart: () -> Unit, onStop: () -> Unit) {
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

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundButton(label = "↺", onClick = onRestart)
            RoundButton(
                label = "■",
                onClick = onStop,
                size = 56,
                fontSize = 18,
                background = GymColors.Rest,
                contentColor = GymColors.Background,
            )
        }

        Spacer(Modifier.height(6.dp))
        Text("↺ rest again · ■ stop", color = GymColors.Dim, fontSize = 9.sp)
    }
}

/**
 * The countdown around the rim. By default the whole circle from twelve
 * o'clock; [startAngle] and [span] give a partial arc, measured clockwise from
 * three o'clock as Compose does.
 */
@Composable
internal fun RestRing(progress: Float, startAngle: Float = -90f, span: Float = 360f) {
    // A Canvas block is not a composable scope, so the palette is read here
    // and closed over rather than looked up per draw.
    val palette = LocalPalette.current

    Canvas(Modifier.fillMaxSize().padding(6.dp)) {
        val stroke = 8f
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        // A band twice the track's width under it, in the background black. On
        // a plain skin it vanishes; over a wallpaper it is what the ring stands
        // out against, instead of whatever the picture has at the rim.
        drawArc(
            color = palette.background,
            startAngle = startAngle,
            sweepAngle = span,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = stroke * 2),
        )
        drawArc(
            color = palette.surface,
            startAngle = startAngle,
            sweepAngle = span,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = stroke),
        )
        drawArc(
            color = palette.rest,
            startAngle = startAngle,
            sweepAngle = span * progress,
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
internal fun PresetButton(
    label: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    size: Int = 52,
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(GymColors.Surface, CircleShape)
            .border(1.dp, GymColors.Outline, CircleShape)
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
