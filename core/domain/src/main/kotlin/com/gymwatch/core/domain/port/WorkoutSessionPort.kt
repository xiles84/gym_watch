package com.gymwatch.core.domain.port

import com.gymwatch.core.domain.model.ExerciseKind
import com.gymwatch.core.domain.model.SessionOwnership
import com.gymwatch.core.domain.model.WorkoutSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * The health backend, seen from the domain.
 *
 * Currently implemented over Health Services' ExerciseClient. If that ever has
 * to change — e.g. because Samsung Health won't ingest our records — this
 * interface is the only thing the rest of the app is coupled to.
 */
interface WorkoutSessionPort {

    /** Emits null when no session is in progress. */
    val snapshots: Flow<WorkoutSnapshot?>

    /**
     * What this specific watch can actually track. Capabilities vary by device,
     * so we ask instead of assuming — the favourites editor only offers these.
     */
    suspend fun supportedKinds(): Set<ExerciseKind>

    /** Must be checked before [start]; see [SessionOwnership.OTHER_APP]. */
    suspend fun ownership(): SessionOwnership

    /** Ends any session owned by another app. Confirm with the user first. */
    suspend fun start(kind: ExerciseKind)

    suspend fun pause()
    suspend fun resume()
    suspend fun end()
}
