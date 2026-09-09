package com.gymwatch.adapters.driving.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Text
import com.gymwatch.core.application.WorkoutUseCase
import com.gymwatch.core.domain.model.ExerciseKind

/**
 * Phase 3 renders the favourites and reports that starting is not wired yet.
 * Health Services lands in phase 4 behind [WorkoutUseCase]; this screen will
 * not need to change when it does.
 */
@Composable
fun WorkoutsScreen(
    useCase: WorkoutUseCase,
    onStart: (ExerciseKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    val favourites by useCase.favourites.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("START", color = GymColors.Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(6.dp))

        favourites.kinds.forEach { kind ->
            PillButton(
                label = "${kind.glyph}  ${kind.displayName}",
                onClick = { onStart(kind) },
                modifier = Modifier.padding(vertical = 3.dp),
            )
        }
    }
}
