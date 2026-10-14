package com.archieapps.applock.service

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expected = ComponentName(context, AppMonitorService::class.java).flattenToString()
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false

    val splitter = TextUtils.SimpleStringSplitter(':')
    splitter.setString(enabledServices)
    while (splitter.hasNext()) {
        val component = ComponentName.unflattenFromString(splitter.next())
        if (component != null && component == ComponentName.unflattenFromString(expected)) {
            return true
        }
    }
    return false
}
