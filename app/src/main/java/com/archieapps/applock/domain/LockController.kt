package com.archieapps.applock.domain

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.archieapps.applock.automation.TempLockExpiryReceiver
import com.archieapps.applock.data.repository.LockStateRepository
import kotlinx.coroutines.flow.Flow

class LockController(
    private val context: Context,
    private val repository: LockStateRepository = LockStateRepository(context),
) {

    val lockingEnabledFlow: Flow<Boolean> = repository.lockingEnabledFlow
    val tempLockUntilFlow: Flow<Long> = repository.tempLockUntilFlow
    val requireAuthForActionsFlow: Flow<Boolean> = repository.requireAuthForActionsFlow

    suspend fun dispatch(action: LockerAction) {
        when (action) {
            LockerAction.Enable -> repository.setLockingEnabled(true)

            LockerAction.Disable -> {
                repository.setLockingEnabled(false)
                clearTempLock()
            }

            LockerAction.Toggle -> repository.setLockingEnabled(!repository.isLockingEnabled())

            is LockerAction.LockTemporarily -> {
                val until = System.currentTimeMillis() + action.minutes * 60_000L
                repository.setTempLockUntil(until)
                scheduleExpiry(until)
            }

            LockerAction.RestoreNormal -> clearTempLock()
        }
    }

    suspend fun setRequireAuthForActions(enabled: Boolean) {
        repository.setRequireAuthForActions(enabled)
    }

    suspend fun requiresAuth(action: LockerAction): Boolean = ActionAuthPolicy.requiresAuth(
        action = action,
        requireAuthForActions = repository.requiresAuthForActions(),
        lockingEnabled = repository.isLockingEnabled(),
    )

    suspend fun rescheduleTempLockIfActive() {
        val until = repository.tempLockUntil()
        if (until > System.currentTimeMillis()) scheduleExpiry(until)
    }

    private suspend fun clearTempLock() {
        repository.setTempLockUntil(0L)
        cancelExpiry()
    }

    private fun scheduleExpiry(triggerAtEpochMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val delay = (triggerAtEpochMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        alarmManager.set(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + delay,
            expiryPendingIntent(),
        )
    }

    private fun cancelExpiry() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(expiryPendingIntent())
    }

    private fun expiryPendingIntent(): PendingIntent {
        val intent = Intent(context, TempLockExpiryReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            EXPIRY_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val EXPIRY_REQUEST_CODE = 4021
    }
}
