package com.archieapps.applock.domain

import android.net.Uri

sealed interface LockerAction {
    data object Enable : LockerAction
    data object Disable : LockerAction
    data object Toggle : LockerAction
    data class LockTemporarily(val minutes: Int) : LockerAction
    data object RestoreNormal : LockerAction

    companion object {
        const val ACTION_ENABLE = "com.archieapps.applock.action.ENABLE"
        const val ACTION_DISABLE = "com.archieapps.applock.action.DISABLE"
        const val ACTION_TOGGLE = "com.archieapps.applock.action.TOGGLE"
        const val ACTION_LOCK = "com.archieapps.applock.action.LOCK"
        const val ACTION_RESTORE = "com.archieapps.applock.action.RESTORE"

        const val EXTRA_MINUTES = "minutes"

        const val LOCK_30_MINUTES = 30
        const val LOCK_60_MINUTES = 60

        fun fromUri(uri: Uri?): LockerAction? {
            if (uri == null) return null
            if (!uri.scheme.equals("applock", ignoreCase = true)) return null
            return when (uri.host?.lowercase() ?: uri.path?.trim('/')?.lowercase()) {
                "action" -> fromPath(uri.lastPathSegment?.lowercase(), uri.minutesParam())
                else -> fromPath(uri.host?.lowercase(), uri.minutesParam())
            }
        }

        fun fromExplicit(action: String?, minutes: Int): LockerAction? = when (action) {
            ACTION_ENABLE -> Enable
            ACTION_DISABLE -> Disable
            ACTION_TOGGLE -> Toggle
            ACTION_LOCK -> LockTemporarily(minutes.coerceAtLeast(1))
            ACTION_RESTORE -> RestoreNormal
            else -> null
        }

        private fun fromPath(segment: String?, minutes: Int?): LockerAction? = when (segment) {
            "enable" -> Enable
            "disable" -> Disable
            "toggle" -> Toggle
            "lock" -> LockTemporarily((minutes ?: LOCK_30_MINUTES).coerceAtLeast(1))
            "restore" -> RestoreNormal
            else -> null
        }

        private fun Uri.minutesParam(): Int? =
            getQueryParameter(EXTRA_MINUTES)?.toIntOrNull()
    }
}
