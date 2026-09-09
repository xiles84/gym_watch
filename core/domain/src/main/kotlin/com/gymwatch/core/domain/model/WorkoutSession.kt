package com.gymwatch.core.domain.model

import kotlin.time.Duration

enum class WorkoutState { PREPARING, ACTIVE, PAUSED, ENDED }

/**
 * Why a session finished. [AUTO_ENDED_PERMISSION_LOST] and [TAKEN_OVER] are not
 * hypothetical: the platform allows exactly one exercise at a time device-wide,
 * and it will end ours without asking. The UI has to be able to say why.
 */
enum class WorkoutEndReason { USER_ENDED, TAKEN_OVER, AUTO_ENDED_PERMISSION_LOST, UNKNOWN }

/** Immutable read of an in-progress workout. Nulls mean "not available yet". */
data class WorkoutSnapshot(
    val kind: ExerciseKind,
    val state: WorkoutState,
    val activeDuration: Duration,
    val heartRateBpm: Int? = null,
    val calories: Int? = null,
    val endReason: WorkoutEndReason? = null,
)

/**
 * Who currently owns the device's single exercise slot.
 *
 * [OTHER_APP] is the Samsung Health case. Starting ours will end theirs, so the
 * UI must confirm rather than do it silently.
 */
enum class SessionOwnership { NONE, OURS, OTHER_APP }
