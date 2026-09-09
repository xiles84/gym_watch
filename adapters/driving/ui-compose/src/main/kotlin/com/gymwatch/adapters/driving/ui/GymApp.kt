package com.gymwatch.adapters.driving.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymwatch.core.application.ChronometerUseCase
import com.gymwatch.core.application.CounterUseCase
import com.gymwatch.core.application.RestTimerUseCase
import com.gymwatch.core.application.StartOutcome
import com.gymwatch.core.application.WorkoutUseCase
import com.gymwatch.core.domain.model.ExerciseKind
import kotlinx.coroutines.launch

/**
 * Four screens on a horizontal pager, always in the same order, with the
 * workout flow layered on top when a session is running or needs a decision.
 *
 * No navigation graph and no menus on purpose: at the gym you should be able to
 * reach anything by swiping without reading the screen.
 */
@Composable
fun GymApp(
    chronometer: ChronometerUseCase,
    restTimer: RestTimerUseCase,
    counter: CounterUseCase,
    workouts: WorkoutUseCase,
    initialPage: Int = 0,
    onWorkoutStateChanged: (Boolean) -> Unit = {},
    ensurePermissions: suspend () -> Boolean = { true },
) {
    val pages = 4
    val pagerState = rememberPagerState(initialPage = initialPage.coerceIn(0, pages - 1)) { pages }
    val scope = rememberCoroutineScope()

    val session by workouts.snapshot.collectAsStateWithLifecycle()
    var pendingConflict by remember { mutableStateOf<ExerciseKind?>(null) }
    var unsupported by remember { mutableStateOf<ExerciseKind?>(null) }

    fun requestStart(kind: ExerciseKind) {
        scope.launch {
            // Health Services refuses to start without the runtime permission,
            // so ask before we try rather than after it fails.
            if (!ensurePermissions()) return@launch
            when (val outcome = workouts.requestStart(kind)) {
                is StartOutcome.NeedsConfirmation -> pendingConflict = outcome.kind
                is StartOutcome.Unsupported -> unsupported = outcome.kind
                is StartOutcome.Started -> onWorkoutStateChanged(true)
                StartOutcome.AlreadyRunning -> Unit
            }
        }
    }

    GymTheme {
        Box(Modifier.fillMaxSize().background(GymColors.Background)) {
            when {
                pendingConflict != null -> ConflictDialog(
                    kind = pendingConflict!!,
                    onConfirm = {
                        val kind = pendingConflict!!
                        pendingConflict = null
                        scope.launch {
                            workouts.forceStart(kind)
                            onWorkoutStateChanged(true)
                        }
                    },
                    onCancel = { pendingConflict = null },
                )

                unsupported != null -> UnsupportedNotice(
                    kind = unsupported!!,
                    onDismiss = { unsupported = null },
                )

                session != null -> WorkoutSessionScreen(
                    snapshot = session!!,
                    onPauseResume = {
                        val snapshot = session ?: return@WorkoutSessionScreen
                        scope.launch {
                            if (snapshot.state == com.gymwatch.core.domain.model.WorkoutState.PAUSED) {
                                workouts.resume()
                            } else {
                                workouts.pause()
                            }
                        }
                    },
                    onEnd = {
                        scope.launch {
                            workouts.end()
                            onWorkoutStateChanged(false)
                        }
                    },
                )

                else -> {
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                        when (page) {
                            0 -> ChronometerScreen(chronometer)
                            1 -> RestTimerScreen(restTimer)
                            2 -> CounterScreen(counter)
                            else -> WorkoutsScreen(workouts, ::requestStart)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        repeat(pages) { index ->
                            Box(
                                Modifier
                                    .size(5.dp)
                                    .background(
                                        if (index == pagerState.currentPage) GymColors.OnSurface
                                        else GymColors.Dim,
                                        CircleShape,
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}
