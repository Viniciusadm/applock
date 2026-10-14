package com.archieapps.applock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.archieapps.applock.domain.LockController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        AppLockSession.reset()

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            try {
                LockController(appContext).rescheduleTempLockIfActive()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
