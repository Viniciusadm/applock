package com.archieapps.applock.service

object AppLockSession {

    @Volatile
    var unlockedPackage: String? = null
        private set

    @Volatile
    var lockShowingFor: String? = null
        private set

    fun unlock(packageName: String) {
        unlockedPackage = packageName
        lockShowingFor = null
    }

    fun onForeground(packageName: String) {
        if (packageName != unlockedPackage) {
            unlockedPackage = null
        }
    }

    fun isUnlocked(packageName: String): Boolean = packageName == unlockedPackage

    fun onLockShowing(packageName: String) {
        lockShowingFor = packageName
    }

    fun onLockDismissed(packageName: String) {
        if (lockShowingFor == packageName) {
            lockShowingFor = null
        }
    }

    fun reset() {
        unlockedPackage = null
        lockShowingFor = null
    }
}
