package com.archieapps.applock.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.archieapps.applock.data.model.InstalledApp
import com.archieapps.applock.data.repository.BlockedAppsRepository
import com.archieapps.applock.data.repository.InstalledAppsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppListUiState(
    val isLoading: Boolean = true,
    val apps: List<InstalledApp> = emptyList(),
    val query: String = "",
    val pendingCriticalPackage: String? = null,
    val pendingCriticalAppName: String = "",
)

class AppListViewModel(
    private val installedAppsRepository: InstalledAppsRepository,
    private val blockedAppsRepository: BlockedAppsRepository,
) : ViewModel() {

    private val installedApps = MutableStateFlow<List<InstalledApp>?>(null)
    private val query = MutableStateFlow("")
    private val criticalPackages = MutableStateFlow<Set<String>>(emptySet())
    private val pendingCriticalPackage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AppListUiState> = combine(
        installedApps,
        blockedAppsRepository.blockedPackages,
        query,
        pendingCriticalPackage,
    ) { apps, blocked, currentQuery, pending ->
        if (apps == null) {
            AppListUiState(isLoading = true, query = currentQuery)
        } else {
            AppListUiState(
                isLoading = false,
                apps = apps
                    .filter { it.name.contains(currentQuery.trim(), ignoreCase = true) }
                    .map { it.copy(isBlocked = it.packageName in blocked) },
                query = currentQuery,
                pendingCriticalPackage = pending,
                pendingCriticalAppName = pending
                    ?.let { pkg -> apps.firstOrNull { it.packageName == pkg }?.name ?: pkg }
                    .orEmpty(),
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppListUiState(),
    )

    init {
        viewModelScope.launch {
            installedApps.value = installedAppsRepository.loadInstalledApps()
        }
        viewModelScope.launch {
            criticalPackages.value = withContext(Dispatchers.IO) {
                installedAppsRepository.criticalPackages()
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        query.update { newQuery }
    }

    fun onToggleBlock(packageName: String, blocked: Boolean) {
        if (blocked && packageName in criticalPackages.value) {
            pendingCriticalPackage.value = packageName
            return
        }
        persistBlock(packageName, blocked)
    }

    fun confirmCriticalBlock() {
        val packageName = pendingCriticalPackage.value ?: return
        pendingCriticalPackage.value = null
        persistBlock(packageName, blocked = true)
    }

    fun dismissCriticalBlock() {
        pendingCriticalPackage.value = null
    }

    private fun persistBlock(packageName: String, blocked: Boolean) {
        viewModelScope.launch {
            blockedAppsRepository.setBlocked(packageName, blocked)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                AppListViewModel(
                    installedAppsRepository = InstalledAppsRepository(application),
                    blockedAppsRepository = BlockedAppsRepository(application),
                )
            }
        }
    }
}
