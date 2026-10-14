package com.archieapps.applock.automation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.archieapps.applock.domain.LockController
import com.archieapps.applock.domain.LockerAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TempLockExpiryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            try {
                LockController(appContext).dispatch(LockerAction.RestoreNormal)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
