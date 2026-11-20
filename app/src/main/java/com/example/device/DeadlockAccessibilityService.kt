package com.example.device

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.DeadlockApp
import com.example.LockBlockerActivity
import com.example.data.local.PersistentVaultManager

class DeadlockAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "DeadlockService"
        private val SETTINGS_PACKAGES = setOf(
            "com.android.settings",
            "com.google.android.packageinstaller",
            "com.android.packageinstaller"
        )

        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            val expectedComponentName = ComponentName(context, DeadlockAccessibilityService::class.java).flattenToString()
            val enabledServicesSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServicesSetting)

            while (colonSplitter.hasNext()) {
                val componentNameString = colonSplitter.next()
                if (componentNameString.equals(expectedComponentName, ignoreCase = true)) {
                    return true
                }
            }
            return false
        }

        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    private var vaultManager: PersistentVaultManager? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        vaultManager = PersistentVaultManager(this)
        Log.i(TAG, "DeadlockAccessibilityService connected and ready.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return

        val pkgName = event.packageName?.toString() ?: return

        // 1. Ignore self app launches
        if (pkgName == packageName) return

        // 2. Anti-tampering check: Prevent clearing data / cache / uninstalling / deactivating Deadlock in Settings
        if (SETTINGS_PACKAGES.contains(pkgName)) {
            val hasActiveLocks = getActiveLockedPackages().isNotEmpty()
            if (hasActiveLocks && isTamperingAttempt(rootInActiveWindow)) {
                Log.w(TAG, "TAMPER ATTEMPT DETECTED IN SETTINGS. Intercepting!")
                performGlobalAction(GLOBAL_ACTION_HOME)

                val blockerIntent = Intent(this, LockBlockerActivity::class.java).apply {
                    putExtra(LockBlockerActivity.EXTRA_PACKAGE_NAME, packageName)
                    putExtra(LockBlockerActivity.EXTRA_APP_LABEL, "Deadlock System Protection")
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    )
                }
                startActivity(blockerIntent)
                return
            }
        }

        // 3. Locked App Launch Interception
        val activeSet = getActiveLockedPackages()
        if (activeSet.contains(pkgName)) {
            Log.w(TAG, "LOCKED APP LAUNCH ATTEMPT DETECTED: $pkgName. Intercepting immediately!")

            performGlobalAction(GLOBAL_ACTION_HOME)

            val blockerIntent = Intent(this, LockBlockerActivity::class.java).apply {
                putExtra(LockBlockerActivity.EXTRA_PACKAGE_NAME, pkgName)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
            }
            startActivity(blockerIntent)
        }
    }

    private fun getActiveLockedPackages(): Set<String> {
        val memorySet = DeadlockApp.instance.activeLockedPackages.value
        if (memorySet.isNotEmpty()) return memorySet

        // Fallback to direct persistent vault
        val vm = vaultManager ?: PersistentVaultManager(this).also { vaultManager = it }
        val recovered = vm.recoverPolicies()
        return recovered
            .filter { !it.isExpired() && it.status.name == "ACTIVE" }
            .map { it.packageName }
            .toSet()
    }

    private fun isTamperingAttempt(rootNode: AccessibilityNodeInfo?): Boolean {
        if (rootNode == null) return false
        return try {
            inspectNodeForDeadlock(rootNode)
        } catch (e: Exception) {
            false
        }
    }

    private fun inspectNodeForDeadlock(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val viewId = node.viewIdResourceName?.lowercase() ?: ""

        val isTargetApp = text.contains("deadlock") || desc.contains("deadlock") || text.contains(packageName)

        val isDangerousAction = text.contains("clear storage") || text.contains("clear data") ||
                text.contains("clear cache") || text.contains("force stop") ||
                text.contains("uninstall") || text.contains("deactivate")

        if (isTargetApp && isDangerousAction) return true

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (inspectNodeForDeadlock(child)) {
                return true
            }
        }
        return false
    }

    override fun onInterrupt() {
        Log.w(TAG, "DeadlockAccessibilityService interrupted.")
    }
}

