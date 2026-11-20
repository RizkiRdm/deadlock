package com.example.device

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.UserManager
import android.util.Log

class DeviceManagerHelper(private val context: Context) {

    private val dpm: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    val adminComponent = ComponentName(context, DeadlockAdminReceiver::class.java)

    fun isDeviceAdmin(): Boolean {
        return dpm.isAdminActive(adminComponent)
    }

    fun isDeviceOwner(): Boolean {
        return dpm.isDeviceOwnerApp(context.packageName)
    }

    fun applySystemHardening() {
        if (!isDeviceOwner()) return
        try {
            // 1. Protect Deadlock from being uninstalled
            dpm.setUninstallBlocked(adminComponent, context.packageName, true)

            // 2. Prevent user from clearing app data, clearing cache, or force-stopping apps in Settings
            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_APPS_CONTROL)

            // 3. Prevent uninstallation of applications across the system
            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_UNINSTALL_APPS)

            // 4. Prevent changing system clock to bypass expiration
            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_DATE_TIME)

            // 5. Prevent factory reset from settings
            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET)

            // 6. Prevent booting into safe mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_SAFE_BOOT)
            }

            // 7. Prevent creating guest/secondary users to bypass
            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_ADD_USER)

            Log.d("DeadlockDeviceManager", "System hardening restrictions applied successfully.")
        } catch (e: Exception) {
            Log.e("DeadlockDeviceManager", "Failed to apply system hardening", e)
        }
    }

    fun enforcePolicyOnPackage(packageName: String): Boolean {
        if (!isDeviceOwner()) {
            Log.w("DeadlockDeviceManager", "Cannot enforce DPM policies: Not a Device Owner.")
            return false
        }

        var success = true
        applySystemHardening()

        try {
            // 1. Block uninstallation of the locked app
            dpm.setUninstallBlocked(adminComponent, packageName, true)
            Log.d("DeadlockDeviceManager", "setUninstallBlocked applied for $packageName")
        } catch (e: Exception) {
            Log.e("DeadlockDeviceManager", "Failed to block uninstall for $packageName", e)
            success = false
        }

        try {
            // 2. Hide the application from launcher & prevent launch
            dpm.setApplicationHidden(adminComponent, packageName, true)
            Log.d("DeadlockDeviceManager", "setApplicationHidden applied for $packageName")
        } catch (e: Exception) {
            Log.e("DeadlockDeviceManager", "Failed to hide application $packageName", e)
            success = false
        }

        try {
            // 3. Suspend package as secondary enforcement
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                dpm.setPackagesSuspended(adminComponent, arrayOf(packageName), true)
                Log.d("DeadlockDeviceManager", "setPackagesSuspended applied for $packageName")
            }
        } catch (e: Exception) {
            Log.e("DeadlockDeviceManager", "Failed to suspend package $packageName", e)
        }

        return success
    }

    fun releasePolicyOnPackage(packageName: String): Boolean {
        if (!isDeviceOwner()) return false
        return try {
            dpm.setApplicationHidden(adminComponent, packageName, false)
            dpm.setUninstallBlocked(adminComponent, packageName, false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                dpm.setPackagesSuspended(adminComponent, arrayOf(packageName), false)
            }
            true
        } catch (e: Exception) {
            Log.e("DeadlockDeviceManager", "Failed to release package $packageName", e)
            false
        }
    }

    fun secureDateTimeSettings(enable: Boolean) {
        if (!isDeviceOwner()) return
        try {
            if (enable) {
                dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_DATE_TIME)
            } else {
                dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_DATE_TIME)
            }
        } catch (e: Exception) {
            Log.e("DeadlockDeviceManager", "Failed to set DISALLOW_CONFIG_DATE_TIME restriction", e)
        }
    }
}
