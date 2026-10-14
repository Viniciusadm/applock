package com.archieapps.applock.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.lockStateDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "lock_state_prefs",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

class LockStateRepository(private val context: Context) {

    val lockingEnabledFlow: Flow<Boolean> = context.lockStateDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[LOCKING_ENABLED_KEY] ?: true }

    val tempLockUntilFlow: Flow<Long> = context.lockStateDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[TEMP_LOCK_UNTIL_KEY] ?: 0L }

    val requireAuthForActionsFlow: Flow<Boolean> = context.lockStateDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[REQUIRE_AUTH_KEY] ?: false }

    suspend fun isLockingEnabled(): Boolean = lockingEnabledFlow.first()

    suspend fun tempLockUntil(): Long = tempLockUntilFlow.first()

    suspend fun requiresAuthForActions(): Boolean = requireAuthForActionsFlow.first()

    suspend fun setLockingEnabled(enabled: Boolean) {
        context.lockStateDataStore.edit { it[LOCKING_ENABLED_KEY] = enabled }
    }

    suspend fun setTempLockUntil(epochMillis: Long) {
        context.lockStateDataStore.edit { it[TEMP_LOCK_UNTIL_KEY] = epochMillis }
    }

    suspend fun setRequireAuthForActions(enabled: Boolean) {
        context.lockStateDataStore.edit { it[REQUIRE_AUTH_KEY] = enabled }
    }

    private companion object {
        val LOCKING_ENABLED_KEY = booleanPreferencesKey("locking_enabled")
        val TEMP_LOCK_UNTIL_KEY = longPreferencesKey("temp_lock_until")
        val REQUIRE_AUTH_KEY = booleanPreferencesKey("require_auth_for_actions")
    }
}

fun isProtectionActive(lockingEnabled: Boolean, tempLockUntil: Long, now: Long): Boolean =
    tempLockUntil > now || lockingEnabled
