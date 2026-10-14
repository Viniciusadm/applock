package com.archieapps.applock.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.protectionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "protection_prefs",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

class ProtectionSettingsRepository(private val context: Context) {

    val protectionEnabledFlow: Flow<Boolean> = context.protectionDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[PROTECTION_ENABLED_KEY] ?: false }

    suspend fun isProtectionEnabled(): Boolean = protectionEnabledFlow.first()

    suspend fun setProtectionEnabled(enabled: Boolean) {
        context.protectionDataStore.edit { preferences ->
            preferences[PROTECTION_ENABLED_KEY] = enabled
        }
    }

    private companion object {
        val PROTECTION_ENABLED_KEY = booleanPreferencesKey("protection_enabled")
    }
}
