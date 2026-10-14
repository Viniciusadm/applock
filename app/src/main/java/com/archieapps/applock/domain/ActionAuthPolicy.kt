package com.archieapps.applock.domain

object ActionAuthPolicy {

    fun isWeakening(action: LockerAction, lockingEnabled: Boolean): Boolean = when (action) {
        LockerAction.Disable -> true
        LockerAction.RestoreNormal -> true
        LockerAction.Toggle -> lockingEnabled
        LockerAction.Enable -> false
        is LockerAction.LockTemporarily -> false
    }

    fun requiresAuth(
        action: LockerAction,
        requireAuthForActions: Boolean,
        lockingEnabled: Boolean,
    ): Boolean = requireAuthForActions && isWeakening(action, lockingEnabled)
}
