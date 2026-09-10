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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymwatch.core.application.ChronometerUseCase
import com.gymwatch.core.application.CounterUseCase
import com.gymwatch.core.application.ProfilesUseCase
import com.gymwatch.core.application.RestTimerUseCase
import com.gymwatch.core.application.ScreenLayoutUseCase
import com.gymwatch.core.domain.model.AppScreen

/** Which editor, if any, is covering the pager. */
private sealed interface Editor {
    data class Preset(val index: Int) : Editor
    data class Profile(val index: Int) : Editor
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
    profiles: ProfilesUseCase,
    screenLayout: ScreenLayoutUseCase,
    requestedScreen: AppScreen? = null,
    onScreenHandled: () -> Unit = {},
) {
    val layout by screenLayout.state.collectAsStateWithLifecycle()
    val profilesState by profiles.state.collectAsStateWithLifecycle()

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

    GymTheme {
        Box(Modifier.fillMaxSize().background(GymColors.Background)) {
            when (val editor = editing) {
                is Editor.Preset -> RestPresetEditor(
                    title = profilesState.current.kind.displayName,
                    initial = profilesState.current.restPresets[editor.index],
                    onConfirm = { duration ->
                        profiles.setRestPreset(
                            profileIndex = profilesState.selected,
                            presetIndex = editor.index,
                            duration = duration,
                        )
                        editing = null
                    },
                )

                is Editor.Profile -> ProfileEditor(
                    initial = profilesState.entries[editor.index],
                    onConfirm = { profile ->
                        profiles.setProfileAt(editor.index, profile)
                        editing = null
                    },
                )

                null -> {
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                        when (visible.getOrNull(page)) {
                            AppScreen.CHRONOMETER -> ChronometerScreen(chronometer)

                            AppScreen.REST_TIMER -> RestTimerScreen(
                                useCase = restTimer,
                                onEditPreset = { editing = Editor.Preset(it) },
                            )

                            AppScreen.COUNTER -> CounterScreen(
                                useCase = counter,
                                label = profilesState.current.counterLabel.name,
                            )

                            AppScreen.PROFILES -> ProfilesScreen(
                                useCase = profiles,
                                onEditProfile = { editing = Editor.Profile(it) },
                            )

                            // Past the last visible screen: the settings page.
                            null -> SettingsScreen(screenLayout)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        repeat(pageCount) { index ->
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
