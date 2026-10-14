package com.archieapps.applock.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.archieapps.applock.LockActivity
import com.archieapps.applock.data.repository.BlockedAppsRepository
import com.archieapps.applock.data.repository.LockStateRepository
import com.archieapps.applock.data.repository.isProtectionActive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.ConcurrentHashMap

class AppMonitorService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var blockedPackages: Set<String> = emptySet()

    @Volatile
    private var lockingEnabled: Boolean = true

    @Volatile
    private var tempLockUntil: Long = 0L

    private var foregroundPackage: String? = null

    private val launchableCache = ConcurrentHashMap<String, Boolean>()

    private val ownPackageName: String by lazy { applicationContext.packageName }

    override fun onServiceConnected() {
        super.onServiceConnected()
        BlockedAppsRepository(applicationContext).blockedPackages
            .onEach { blockedPackages = it }
            .launchIn(scope)

        val lockStateRepository = LockStateRepository(applicationContext)
        lockStateRepository.lockingEnabledFlow
            .onEach { lockingEnabled = it }
            .launchIn(scope)
        lockStateRepository.tempLockUntilFlow
            .onEach { tempLockUntil = it }
            .launchIn(scope)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        handleForeground(packageName)
    }

    private fun handleForeground(packageName: String) {
        if (packageName == ownPackageName) return
        if (!isProtectionActive(lockingEnabled, tempLockUntil, System.currentTimeMillis())) return
        if (!isLaunchableApp(packageName)) return
        if (packageName == foregroundPackage) return

        foregroundPackage = packageName
        AppLockSession.onForeground(packageName)

        val shouldLock = packageName in blockedPackages &&
            !AppLockSession.isUnlocked(packageName) &&
            AppLockSession.lockShowingFor != packageName

        if (shouldLock) {
            launchLockScreen(packageName)
        }
    }

    private fun isLaunchableApp(packageName: String): Boolean {
        if (packageName in blockedPackages) return true
        return launchableCache.getOrPut(packageName) {
            packageManager.getLaunchIntentForPackage(packageName) != null
        }
    }

    private fun launchLockScreen(packageName: String) {
        AppLockSession.onLockShowing(packageName)
        val intent = Intent(this, LockActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION,
            )
            putExtra(LockActivity.EXTRA_PACKAGE, packageName)
        }
        startActivity(intent)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
