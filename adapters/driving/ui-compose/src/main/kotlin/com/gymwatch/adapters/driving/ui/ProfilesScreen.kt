package com.gymwatch.adapters.driving.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Text
import com.gymwatch.core.application.ProfilesUseCase
import kotlinx.coroutines.launch

/**
 * The three profiles, and the way out to Samsung Health.
 *
 * This screen used to start a Health Services exercise. It no longer does, and
 * that is the point: Health Services streams live metrics but persists nothing,
 * Health Connect does not run on Wear OS, and Samsung Health has no third-party
 * write API — so a workout we tracked could never reach its history. Worse, the
 * platform allows one exercise device-wide, so starting ours *ended* whatever
 * Samsung Health was recording. See docs/LESSONS.md #2.
 *
 * So Samsung Health records the workout, and a profile configures the parts we
 * do own: rest lengths and what the counter counts.
 */
@Composable
fun ProfilesScreen(
    useCase: ProfilesUseCase,
    onEditProfile: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val profiles by useCase.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("PROFILE", color = GymColors.Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(6.dp))

        profiles.entries.forEachIndexed { index, profile ->
            ProfileRow(
                label = profile.kind.glyph + "  " + profile.kind.displayName,
                selected = index == profiles.selected,
                onClick = { useCase.select(index) },
                onLongClick = { onEditProfile(index) },
            )
        }

        Spacer(Modifier.height(6.dp))

        PillButton(
            label = "open Samsung Health",
            onClick = { scope.launch { useCase.openHealthApp() } },
            background = GymColors.Background,
            contentColor = GymColors.Muted,
            modifier = Modifier.border(1.dp, GymColors.Surface, RoundedCornerShape(percent = 50)),
        )

        Spacer(Modifier.height(4.dp))
        Text("tap to switch · hold to edit", color = GymColors.Dim, fontSize = 9.sp)
    }
}

@Composable
private fun ProfileRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(GymColors.Surface, RoundedCornerShape(percent = 50))
            .then(
                if (selected) {
                    Modifier.border(2.dp, GymColors.Rest, RoundedCornerShape(percent = 50))
                } else {
                    Modifier
                },
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 9.dp, horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = GymColors.OnSurface,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
    }
}
