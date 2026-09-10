package com.gymwatch.adapters.driven.persistence

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gymwatch.core.domain.model.ExerciseKind
import com.gymwatch.core.domain.model.RestPresets
import com.gymwatch.core.domain.model.WorkoutSetup
import com.gymwatch.core.domain.port.WorkoutSetupRepositoryPort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration.Companion.seconds

/**
 * The three shortcuts and the rest presets, as two short strings.
 *
 * Everything is decoded defensively — an exercise name from a later version, a
 * list of the wrong length — because the failure mode of a strict decode is an
 * app that will not open at the gym. The domain's `of` factories do the padding
 * and clamping, so this file only has to parse.
 */
internal class DataStoreWorkoutSetupRepository(
    private val dataStore: DataStore<Preferences>,
) : WorkoutSetupRepositoryPort {

    override val setup: Flow<WorkoutSetup> = dataStore.data.map { prefs ->
        val shortcuts = prefs[SHORTCUTS]?.split(SEPARATOR)?.mapNotNull(ExerciseKind::of)
            ?: return@map migrated(prefs)
        WorkoutSetup.of(shortcuts, decodePresets(prefs[REST_SECONDS], SEPARATOR))
    }

    override suspend fun save(setup: WorkoutSetup) {
        dataStore.edit { prefs ->
            prefs[SHORTCUTS] = setup.shortcuts.joinToString(SEPARATOR) { it.name }
            prefs[REST_SECONDS] = setup.restPresets.durations
                .joinToString(SEPARATOR) { it.inWholeSeconds.toString() }
        }
    }

    /**
     * First run after profiles were removed: the three profile workouts become
     * the three shortcuts, and the rest presets are the ones the *selected*
     * profile had — the lengths the user was actually resting for.
     *
     * The legacy keys are only read. They stay in the file, so an older build
     * reinstalled over this one still finds its profiles.
     */
    private fun migrated(prefs: Preferences): WorkoutSetup {
        val kinds = prefs[LEGACY_PROFILE_KINDS]?.split(SEPARATOR)?.mapNotNull(ExerciseKind::of)
            ?: return WorkoutSetup.DEFAULT
        val selected = prefs[LEGACY_PROFILE_SELECTED] ?: 0
        val selectedRest = prefs[LEGACY_PROFILE_REST]?.split(SEPARATOR)?.getOrNull(selected)
        return WorkoutSetup.of(kinds, decodePresets(selectedRest, LEGACY_INNER))
    }

    private fun decodePresets(encoded: String?, separator: String): RestPresets {
        val durations = encoded?.split(separator)?.mapNotNull { it.toLongOrNull()?.seconds }
        return if (durations.isNullOrEmpty()) RestPresets.DEFAULT else RestPresets.of(durations)
    }

    private companion object {
        val SHORTCUTS = stringPreferencesKey("workout_shortcuts")
        val REST_SECONDS = stringPreferencesKey("rest_preset_seconds")
        const val SEPARATOR = ","

        /** Written by the profiles version; read once, never written. */
        val LEGACY_PROFILE_KINDS = stringPreferencesKey("profile_kinds")
        val LEGACY_PROFILE_REST = stringPreferencesKey("profile_rest_seconds")
        val LEGACY_PROFILE_SELECTED = intPreferencesKey("profile_selected")

        /** Profiles were comma-separated; each one's durations were pipe-separated. */
        const val LEGACY_INNER = "|"
    }
}
