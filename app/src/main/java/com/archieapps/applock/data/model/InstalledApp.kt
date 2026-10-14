package com.archieapps.applock.data.model

import android.graphics.drawable.Drawable

data class InstalledApp(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    val isBlocked: Boolean,
)
