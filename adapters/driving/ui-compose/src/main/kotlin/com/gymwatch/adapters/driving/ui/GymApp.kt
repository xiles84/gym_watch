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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymwatch.core.application.ChronometerUseCase
import com.gymwatch.core.application.CounterUseCase
import com.gymwatch.core.application.RestTimerUseCase
import com.gymwatch.core.application.WorkoutUseCase
import com.gymwatch.core.domain.model.ExerciseKind

/**
 * Four screens on a horizontal pager, always in the same order.
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
    onStartWorkout: (ExerciseKind) -> Unit,
) {
    val pages = 4
    val pagerState = rememberPagerState(initialPage = 0) { pages }

    GymTheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(GymColors.Background),
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    0 -> ChronometerScreen(chronometer)
                    1 -> RestTimerScreen(restTimer)
                    2 -> CounterScreen(counter)
                    else -> WorkoutsScreen(workouts, onStartWorkout)
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
