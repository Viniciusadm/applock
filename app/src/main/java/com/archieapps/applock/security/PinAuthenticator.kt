package com.archieapps.applock.security

import kotlinx.coroutines.delay

class PinAuthenticator(private val pinManager: PinManager) {

    private var failedAttempts = 0

    suspend fun verify(pin: String): Boolean {
        val backoffMs = (failedAttempts * BACKOFF_STEP_MS).coerceAtMost(MAX_BACKOFF_MS)
        if (backoffMs > 0) delay(backoffMs)
        return if (pinManager.verifyPin(pin)) {
            failedAttempts = 0
            true
        } else {
            failedAttempts++
            false
        }
    }

    fun reset() {
        failedAttempts = 0
    }

    private companion object {
        const val BACKOFF_STEP_MS = 500L
        const val MAX_BACKOFF_MS = 5_000L
    }
}
