package com.gymwatch.adapters.driving.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Text
import com.gymwatch.core.application.CounterUseCase

/**
 * @param label what is being counted — supplied by the active profile, so
 *   switching from weights to a run relabels this without touching the count.
 */
@Composable
fun CounterScreen(
    useCase: CounterUseCase,
    label: String,
    modifier: Modifier = Modifier,
) {
    val counter by useCase.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            // Kept as a bonus, not a requirement: this watch has no rotating
            // bezel, so the buttons below are the real interface (LESSONS.md #24).
            .rotaryStepper { direction -> useCase.stepBy(direction) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(label, color = GymColors.Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)

        Spacer(Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RoundButton(label = "−", onClick = useCase::decrement, fontSize = 22)
            Text(
                text = counter.value.toString(),
                color = GymColors.OnSurface,
                fontSize = 46.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    // Long-press to reset: a plain tap here would be far too
                    // easy to trigger by accident mid-set.
                    .combinedClickable(onClick = {}, onLongClick = useCase::reset),
            )
            RoundButton(label = "+", onClick = useCase::increment, fontSize = 22)
        }

        Spacer(Modifier.height(8.dp))
        Text("hold number to reset", color = GymColors.Dim, fontSize = 9.sp)
    }
}
