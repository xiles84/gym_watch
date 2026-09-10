package com.gymwatch.adapters.driving.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Text
import com.gymwatch.core.application.ScreenLayoutUseCase
import com.gymwatch.core.domain.model.ScreenLayout

/**
 * Turns screens on and off and reorders them.
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("SCREENS", color = GymColors.Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(6.dp))

        layout.order.forEachIndexed { index, screen ->
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

        Spacer(Modifier.height(8.dp))
        Text(
            text = if (layout.visible.size <= 1) "one screen must stay on" else "tap ● to hide",
            color = GymColors.Dim,
            fontSize = 9.sp,
        )
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
            .padding(vertical = 2.dp)
            .background(
                if (visible) GymColors.Surface else GymColors.Background,
                RoundedCornerShape(percent = 50),
            )
            .padding(vertical = 3.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArrowButton(glyph = "▲", enabled = canMoveUp, onClick = onMoveUp)

        Text(
            text = title,
            color = if (visible) GymColors.OnSurface else GymColors.Dim,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
        )

        ArrowButton(glyph = "▼", enabled = canMoveDown, onClick = onMoveDown)

        Box(
            modifier = Modifier
                .size(30.dp)
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
