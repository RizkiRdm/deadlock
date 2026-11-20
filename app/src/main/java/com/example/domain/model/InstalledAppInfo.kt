package com.example.domain.model

import android.graphics.drawable.Drawable

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val isSystemApp: Boolean,
    val icon: Drawable? = null
)
