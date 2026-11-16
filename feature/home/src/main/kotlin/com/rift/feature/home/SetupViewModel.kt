package com.rift.feature.home

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
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

data class SetupUiState(
    val currentStep: Int = 0,
    val totalSteps: Int = 5,
    val useSystemWallpaper: Boolean = true,
    val drawerSortMode: DrawerSortMode = DrawerSortMode.CATEGORIZED,
    val selectedFavoritePackages: Set<String> = emptySet(),
    val installedApps: List<AppInfo> = emptyList(),
    val isCompleted: Boolean = false,
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val appsRepository: AppsRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _useSystemWallpaper = MutableStateFlow(true)
    val useSystemWallpaper: StateFlow<Boolean> = _useSystemWallpaper.asStateFlow()

    private val _drawerSortMode = MutableStateFlow(DrawerSortMode.CATEGORIZED)
    val drawerSortMode: StateFlow<DrawerSortMode> = _drawerSortMode.asStateFlow()

    private val _selectedFavoritePackages = MutableStateFlow<Set<String>>(emptySet())
    val selectedFavoritePackages: StateFlow<Set<String>> = _selectedFavoritePackages.asStateFlow()

    val uiState: StateFlow<SetupUiState> = combine(
        appsRepository.allApps,
        _currentStep,
        _useSystemWallpaper,
        _drawerSortMode,
        _selectedFavoritePackages,
    ) { apps, step, useSysWallpaper, sortMode, selectedFavs ->
        // Auto-select initial top favorites if none set yet
        val initialFavs = if (selectedFavs.isEmpty()) {
            val phone = apps.find { it.packageName.contains("dialer") || it.packageName.contains("phone") }
            val msg = apps.find { it.packageName.contains("messaging") || it.packageName.contains("mms") }
            val whatsapp = apps.find { it.packageName.contains("whatsapp") }
            val snapchat = apps.find { it.packageName.contains("snapchat") }
            val camera = apps.find { it.packageName.contains("camera") }
            val browser = apps.find { it.packageName.contains("chrome") || it.packageName.contains("browser") }

            listOfNotNull(phone, msg, whatsapp, snapchat, camera, browser)
                .take(5)
                .map { it.packageName }
                .toSet()
        } else {
            selectedFavs
        }

        SetupUiState(
            currentStep = step,
            totalSteps = 5,
            useSystemWallpaper = useSysWallpaper,
            drawerSortMode = sortMode,
            selectedFavoritePackages = initialFavs,
            installedApps = apps,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SetupUiState(),
    )

    fun nextStep() {
        if (_currentStep.value < 4) {
            _currentStep.value += 1
        }
    }

    fun previousStep() {
        if (_currentStep.value > 0) {
            _currentStep.value -= 1
        }
    }

    fun setWallpaperChoice(useSystem: Boolean) {
        _useSystemWallpaper.value = useSystem
    }

    fun setDrawerSortChoice(mode: DrawerSortMode) {
        _drawerSortMode.value = mode
    }

    fun toggleFavoritePackage(pkg: String) {
        val current = _selectedFavoritePackages.value.toMutableSet()
        if (current.contains(pkg)) {
            current.remove(pkg)
        } else {
            if (current.size < 5) {
                current.add(pkg)
            }
        }
        _selectedFavoritePackages.value = current
    }

    fun requestDefaultLauncher(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                    if (context is Activity) {
                        context.startActivity(intent)
                    } else {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                    return
                }
            }
        }
        // Fallback to home settings
        val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        }
    }

    fun finishSetup(onDone: () -> Unit) {
        viewModelScope.launch {
            val favs = uiState.value.selectedFavoritePackages.toList()
            settingsRepository.update {
                it.copy(
                    hasCompletedSetup = true,
                    useSystemWallpaper = _useSystemWallpaper.value,
                    drawerSortMode = _drawerSortMode.value,
                    favoriteAppPackages = favs,
                )
            }
            onDone()
        }
    }
}
