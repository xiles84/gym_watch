package com.gymwatch.adapters.driving.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Text
import com.gymwatch.core.domain.model.ExerciseKind

/**
 * Chooses the workout for one shortcut, from every type Samsung Health offers.
 *
 * About a hundred rows, so it opens centred on the current choice rather than
 * at the top — changing weight machine to treadmill should not start with a
 * scroll past sixty sports. Everything is a tap; there is no text entry, because
 * typing on a watch with chalk on your hands does not work.
 *
 * [ScalingLazyColumn] rather than a scrolling Column: on a round screen a plain
 * Column loses its first and last rows to the curve (docs/LESSONS.md #25).
 */
@Composable
fun WorkoutPicker(
    selected: ExerciseKind,
    icons: WorkoutIcons,
    onPick: (ExerciseKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    // +1 for the title row above the first kind.
    val listState = rememberScalingLazyListState(initialCenterItemIndex = selected.ordinal + 1)

    ScalingLazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Text("WORKOUT", color = GymColors.Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
        }

        ExerciseKind.entries.forEach { kind ->
            item {
                val isSelected = kind == selected
                val content = if (isSelected) GymColors.Background else GymColors.OnSurface

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) GymColors.Rest else GymColors.Surface,
                            RoundedCornerShape(percent = 50),
                        )
                        .clickable { onPick(kind) }
                        .padding(vertical = 7.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ExerciseIcon(kind = kind, icons = icons, tint = content, size = 20.dp)
                    Text(
                        text = kind.displayName,
                        color = content,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
