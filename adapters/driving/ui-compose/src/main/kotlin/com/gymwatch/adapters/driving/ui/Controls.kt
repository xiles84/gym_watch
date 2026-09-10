package com.gymwatch.adapters.driving.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text

/**
 * Hand-rolled circular button.
 *
 * Sized for a gloved, sweaty thumb rather than for density: 48dp is the smallest
 * target that is reliably hittable without looking at the watch.
 *
 * The outline only shows over a wallpaper ([GymColors.Outline]), where a dark
 * button on dimmed art would otherwise lose its edge.
 */
@Composable
internal fun RoundButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Int = 48,
    background: Color = GymColors.Surface,
    contentColor: Color = GymColors.OnSurface,
    fontSize: Int = 18,
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(background, CircleShape)
            .border(1.dp, GymColors.Outline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = contentColor,
            fontSize = fontSize.sp,
            textAlign = TextAlign.Center,
        )
    }
}
