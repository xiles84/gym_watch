package com.gymwatch.adapters.driving.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Text
import com.gymwatch.core.application.CounterUseCase
import com.gymwatch.core.application.RestTimerUseCase
import kotlin.time.Duration

/**
 * The rest timer above, the set counter below: between sets you tap + and start
 * the rest, and this saves the swipe between them.
 *
 * Nothing here holds state of its own. Both halves read the same use cases as
 * the REST and COUNTER screens, so a rest started on one page is already running
 * on the other, and the confirm rules are the shared [RestQuestions].
 *
 * Everything is smaller than on the dedicated screens, but no target is under
 * 40dp. The rest ring becomes a half ring over the top, so it does not seem to
 * belong to the counter.
 */
@Composable
fun RestAndCounterScreen(
    restTimer: RestTimerUseCase,
    counter: CounterUseCase,
    onEditPreset: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val timer by restTimer.state.collectAsStateWithLifecycle()
    val presets by restTimer.presets.collectAsStateWithLifecycle()
    val sets by counter.state.collectAsStateWithLifecycle()
    val tick by rememberTick(active = timer.isRunning, periodMs = 200L)

    // As on the rest screen: reading `tick` schedules the redraw, the clock
    // supplies the value.
    @Suppress("UNUSED_EXPRESSION") tick
    val alarming = restTimer.isAlarmingNow()
    val counting = restTimer.needsResetConfirmationNow()
    val questions = rememberRestQuestions(restTimer, counting)

    Box(modifier = modifier.fillMaxSize()) {
        when {
            alarming -> RestRing(progress = 1f, startAngle = TOP_HALF_START, span = TOP_HALF_SPAN)
            counting -> RestRing(restTimer.progressNow(), startAngle = TOP_HALF_START, span = TOP_HALF_SPAN)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                // A bonus, as on the counter screen: there is no bezel that
                // turns on this watch (docs/LESSONS.md #24).
                .rotaryStepper { direction -> counter.stepBy(direction) },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(bottom = 10.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                when {
                    alarming -> CompactRest(
                        label = "REST OVER",
                        remaining = Duration.ZERO.asRestLabel(),
                        onRestart = { restTimer.requestRestart() },
                        onStop = { restTimer.requestStop() },
                        ringing = true,
                    )

                    counting -> CompactRest(
                        label = "REST",
                        remaining = restTimer.remainingNow().asRestLabel(),
                        onRestart = questions::restart,
                        onStop = questions::stop,
                        ringing = false,
                    )

                    else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        HalfLabel("REST")
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            presets.durations.forEachIndexed { index, duration ->
                                PresetButton(
                                    label = duration.asRestLabel(),
                                    onClick = { restTimer.start(index) },
                                    onLongClick = { onEditPreset(index) },
                                    size = 46,
                                )
                            }
                        }
                    }
                }
            }

            Box(
                Modifier
                    .width(140.dp)
                    .height(1.dp)
                    .background(GymColors.Dim),
            )

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    HalfLabel("SETS")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        RoundButton(label = "−", onClick = counter::decrement, size = 44, fontSize = 20)
                        Text(
                            text = sets.value.toString(),
                            color = GymColors.OnSurface,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                // Long-press, as on the counter screen: a tap
                                // here mid-set must not zero it.
                                .combinedClickable(onClick = {}, onLongClick = counter::reset),
                        )
                        RoundButton(label = "+", onClick = counter::increment, size = 44, fontSize = 20)
                    }
                    Text("hold number to reset", color = GymColors.Dim, fontSize = 9.sp)
                }
            }
        }
    }

    RestQuestionDialog(questions, restTimer, counting)
}

/**
 * ↺, the time, ■ in one row. At zero ■ grows and takes the rest colour, as on
 * the rest screen's alarm, because silencing it is the usual answer.
 */
@Composable
private fun CompactRest(
    label: String,
    remaining: String,
    onRestart: () -> Unit,
    onStop: () -> Unit,
    ringing: Boolean,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HalfLabel(label, color = if (ringing) GymColors.Rest else GymColors.Muted)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            RoundButton(label = "↺", onClick = onRestart, size = 40, fontSize = 16)
            // Fixed width, so the buttons do not shuffle as the digits change.
            Text(
                text = remaining,
                color = GymColors.Rest,
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(84.dp),
            )
            if (ringing) {
                RoundButton(
                    label = "■",
                    onClick = onStop,
                    size = 48,
                    fontSize = 16,
                    background = GymColors.Rest,
                    contentColor = GymColors.Background,
                )
            } else {
                RoundButton(label = "■", onClick = onStop, size = 40, fontSize = 12)
            }
        }
    }
}

@Composable
private fun HalfLabel(text: String, color: Color = GymColors.Muted) {
    Text(text, color = color, fontSize = 10.sp, letterSpacing = 1.5.sp)
}

/** Nine o'clock round to three, over the top. */
private const val TOP_HALF_START = 180f
private const val TOP_HALF_SPAN = 180f
