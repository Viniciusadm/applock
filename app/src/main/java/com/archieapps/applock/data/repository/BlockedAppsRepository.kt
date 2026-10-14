package com.archieapps.applock.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_locker_prefs",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

class BlockedAppsRepository(private val context: Context) {

    val blockedPackages: Flow<Set<String>> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences -> preferences[BLOCKED_APPS_KEY] ?: emptySet() }

    suspend fun setBlocked(packageName: String, blocked: Boolean) {
        context.dataStore.edit { preferences ->
            val current = preferences[BLOCKED_APPS_KEY] ?: emptySet()
            preferences[BLOCKED_APPS_KEY] = if (blocked) {
                current + packageName
            } else {
                current - packageName
            }
        }
    }

    private companion object {
        val BLOCKED_APPS_KEY = stringSetPreferencesKey("blocked_apps")
    }
}
