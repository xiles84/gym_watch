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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.Text
import com.gymwatch.core.application.ScreenLayoutUseCase
import com.gymwatch.core.application.SkinUseCase
import com.gymwatch.core.domain.model.Skin

/**
 * Turns screens on and off, reorders them, and picks the colour scheme.
 *
 * This screen is never in the layout it edits — the pager appends it last
 * unconditionally. A settings screen that can hide itself is a trap with no way
 * out short of clearing app data.
 *
 * [ScalingLazyColumn] rather than a scrolling Column: on a round screen a plain
 * Column loses its first and last rows to the curve, and this list is now long
 * enough for that to bite (docs/LESSONS.md #25).
 */
@Composable
fun SettingsScreen(
    screenLayout: ScreenLayoutUseCase,
    skins: SkinUseCase,
    modifier: Modifier = Modifier,
) {
    val layout by screenLayout.state.collectAsStateWithLifecycle()
    val skin by skins.state.collectAsStateWithLifecycle()

    ScalingLazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 30.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        // The list is short and fixed; left on, auto-centering opens with half a
        // screen of black above the first row.
        autoCentering = null,
    ) {
        item { SectionLabel("SCREENS") }

        layout.order.forEachIndexed { index, screen ->
            item {
                ScreenRow(
                    title = screen.title,
                    visible = layout.isVisible(screen),
                    canMoveUp = index > 0,
                    canMoveDown = index < layout.order.lastIndex,
                    onToggle = { screenLayout.toggle(screen) },
                    onMoveUp = { screenLayout.move(screen, -1) },
                    onMoveDown = { screenLayout.move(screen, 1) },
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

        item { SectionLabel("SKIN") }

        Skin.entries.forEach { entry ->
            item {
                SkinRow(
                    skin = entry,
                    selected = entry == skin,
                    onClick = { skins.select(entry) },
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = GymColors.Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
}

/**
 * The two dots are the skin's own chrono and rest colours, not the current
 * theme's — the point of the row is to show what you are about to switch to.
 */
@Composable
private fun SkinRow(skin: Skin, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .background(
                if (selected) GymColors.Surface else GymColors.Background,
                RoundedCornerShape(percent = 50),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Swatch(Color(skin.palette.chrono))
        Swatch(Color(skin.palette.rest))

        Text(
            text = skin.title,
            color = if (selected) GymColors.OnSurface else GymColors.Muted,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
        )

        Text(
            text = if (selected) "●" else "○",
            color = if (selected) GymColors.Go else GymColors.Dim,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun Swatch(color: Color) {
    Box(Modifier.size(11.dp).background(color, CircleShape))
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
