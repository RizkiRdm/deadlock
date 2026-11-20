package com.example

import android.app.Application
import com.example.data.local.DeadlockDatabase
import com.example.data.local.OnboardingPreferences
import com.example.data.local.PersistentVaultManager
import com.example.data.repository.PolicyRepository
import com.example.device.DeviceManagerHelper
import com.example.domain.model.LockStatus
import com.example.domain.usecase.GetInstalledAppsUseCase
import com.example.security.PolicyIntegrityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DeadlockApp : Application() {

    lateinit var database: DeadlockDatabase
        private set

    lateinit var integrityManager: PolicyIntegrityManager
        private set

    lateinit var deviceHelper: DeviceManagerHelper
        private set

    lateinit var vaultManager: PersistentVaultManager
        private set

    lateinit var onboardingPreferences: OnboardingPreferences
        private set

    lateinit var policyRepository: PolicyRepository
        private set

    lateinit var getInstalledAppsUseCase: GetInstalledAppsUseCase
        private set

    private val _activeLockedPackages = MutableStateFlow<Set<String>>(emptySet())
    val activeLockedPackages = _activeLockedPackages.asStateFlow()

    private val appScope = CoroutineScope(Dispatchers.IO)

    companion object {
        lateinit var instance: DeadlockApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = DeadlockDatabase.getInstance(this)
        integrityManager = PolicyIntegrityManager()
        deviceHelper = DeviceManagerHelper(this)
        vaultManager = PersistentVaultManager(this)
        onboardingPreferences = OnboardingPreferences(this)
        policyRepository = PolicyRepository(
            dao = database.lockedPolicyDao(),
            integrityManager = integrityManager,
            deviceHelper = deviceHelper,
            vaultManager = vaultManager
        )
        getInstalledAppsUseCase = GetInstalledAppsUseCase(this)

        appScope.launch {
            // Restore from persistent vault if data was cleared
            policyRepository.initializeAndRestoreIfNeeded()

            // Keep active packages in instant-lookup memory
            policyRepository.allPolicies.collect { policies ->
                val activeSet = policies
                    .filter { it.status == LockStatus.ACTIVE && !it.isExpired() }
                    .map { it.packageName }
                    .toSet()
                _activeLockedPackages.value = activeSet
            }
        }
    }
}

