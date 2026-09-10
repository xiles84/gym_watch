package com.gymwatch.adapters.driving.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.Text

/**
 * Asks before throwing away a clock that is still counting.
 *
 * Wear's own [AlertDialog] for the round-screen layout and swipe-to-dismiss,
 * with this app's round buttons inside so it reads as part of the app. Whether
 * to ask at all is decided by the domain model (`needsResetConfirmation`); this
 * only renders the question.
 */
@Composable
internal fun ConfirmResetDialog(
    visible: Boolean,
    title: String,
    detail: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        title = { Text(title, color = GymColors.OnSurface, textAlign = TextAlign.Center) },
        text = { Text(detail, color = GymColors.Muted, textAlign = TextAlign.Center) },
        confirmButton = {
            RoundButton(
                label = "✓",
                onClick = onConfirm,
                background = GymColors.Go,
                contentColor = GymColors.Background,
            )
        },
        dismissButton = { RoundButton(label = "✕", onClick = onDismiss) },
    )
}
