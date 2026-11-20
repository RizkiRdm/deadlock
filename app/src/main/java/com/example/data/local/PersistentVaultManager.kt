package com.example.data.local

import android.content.Context
import android.util.Log
import com.example.domain.model.LockStatus
import com.example.domain.model.LockedAppPolicy
import com.example.security.PolicyIntegrityManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class PersistentVaultManager(private val context: Context) {

    private val integrityManager = PolicyIntegrityManager()

    companion object {
        private const val VAULT_FILENAME = "deadlock_persistent_vault.json"
        private const val BACKUP_FILENAME = "deadlock_secure_backup.json"
    }

    private fun getStorageFiles(): List<File> {
        val files = mutableListOf<File>()

        try {
            // 1. Device Protected Storage (survives credential-encrypted data clears on modern Android)
            val directBootContext = context.createDeviceProtectedStorageContext()
            files.add(File(directBootContext.filesDir, VAULT_FILENAME))
        } catch (e: Exception) {
            Log.w("PersistentVault", "Device protected storage unavailable: ${e.message}")
        }

        // 2. Standard noBackupFilesDir (immune to standard cloud and system backup resets)
        try {
            files.add(File(context.noBackupFilesDir, VAULT_FILENAME))
        } catch (e: Exception) {
            Log.w("PersistentVault", "noBackupFilesDir unavailable: ${e.message}")
        }

        // 3. Fallback persistent filesDir
        try {
            files.add(File(context.filesDir, BACKUP_FILENAME))
        } catch (e: Exception) {
            Log.w("PersistentVault", "filesDir unavailable: ${e.message}")
        }

        return files
    }

    fun persistPolicies(policies: List<LockedAppPolicy>) {
        try {
            val jsonArray = JSONArray()
            for (policy in policies) {
                val obj = JSONObject().apply {
                    put("id", policy.id)
                    put("packageName", policy.packageName)
                    put("labelSnapshot", policy.labelSnapshot)
                    put("startedAt", policy.startedAt)
                    put("expiresAt", policy.expiresAt)
                    put("durationYears", policy.durationYears)
                    put("status", policy.status.name)
                    put("integritySignature", policy.integritySignature)
                    put("lastVerifiedAt", policy.lastVerifiedAt)
                }
                jsonArray.put(obj)
            }
            val rawJson = jsonArray.toString()

            for (file in getStorageFiles()) {
                try {
                    file.parentFile?.mkdirs()
                    file.writeText(rawJson, Charsets.UTF_8)
                    Log.d("PersistentVault", "Successfully mirrored ${policies.size} policies to ${file.absolutePath}")
                } catch (e: Exception) {
                    Log.e("PersistentVault", "Failed writing to ${file.name}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("PersistentVault", "Failed to encode policies to vault", e)
        }
    }

    fun recoverPolicies(): List<LockedAppPolicy> {
        for (file in getStorageFiles()) {
            if (file.exists() && file.length() > 0) {
                try {
                    val rawJson = file.readText(Charsets.UTF_8)
                    val jsonArray = JSONArray(rawJson)
                    val recovered = mutableListOf<LockedAppPolicy>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val policy = LockedAppPolicy(
                            id = obj.optString("id", ""),
                            packageName = obj.optString("packageName", ""),
                            labelSnapshot = obj.optString("labelSnapshot", ""),
                            startedAt = obj.optLong("startedAt", 0L),
                            expiresAt = obj.optLong("expiresAt", 0L),
                            durationYears = obj.optInt("durationYears", 1),
                            status = try {
                                LockStatus.valueOf(obj.optString("status", "ACTIVE"))
                            } catch (e: Exception) {
                                LockStatus.ACTIVE
                            },
                            integritySignature = obj.optString("integritySignature", ""),
                            lastVerifiedAt = obj.optLong("lastVerifiedAt", 0L)
                        )
                        recovered.add(policy)
                    }

                    // Authenticate cryptographic integrity
                    val authenticPolicies = recovered.filter { policy ->
                        integrityManager.verifyPolicy(policy)
                    }

                    if (authenticPolicies.isNotEmpty()) {
                        Log.i("PersistentVault", "Recovered ${authenticPolicies.size} valid policies from ${file.name}")
                        return authenticPolicies
                    }
                } catch (e: Exception) {
                    Log.e("PersistentVault", "Error parsing vault file: ${file.name}", e)
                }
            }
        }
        return emptyList()
    }
}

