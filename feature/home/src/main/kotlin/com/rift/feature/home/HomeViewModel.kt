package com.rift.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rift.data.apps.AppInfo
import com.rift.data.apps.AppsRepository
import com.rift.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val hasCompletedSetup: Boolean = false,
    val useSystemWallpaper: Boolean = true,
    val favoriteApps: List<AppInfo> = emptyList(),
    val allInstalledApps: List<AppInfo> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appsRepository: AppsRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        appsRepository.allApps,
        settingsRepository.settings,
    ) { apps, settings ->
        // 1. Identify Candidate Suggested Apps if none set
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
            candidates.take(5)
        }

        HomeUiState(
            hasCompletedSetup = settings.hasCompletedSetup,
            useSystemWallpaper = settings.useSystemWallpaper,
            favoriteApps = favoriteApps,
            allInstalledApps = apps,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(),
    )

    fun launchApp(app: AppInfo) {
        appsRepository.launchApp(app)
    }

    fun moveFavoriteApp(app: AppInfo, direction: Int) {
        viewModelScope.launch {
            val currentList = uiState.value.favoriteApps.map { it.packageName }.toMutableList()
            val index = currentList.indexOf(app.packageName)
            if (index != -1) {
                val newIndex = (index + direction).coerceIn(0, currentList.size - 1)
                if (index != newIndex) {
                    val item = currentList.removeAt(index)
                    currentList.add(newIndex, item)
                    settingsRepository.update { it.copy(favoriteAppPackages = currentList) }
                }
            }
        }
    }

    fun removeFavoriteApp(app: AppInfo) {
        viewModelScope.launch {
            val currentList = uiState.value.favoriteApps.map { it.packageName }.toMutableList()
            currentList.remove(app.packageName)
            settingsRepository.update { it.copy(favoriteAppPackages = currentList) }
        }
    }

    fun addFavoriteApp(app: AppInfo) {
        viewModelScope.launch {
            val currentList = uiState.value.favoriteApps.map { it.packageName }.toMutableList()
            if (!currentList.contains(app.packageName) && currentList.size < 5) {
                currentList.add(app.packageName)
                settingsRepository.update { it.copy(favoriteAppPackages = currentList) }
            }
        }
    }

    fun replaceFavoriteApp(oldApp: AppInfo, newApp: AppInfo) {
        viewModelScope.launch {
            val currentList = uiState.value.favoriteApps.map { it.packageName }.toMutableList()
            val index = currentList.indexOf(oldApp.packageName)
            if (index != -1) {
                currentList[index] = newApp.packageName
            } else {
                currentList.add(newApp.packageName)
            }
            settingsRepository.update { it.copy(favoriteAppPackages = currentList.take(5)) }
        }
    }
}
