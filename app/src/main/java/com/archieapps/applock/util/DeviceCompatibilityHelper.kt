package com.archieapps.applock.util

import android.os.Build
import androidx.annotation.StringRes
import com.archieapps.applock.R

enum class DeviceManufacturer {
    XIAOMI,
    HUAWEI,
    SAMSUNG,
    OPPO,
    REALME,
    VIVO,
    ONEPLUS,
    OTHER,
}

object DeviceCompatibilityHelper {

    fun current(): DeviceManufacturer {
        val id = (Build.MANUFACTURER + " " + Build.BRAND).lowercase()
        return when {
            id.contains("xiaomi") || id.contains("redmi") || id.contains("poco") -> DeviceManufacturer.XIAOMI
            id.contains("huawei") || id.contains("honor") -> DeviceManufacturer.HUAWEI
            id.contains("samsung") -> DeviceManufacturer.SAMSUNG
            id.contains("oppo") -> DeviceManufacturer.OPPO
            id.contains("realme") -> DeviceManufacturer.REALME
            id.contains("vivo") -> DeviceManufacturer.VIVO
            id.contains("oneplus") -> DeviceManufacturer.ONEPLUS
            else -> DeviceManufacturer.OTHER
        }
    }

    fun hasSpecificInstructions(): Boolean = current() != DeviceManufacturer.OTHER

    @StringRes
    fun instructionsRes(): Int = when (current()) {
        DeviceManufacturer.XIAOMI -> R.string.compat_xiaomi
        DeviceManufacturer.HUAWEI -> R.string.compat_huawei
        DeviceManufacturer.SAMSUNG -> R.string.compat_samsung
        DeviceManufacturer.OPPO -> R.string.compat_oppo
        DeviceManufacturer.REALME -> R.string.compat_realme
        DeviceManufacturer.VIVO -> R.string.compat_vivo
        DeviceManufacturer.ONEPLUS -> R.string.compat_oneplus
        DeviceManufacturer.OTHER -> R.string.compat_generic
    }
}
