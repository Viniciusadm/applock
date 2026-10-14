package com.archieapps.applock.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import com.archieapps.applock.data.model.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InstalledAppsRepository(context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val ownPackageName: String = context.packageName

    suspend fun loadInstalledApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        packageManager.queryIntentActivities(launcherIntent, 0)
            .asSequence()
            .map { it.activityInfo.packageName }
            .filter { it != ownPackageName }
            .distinct()
            .mapNotNull { packageName ->
                runCatching {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    InstalledApp(
                        name = packageManager.getApplicationLabel(appInfo).toString(),
                        packageName = packageName,
                        icon = packageManager.getApplicationIcon(appInfo),
                        isBlocked = false,
                    )
                }.getOrNull()
            }
            .sortedBy { it.name.lowercase() }
            .toList()
    }

    fun criticalPackages(): Set<String> = buildSet {
        add(ownPackageName)

        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        packageManager.queryIntentActivities(homeIntent, 0).forEach { add(it.activityInfo.packageName) }

        runCatching {
            val settingsIntent = Intent(Settings.ACTION_SETTINGS)
            settingsIntent.resolveActivity(packageManager)?.packageName?.let { add(it) }
        }
    }

    suspend fun getApp(packageName: String): InstalledApp? = withContext(Dispatchers.IO) {
        runCatching {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            InstalledApp(
                name = packageManager.getApplicationLabel(appInfo).toString(),
                packageName = packageName,
                icon = packageManager.getApplicationIcon(appInfo),
                isBlocked = true,
            )
        }.getOrNull()
    }
}
