package com.example.data.repository

import com.example.data.local.PersistentVaultManager
import com.example.data.local.dao.LockedPolicyDao
import com.example.data.local.entity.LockedPolicyEntity
import com.example.device.DeviceManagerHelper
import com.example.domain.model.LockStatus
import com.example.domain.model.LockedAppPolicy
import com.example.security.PolicyIntegrityManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.UUID

class PolicyRepository(
    private val dao: LockedPolicyDao,
    private val integrityManager: PolicyIntegrityManager,
    private val deviceHelper: DeviceManagerHelper,
    private val vaultManager: PersistentVaultManager
) {

    suspend fun initializeAndRestoreIfNeeded() {
        val existing = dao.getAllPoliciesList()
        if (existing.isEmpty()) {
            val recovered = vaultManager.recoverPolicies()
            if (recovered.isNotEmpty()) {
                for (policy in recovered) {
                    if (integrityManager.verifyPolicy(policy) && !policy.isExpired()) {
                        dao.insertPolicy(LockedPolicyEntity.fromDomain(policy))
                        deviceHelper.enforcePolicyOnPackage(policy.packageName)
                    }
                }
            }
        } else {
            // Keep vault updated with current active policies
            val currentPolicies = existing.map { it.toDomain() }
            vaultManager.persistPolicies(currentPolicies)
        }
    }

    val allPolicies: Flow<List<LockedAppPolicy>> = dao.getAllPolicies().map { list ->
        val mapped = list.map { entity ->
            val domain = entity.toDomain()
            val isAuthentic = integrityManager.verifyPolicy(domain)

            when {
                !isAuthentic -> {
                    domain.copy(status = LockStatus.INVALID)
                }
                domain.isExpired() -> {
                    domain.copy(status = LockStatus.EXPIRED)
                }
                else -> domain
            }
        }
        vaultManager.persistPolicies(mapped)
        mapped
    }

    suspend fun getPolicyById(id: String): LockedAppPolicy? {
        val entity = dao.getPolicyById(id) ?: return null
        val domain = entity.toDomain()
        val isAuthentic = integrityManager.verifyPolicy(domain)
        return when {
            !isAuthentic -> domain.copy(status = LockStatus.INVALID)
            domain.isExpired() -> domain.copy(status = LockStatus.EXPIRED)
            else -> domain
        }
    }

    suspend fun getPolicyByPackageName(packageName: String): LockedAppPolicy? {
        val entity = dao.getPolicyByPackageName(packageName) ?: return null
        val domain = entity.toDomain()
        val isAuthentic = integrityManager.verifyPolicy(domain)
        return when {
            !isAuthentic -> domain.copy(status = LockStatus.INVALID)
            domain.isExpired() -> domain.copy(status = LockStatus.EXPIRED)
            else -> domain
        }
    }

    suspend fun createPolicy(
        packageName: String,
        labelSnapshot: String,
        durationYears: Int
    ): Result<LockedAppPolicy> {
        if (durationYears < 1) {
            return Result.failure(IllegalArgumentException("Duration must be at least 1 year."))
        }

        val existing = dao.getPolicyByPackageName(packageName)
        if (existing != null) {
            val existingDomain = existing.toDomain()
            if (!existingDomain.isExpired() && existingDomain.status == LockStatus.ACTIVE) {
                return Result.failure(IllegalStateException("Active lock policy already exists for $packageName."))
            }
        }

        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.YEAR, durationYears)
        }
        val expiresAt = calendar.timeInMillis

        val id = UUID.randomUUID().toString()
        val preliminaryPolicy = LockedAppPolicy(
            id = id,
            packageName = packageName,
            labelSnapshot = labelSnapshot,
            startedAt = now,
            expiresAt = expiresAt,
            durationYears = durationYears,
            status = LockStatus.ACTIVE
        )

        val signature = integrityManager.signPolicy(preliminaryPolicy)
        val signedPolicy = preliminaryPolicy.copy(integritySignature = signature)

        val entity = LockedPolicyEntity.fromDomain(signedPolicy)
        dao.insertPolicy(entity)

        // Backup immediately into persistent vault
        val allCurrent = dao.getAllPoliciesList().map { it.toDomain() }
        vaultManager.persistPolicies(allCurrent)

        // Attempt real device enforcement
        deviceHelper.enforcePolicyOnPackage(packageName)
        deviceHelper.secureDateTimeSettings(true)

        return Result.success(signedPolicy)
    }

    fun isDeviceOwner(): Boolean = deviceHelper.isDeviceOwner()
    fun isDeviceAdmin(): Boolean = deviceHelper.isDeviceAdmin()
}

