package com.example.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.DeadlockDatabase
import com.example.domain.model.LockStatus
import com.example.security.PolicyIntegrityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PackageChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val deviceHelper = DeviceManagerHelper(context)
        val database = DeadlockDatabase.getInstance(context)
        val integrityManager = PolicyIntegrityManager()

        if (action == Intent.ACTION_PACKAGE_ADDED || action == Intent.ACTION_PACKAGE_REPLACED) {
            val dataUri = intent.data ?: return
            val packageName = dataUri.schemeSpecificPart ?: return

            Log.i("PackageChangeReceiver", "Package event detected: $packageName (Action: $action)")

            CoroutineScope(Dispatchers.IO).launch {
                val entity = database.lockedPolicyDao().getPolicyByPackageName(packageName)
                if (entity != null) {
                    val policy = entity.toDomain()
                    val isValid = integrityManager.verifyPolicy(policy)
                    val isExpired = policy.isExpired()

                    if (isValid && !isExpired && policy.status == LockStatus.ACTIVE) {
                        Log.i("PackageChangeReceiver", "Locked package re-installed: $packageName. Enforcing lock!")
                        deviceHelper.enforcePolicyOnPackage(packageName)
                    }
                }
            }
        } else if (action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("PackageChangeReceiver", "Boot completed. Re-enforcing all active policies.")
            CoroutineScope(Dispatchers.IO).launch {
                val activeEntities = database.lockedPolicyDao().getActivePoliciesList()
                for (entity in activeEntities) {
                    val policy = entity.toDomain()
                    if (integrityManager.verifyPolicy(policy) && !policy.isExpired()) {
                        deviceHelper.enforcePolicyOnPackage(policy.packageName)
                    }
                }
            }
        }
    }
}
