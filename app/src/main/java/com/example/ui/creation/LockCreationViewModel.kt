package com.example.ui.creation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.DeadlockApp
import com.example.domain.model.InstalledAppInfo
import com.example.domain.model.LockedAppPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class LockCreationUiState(
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val filteredApps: List<InstalledAppInfo> = emptyList(),
    val searchQuery: String = "",
    val isLoadingApps: Boolean = false,
    val selectedApp: InstalledAppInfo? = null,
    val selectedYears: Int = 1,
    val isCustomYears: Boolean = false,
    val calculatedExpirationDate: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val createdPolicy: LockedAppPolicy? = null
)

class LockCreationViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DeadlockApp
    private val repository = app.policyRepository
    private val getInstalledAppsUseCase = app.getInstalledAppsUseCase

    private val _uiState = MutableStateFlow(LockCreationUiState())
    val uiState: StateFlow<LockCreationUiState> = _uiState.asStateFlow()

    init {
        loadInstalledApps()
        updateExpirationDate(1)
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingApps = true) }
            val apps = getInstalledAppsUseCase.execute()
            _uiState.update {
                it.copy(
                    installedApps = apps,
                    filteredApps = filterApps(apps, it.searchQuery),
                    isLoadingApps = false
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                filteredApps = filterApps(it.installedApps, query)
            )
        }
    }

    private fun filterApps(apps: List<InstalledAppInfo>, query: String): List<InstalledAppInfo> {
        if (query.isBlank()) return apps
        val clean = query.trim().lowercase()
        return apps.filter {
            it.label.lowercase().contains(clean) || it.packageName.lowercase().contains(clean)
        }
    }

    fun selectApp(app: InstalledAppInfo) {
        _uiState.update { it.copy(selectedApp = app) }
    }

    fun selectDuration(years: Int, isCustom: Boolean = false) {
        val validYears = years.coerceAtLeast(1)
        _uiState.update {
            it.copy(
                selectedYears = validYears,
                isCustomYears = isCustom
            )
        }
        updateExpirationDate(validYears)
    }

    private fun updateExpirationDate(years: Int) {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.YEAR, years)
        }
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val formattedDate = formatter.format(calendar.time)
        _uiState.update { it.copy(calculatedExpirationDate = formattedDate) }
    }

    fun activateLock(onSuccess: (LockedAppPolicy) -> Unit) {
        val selected = _uiState.value.selectedApp ?: return
        val years = _uiState.value.selectedYears

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            val result = repository.createPolicy(
                packageName = selected.packageName,
                labelSnapshot = selected.label,
                durationYears = years
            )
            result.onSuccess { policy ->
                _uiState.update { it.copy(isSubmitting = false, createdPolicy = policy) }
                onSuccess(policy)
            }.onFailure { ex ->
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        error = ex.localizedMessage ?: "Failed to activate lock."
                    )
                }
            }
        }
    }

    fun resetCreationState() {
        _uiState.update {
            LockCreationUiState(
                installedApps = it.installedApps,
                filteredApps = it.installedApps,
                selectedYears = 1
            )
        }
        updateExpirationDate(1)
    }
}
