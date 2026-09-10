package com.gymwatch.adapters.driving.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.gymwatch.core.domain.model.ExerciseKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Where exercise icons come from.
 *
 * Declared here and implemented by the composition root, so this driving adapter
 * never depends on the driven one that reads Samsung Health's resources. A null
 * icon is normal — no health app installed, or a name that vanished in an
 * update — and [ExerciseIcon] falls back to the exercise's initial.
 */
fun interface WorkoutIcons {
    /** May decode a resource; only ever called off the main thread. */
    fun load(kind: ExerciseKind): Bitmap?
}

/**
 * The icon tinted to [tint]. Samsung Health's glyphs are white on transparent,
 * so tinting is what makes them follow the skin instead of looking pasted in.
 */
@Composable
internal fun ExerciseIcon(
    kind: ExerciseKind,
    icons: WorkoutIcons,
    tint: Color,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, icons, kind) {
        value = withContext(Dispatchers.IO) { icons.load(kind)?.asImageBitmap() }
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        val loaded = bitmap
        if (loaded != null) {
            Image(
                bitmap = loaded,
                contentDescription = kind.displayName,
                colorFilter = ColorFilter.tint(tint),
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = kind.displayName.take(1),
                color = tint,
                fontSize = (size.value * 0.55f).sp,
            )
        }
    }
}
