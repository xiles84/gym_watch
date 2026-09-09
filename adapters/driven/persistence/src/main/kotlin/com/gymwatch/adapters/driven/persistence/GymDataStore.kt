package com.gymwatch.adapters.driven.persistence

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * One DataStore for the whole app. DataStore throws if the same file is opened
 * twice in a process, so it is created once here as a Context extension and
 * every repository is handed the same instance.
 */
internal val Context.gymDataStore: DataStore<Preferences> by preferencesDataStore(name = "gym_watch")
