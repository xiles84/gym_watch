package com.gymwatch.adapters.driven.persistence

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gymwatch.core.domain.model.CounterLabel
import com.gymwatch.core.domain.model.ExerciseKind
import com.gymwatch.core.domain.model.Profiles
import com.gymwatch.core.domain.model.RestPresets
import com.gymwatch.core.domain.model.WorkoutProfile
import com.gymwatch.core.domain.port.ProfilesRepositoryPort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration.Companion.seconds

/**
 * Profiles as three parallel lists rather than one serialised blob.
 *
 * Everything is decoded defensively — an enum constant removed in a later
 * version, a list of the wrong length, a nonsense index — because the failure
 * mode of a strict decode is an app that will not open at the gym. The domain's
 * `of` factories do the padding and clamping, so this file only has to parse.
 */
internal class DataStoreProfilesRepository(
    private val dataStore: DataStore<Preferences>,
) : ProfilesRepositoryPort {

    override val profiles: Flow<Profiles> = dataStore.data.map { prefs ->
        val kinds = prefs[KINDS]?.split(OUTER)?.mapNotNull(::decodeKind)
            ?: return@map migrated(prefs)

        val restSeconds = prefs[REST]?.split(OUTER).orEmpty()
        val labels = prefs[LABELS]?.split(OUTER).orEmpty()

        val entries = kinds.mapIndexed { index, kind ->
            WorkoutProfile(
                kind = kind,
                restPresets = decodePresets(restSeconds.getOrNull(index)),
                counterLabel = decodeLabel(labels.getOrNull(index)),
            )
        }
        Profiles.of(entries, prefs[SELECTED] ?: 0)
    }

    override suspend fun save(profiles: Profiles) {
        dataStore.edit { prefs ->
            prefs[KINDS] = profiles.entries.joinToString(OUTER) { it.kind.name }
            prefs[REST] = profiles.entries.joinToString(OUTER) { profile ->
                profile.restPresets.durations.joinToString(INNER) { it.inWholeSeconds.toString() }
            }
            prefs[LABELS] = profiles.entries.joinToString(OUTER) { it.counterLabel.name }
            prefs[SELECTED] = profiles.selected
        }
    }

    /**
     * First run after the upgrade: carry over what the old favourites and the
     * single rest length were, so an existing install keeps its three workouts
     * and the rest time it was set to.
     */
    private fun migrated(prefs: Preferences): Profiles {
        val legacyKinds = prefs[LEGACY_FAVOURITES]?.split(OUTER)?.mapNotNull(::decodeKind)
        val legacyRest = prefs[LEGACY_REST_SECONDS]?.seconds

        if (legacyKinds.isNullOrEmpty() && legacyRest == null) return Profiles.DEFAULT

        val kinds = legacyKinds ?: Profiles.DEFAULT.entries.map { it.kind }
        val entries = List(Profiles.COUNT) { index ->
            val default = Profiles.DEFAULT.entries[index]
            val presets = legacyRest
                // The old single length becomes the middle preset; the outer two
                // bracket it, so all three are useful immediately.
                ?.let { RestPresets.of(listOf(it / 2, it, it * 2)) }
                ?: default.restPresets
            WorkoutProfile(
                kind = kinds.getOrNull(index) ?: default.kind,
                restPresets = presets,
                counterLabel = default.counterLabel,
            )
        }
        return Profiles.of(entries, 0)
    }

    private fun decodePresets(encoded: String?): RestPresets {
        val durations = encoded?.split(INNER)?.mapNotNull { it.toLongOrNull()?.seconds }
        return if (durations.isNullOrEmpty()) RestPresets.DEFAULT else RestPresets.of(durations)
    }

    private fun decodeKind(name: String): ExerciseKind? =
        ExerciseKind.entries.firstOrNull { it.name == name }

    private fun decodeLabel(name: String?): CounterLabel =
        CounterLabel.entries.firstOrNull { it.name == name } ?: CounterLabel.SETS

    private companion object {
        val KINDS = stringPreferencesKey("profile_kinds")
        val REST = stringPreferencesKey("profile_rest_seconds")
        val LABELS = stringPreferencesKey("profile_counter_labels")
        val SELECTED = intPreferencesKey("profile_selected")

        /** Written by versions before profiles existed; read once, never written. */
        val LEGACY_FAVOURITES = stringPreferencesKey("favourite_kinds")
        val LEGACY_REST_SECONDS = longPreferencesKey("rest_seconds")

        const val OUTER = ","
        const val INNER = "|"
    }
}
