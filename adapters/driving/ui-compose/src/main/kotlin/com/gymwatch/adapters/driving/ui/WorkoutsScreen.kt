package com.gymwatch.adapters.driving.ui

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Text
import com.gymwatch.core.application.WorkoutSetupUseCase
import com.gymwatch.core.domain.model.ExerciseKind
import kotlinx.coroutines.launch

/**
 * Three workout shortcuts. A tap opens that exercise's start screen in Samsung
 * Health; holding one changes it.
 *
 * Samsung Health records the workout, because nothing else can
 * (docs/LESSONS.md #2). This screen used to switch between profiles and could
 * only reach Samsung Health's home screen; the route straight into an exercise
 * is the intent Samsung Health's own watch-face complication uses (#28).
 *
 * The icons are Samsung Health's, read from it at runtime and tinted to the
 * skin, so the circle you tap looks like the screen you land on.
 */
@Composable
fun WorkoutsScreen(
    useCase: WorkoutSetupUseCase,
    icons: WorkoutIcons,
    onChangeShortcut: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val setup by useCase.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("WORKOUTS", color = GymColors.Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            setup.shortcuts.forEachIndexed { index, kind ->
                Shortcut(
                    kind = kind,
                    icons = icons,
                    onClick = { scope.launch { useCase.startWorkout(index) } },
                    onLongClick = { onChangeShortcut(index) },
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("tap to start · hold to change", color = GymColors.Dim, fontSize = 9.sp)
    }
}

/**
 * 54dp, the same target size as the rest presets, and long-pressable so a
 * shortcut is never changed by accident mid-set.
 */
@Composable
private fun Shortcut(
    kind: ExerciseKind,
    icons: WorkoutIcons,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        modifier = Modifier.width(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(GymColors.Surface, CircleShape)
                .border(1.dp, GymColors.Outline, CircleShape)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
            contentAlignment = Alignment.Center,
        ) {
            ExerciseIcon(kind = kind, icons = icons, tint = GymColors.OnSurface, size = 30.dp)
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = kind.displayName,
            color = GymColors.OnSurface,
            fontSize = 10.sp,
            lineHeight = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
