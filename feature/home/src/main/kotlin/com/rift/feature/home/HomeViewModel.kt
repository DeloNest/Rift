package com.rift.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rift.data.apps.AppInfo
import com.rift.data.apps.AppsRepository
import com.rift.data.settings.DrawerSortMode
import com.rift.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val hasCompletedSetup: Boolean = false,
    val favoriteApps: List<AppInfo> = emptyList(),
    val allInstalledApps: List<AppInfo> = emptyList(),
    val candidateApps: List<AppInfo> = emptyList(),
    val selectedPackagesForSetup: Set<String> = emptySet(),
    val selectedSortModeForSetup: DrawerSortMode = DrawerSortMode.CATEGORIZED,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appsRepository: AppsRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _selectedPackagesForSetup = MutableStateFlow<Set<String>>(emptySet())
    val selectedPackagesForSetup: StateFlow<Set<String>> = _selectedPackagesForSetup.asStateFlow()

    private val _selectedSortModeForSetup = MutableStateFlow(DrawerSortMode.CATEGORIZED)
    val selectedSortModeForSetup: StateFlow<DrawerSortMode> = _selectedSortModeForSetup.asStateFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        appsRepository.allApps,
        settingsRepository.settings,
        _selectedPackagesForSetup,
        _selectedSortModeForSetup,
    ) { apps, settings, selectedForSetup, sortModeForSetup ->
        // 1. Identify Candidate Suggested Apps (Phone, SMS, WhatsApp, Snapchat, Browser, Camera)
        val candidates = mutableListOf<AppInfo>()
        val phoneApp = apps.find { it.packageName.contains("dialer") || it.packageName.contains("phone") }
        val msgApp = apps.find { it.packageName.contains("messaging") || it.packageName.contains("mms") }
        val whatsappApp = apps.find { it.packageName.contains("whatsapp") }
        val snapchatApp = apps.find { it.packageName.contains("snapchat") }
        val browserApp = apps.find { it.packageName.contains("chrome") || it.packageName.contains("browser") }
        val cameraApp = apps.find { it.packageName.contains("camera") }

        listOfNotNull(phoneApp, msgApp, whatsappApp, snapchatApp, browserApp, cameraApp).forEach {
            if (!candidates.contains(it)) candidates.add(it)
        }

        // Add others if fewer than 6
        for (app in apps) {
            if (candidates.size >= 8) break
            if (!candidates.contains(app)) candidates.add(app)
        }

        // 2. Resolve Active Favorite Apps
        val favoriteApps = if (settings.favoriteAppPackages.isNotEmpty()) {
            val appMap = apps.associateBy { it.packageName }
            settings.favoriteAppPackages.mapNotNull { appMap[it] }
        } else {
            // Default out of the box favorites
            candidates.take(4)
        }

        // Initialize setup selection if empty
        val effectiveSelectedSetup = if (selectedForSetup.isEmpty()) {
            favoriteApps.map { it.packageName }.toSet()
        } else {
            selectedForSetup
        }

        HomeUiState(
            hasCompletedSetup = settings.hasCompletedSetup,
            favoriteApps = favoriteApps,
            allInstalledApps = apps,
            candidateApps = candidates,
            selectedPackagesForSetup = effectiveSelectedSetup,
            selectedSortModeForSetup = sortModeForSetup,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(),
    )

    fun toggleSetupFavorite(packageName: String) {
        val current = _selectedPackagesForSetup.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            if (current.size < 5) {
                current.add(packageName)
            }
        }
        _selectedPackagesForSetup.value = current
    }

    fun setSetupSortMode(mode: DrawerSortMode) {
        _selectedSortModeForSetup.value = mode
    }

    fun completeSetup() {
        viewModelScope.launch {
            val selected = _selectedPackagesForSetup.value.ifEmpty {
                uiState.value.favoriteApps.map { it.packageName }.toSet()
            }
            settingsRepository.update {
                it.copy(
                    hasCompletedSetup = true,
                    favoriteAppPackages = selected.toList(),
                    drawerSortMode = _selectedSortModeForSetup.value,
                )
            }
        }
    }

    fun skipSetup() {
        viewModelScope.launch {
            settingsRepository.update {
                it.copy(hasCompletedSetup = true)
            }
        }
    }

    fun rerunSetup() {
        viewModelScope.launch {
            settingsRepository.update {
                it.copy(hasCompletedSetup = false)
            }
        }
    }

    fun launchApp(app: AppInfo) {
        appsRepository.launchApp(app)
    }
}
