package com.gymwatch.adapters.driving.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.Text
import com.gymwatch.core.application.ScreenLayoutUseCase

/**
 * Turns screens on and off and reorders them.
 *
 * A [ScalingLazyColumn], not a plain scrolling Column: on a round display the
 * corners cut off anything full-width near the top and bottom edges. This is
 * the component that knows that — it centres the list, shrinks the items
 * approaching the rim, and pads for the curve, so every row is reachable and
 * readable. A hand-padded Column only ever approximates it.
 *
 * This screen is never in the layout it edits — the pager appends it last
 * unconditionally. A settings screen that can hide itself is a trap with no way
 * out short of clearing app data.
 */
@Composable
fun SettingsScreen(
    useCase: ScreenLayoutUseCase,
    modifier: Modifier = Modifier,
) {
    val layout by useCase.state.collectAsStateWithLifecycle()

    ScalingLazyColumn(
        modifier = modifier.fillMaxSize(),
        // Bottom padding clears the pager's page-indicator dots, which float
        // over this screen.
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 22.dp, bottom = 44.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        // autoCentering would centre the first item, leaving half a screen of
        // black above a list this short. The rim scaling still applies, which
        // is the part that matters on a round display.
        autoCentering = null,
    ) {
        item {
            Text("SCREENS", color = GymColors.Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
        }

        layout.order.forEachIndexed { index, screen ->
            item {
                ScreenRow(
                    title = screen.title,
                    visible = layout.isVisible(screen),
                    canMoveUp = index > 0,
                    canMoveDown = index < layout.order.lastIndex,
                    onToggle = { useCase.toggle(screen) },
                    onMoveUp = { useCase.move(screen, -1) },
                    onMoveDown = { useCase.move(screen, 1) },
                )
            }
        }

        item {
            Text(
                text = if (layout.visible.size <= 1) "one screen must stay on" else "tap ● to hide",
                color = GymColors.Dim,
                fontSize = 9.sp,
            )
        }
    }
}

@Composable
private fun ScreenRow(
    title: String,
    visible: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (visible) GymColors.Surface else GymColors.Background,
                RoundedCornerShape(percent = 50),
            )
            .padding(vertical = 3.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArrowButton(glyph = "▲", enabled = canMoveUp, onClick = onMoveUp)

        Text(
            text = title,
            color = if (visible) GymColors.OnSurface else GymColors.Dim,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
        )

        ArrowButton(glyph = "▼", enabled = canMoveDown, onClick = onMoveDown)

        Box(
            modifier = Modifier
                .size(28.dp)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (visible) "●" else "○",
                color = if (visible) GymColors.Go else GymColors.Dim,
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
private fun ArrowButton(glyph: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(26.dp)
            // Kept clickable when disabled would be a lie; a no-op tap on a dim
            // arrow at the end of the list is what the domain does anyway.
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            color = if (enabled) GymColors.Muted else GymColors.Dim,
            fontSize = 11.sp,
        )
    }
}
