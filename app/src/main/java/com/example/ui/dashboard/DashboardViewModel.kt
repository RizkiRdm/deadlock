package com.example.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.DeadlockApp
import com.example.device.DeadlockAccessibilityService
import com.example.domain.model.LockedAppPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val policies: List<LockedAppPolicy> = emptyList(),
    val activeCount: Int = 0,
    val isDeviceOwner: Boolean = false,
    val isDeviceAdmin: Boolean = false,
    val isAccessibilityActive: Boolean = false
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DeadlockApp
    private val repository = app.policyRepository
    private val _accessibilityState = MutableStateFlow(
        DeadlockAccessibilityService.isAccessibilityServiceEnabled(application)
    )

    fun refreshServiceStatus() {
        _accessibilityState.value = DeadlockAccessibilityService.isAccessibilityServiceEnabled(getApplication())
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.allPolicies,
        _accessibilityState
    ) { policies, isAccessibilityActive ->
        DashboardUiState(
            policies = policies,
            activeCount = policies.count { it.status.name == "ACTIVE" },
            isDeviceOwner = repository.isDeviceOwner(),
            isDeviceAdmin = repository.isDeviceAdmin(),
            isAccessibilityActive = isAccessibilityActive
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(
            isDeviceOwner = repository.isDeviceOwner(),
            isDeviceAdmin = repository.isDeviceAdmin(),
            isAccessibilityActive = DeadlockAccessibilityService.isAccessibilityServiceEnabled(application)
        )
    )
}
