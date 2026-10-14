package com.archieapps.applock.security

import android.os.SystemClock

object SettingsAuthSession {

    const val DEFAULT_TOLERANCE_MS = 30_000L

    @Volatile
    var toleranceMillis: Long = DEFAULT_TOLERANCE_MS

    @Volatile
    private var authenticated = false

    @Volatile
    private var lastValidElapsed = 0L

    fun markAuthenticated() {
        authenticated = true
        lastValidElapsed = SystemClock.elapsedRealtime()
    }

    fun onEnterBackground() {
        lastValidElapsed = SystemClock.elapsedRealtime()
    }

    fun isAuthenticated(): Boolean =
        authenticated && (SystemClock.elapsedRealtime() - lastValidElapsed) <= toleranceMillis

    fun invalidate() {
        authenticated = false
        lastValidElapsed = 0L
    }
}
