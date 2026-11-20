package com.example.domain.usecase

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.domain.model.InstalledAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetInstalledAppsUseCase(private val context: Context) {

    suspend fun execute(query: String = ""): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val selfPackage = context.packageName

        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        val packageMap = mutableMapOf<String, InstalledAppInfo>()

        for (resolveInfo in resolveInfos) {
            val pkgName = resolveInfo.activityInfo.packageName
            if (pkgName == selfPackage) continue

            try {
                val appInfo = pm.getApplicationInfo(pkgName, 0)
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val label = resolveInfo.loadLabel(pm).toString()
                val icon = resolveInfo.loadIcon(pm)

                packageMap[pkgName] = InstalledAppInfo(
                    packageName = pkgName,
                    label = label,
                    isSystemApp = isSystem,
                    icon = icon
                )
            } catch (e: Exception) {
                // Ignore missing package info
            }
        }

        val apps = packageMap.values.toList().sortedBy { it.label.lowercase() }

        if (query.isBlank()) {
            apps
        } else {
            val cleanQuery = query.trim().lowercase()
            apps.filter {
                it.label.lowercase().contains(cleanQuery) || it.packageName.lowercase().contains(cleanQuery)
            }
        }
    }
}
