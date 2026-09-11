package com.gymwatch.adapters.driving.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymwatch.core.application.ChronometerUseCase
import com.gymwatch.core.application.CounterUseCase
import com.gymwatch.core.application.RestTimerUseCase
import com.gymwatch.core.application.ScreenLayoutUseCase
import com.gymwatch.core.application.SkinUseCase
import com.gymwatch.core.application.WorkoutSetupUseCase
import com.gymwatch.core.domain.model.AppScreen

/** Which editor, if any, is covering the pager. */
private sealed interface Editor {
    data class Preset(val index: Int) : Editor
    data class Shortcut(val index: Int) : Editor
}

/**
 * The screens the user chose, in the order they chose, with settings always
 * appended last.
 *
 * Still no navigation graph and no menus: at the gym you should be able to
 * reach anything by swiping without reading the screen. What changed is that
 * the set of screens is now yours to pick — a page you never use is a page you
 * should not have to swipe past.
 */
@Composable
fun GymApp(
    chronometer: ChronometerUseCase,
    restTimer: RestTimerUseCase,
    counter: CounterUseCase,
    workouts: WorkoutSetupUseCase,
    workoutIcons: WorkoutIcons,
    screenLayout: ScreenLayoutUseCase,
    skins: SkinUseCase,
    requestedScreen: AppScreen? = null,
    onScreenHandled: () -> Unit = {},
) {
    val layout by screenLayout.state.collectAsStateWithLifecycle()
    val setup by workouts.state.collectAsStateWithLifecycle()
    val skin by skins.state.collectAsStateWithLifecycle()

    val visible = layout.visible
    // Settings is appended rather than being an AppScreen, so it can never be
    // hidden by the screen that does the hiding.
    val pageCount = visible.size + 1

    val startPage = requestedScreen
        ?.let { visible.indexOf(it) }
        ?.takeIf { it >= 0 }
        ?: 0

    val pagerState = rememberPagerState(initialPage = startPage) { pageCount }

    var editing by remember { mutableStateOf<Editor?>(null) }

    // A request can also arrive while the app is already open — tapping the
    // watch-face indicator delivers a new intent rather than recreating the
    // Activity, so honouring it only in initialPage would silently do nothing.
    LaunchedEffect(requestedScreen, visible) {
        val target = requestedScreen ?: return@LaunchedEffect
        val index = visible.indexOf(target)
        if (index >= 0) {
            editing = null
            pagerState.animateScrollToPage(index)
        }
        onScreenHandled()
    }

    // The watch's back button closes an editor instead of leaving the app.
    // Without a handler, back finished the Activity from inside the rest editor.
    // Nothing is saved: ✓ is the only way to keep a change, so backing out is
    // also how to cancel one.
    BackHandler(enabled = editing != null) { editing = null }

    GymTheme(skin) {
        Box(Modifier.fillMaxSize().background(GymColors.Background)) {
            when (val editor = editing) {
                is Editor.Preset -> {
                    Backdrop(WallpaperSlot.REST_EDITOR)
                    RestPresetEditor(
                        title = "rest",
                        initial = setup.restPresets[editor.index],
                        onConfirm = { duration ->
                            workouts.setRestPreset(editor.index, duration)
                            editing = null
                        },
                    )
                }

                is Editor.Shortcut -> {
                    Backdrop(WallpaperSlot.WORKOUT_PICKER)
                    WorkoutPicker(
                        selected = setup.shortcutAt(editor.index),
                        icons = workoutIcons,
                        onPick = { kind ->
                            workouts.setShortcut(editor.index, kind)
                            editing = null
                        },
                    )
                }

                null -> {
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                        val screen = visible.getOrNull(page)

                        Box(Modifier.fillMaxSize()) {
                            // The rest timer draws its own: only it knows whether
                            // it is counting down or ringing.
                            if (screen != AppScreen.REST_TIMER) Backdrop(wallpaperSlotOf(screen))

                            when (screen) {
                                AppScreen.CHRONOMETER -> ChronometerScreen(chronometer)

                                AppScreen.REST_TIMER -> RestTimerScreen(
                                    useCase = restTimer,
                                    onEditPreset = { editing = Editor.Preset(it) },
                                )

                                AppScreen.COUNTER -> CounterScreen(counter)

                                AppScreen.REST_AND_COUNTER -> RestAndCounterScreen(
                                    restTimer = restTimer,
                                    counter = counter,
                                    onEditPreset = { editing = Editor.Preset(it) },
                                )

                                AppScreen.WORKOUTS -> WorkoutsScreen(
                                    useCase = workouts,
                                    icons = workoutIcons,
                                    onChangeShortcut = { editing = Editor.Shortcut(it) },
                                )

                                // Past the last visible screen: the settings page.
                                null -> SettingsScreen(screenLayout, skins)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp),
                    ) {
                        repeat(pageCount) { index ->
                            // Each dot sits on a dark disc: invisible on black,
                            // and what keeps it visible on a bright patch of art.
                            Box(
                                Modifier
                                    .size(9.dp)
                                    .background(Color.Black.copy(alpha = 0.7f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
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
}

/** The picture behind a pager page; `null` is the settings page after the last screen. */
private fun wallpaperSlotOf(screen: AppScreen?): WallpaperSlot = when (screen) {
    AppScreen.CHRONOMETER -> WallpaperSlot.CHRONO
    AppScreen.REST_TIMER -> WallpaperSlot.REST
    AppScreen.COUNTER -> WallpaperSlot.COUNTER
    AppScreen.REST_AND_COUNTER -> WallpaperSlot.REST_AND_COUNTER
    AppScreen.WORKOUTS -> WallpaperSlot.WORKOUTS
    null -> WallpaperSlot.SETTINGS
}
