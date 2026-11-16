package com.rift.feature.drawer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rift.data.apps.AppCategory
import com.rift.data.apps.AppCategorizer
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DrawerUiState(
    val selectedCategory: AppCategory = AppCategory.GENERAL,
    val categories: List<AppCategory> = AppCategory.entries.sortedBy { it.order },
    val categorizedApps: Map<AppCategory, List<AppInfo>> = emptyMap(),
    val currentCategoryApps: List<AppInfo> = emptyList(),
    val allAlphabeticalApps: List<AppInfo> = emptyList(),
    val sortMode: DrawerSortMode = DrawerSortMode.CATEGORIZED,
    val totalAppsCount: Int = 0,
)

@HiltViewModel
class DrawerViewModel @Inject constructor(
    private val appsRepository: AppsRepository,
    private val appCategorizer: AppCategorizer,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(AppCategory.GENERAL)
    val selectedCategory: StateFlow<AppCategory> = _selectedCategory.asStateFlow()

    private val _manualOrder = MutableStateFlow<Map<String, Int>>(emptyMap())

    val uiState: StateFlow<DrawerUiState> = combine(
        appsRepository.allApps,
        _selectedCategory,
        _manualOrder,
        settingsRepository.settings,
    ) { apps, selectedCat, manualOrder, settings ->
        // 1. Resolve Categories Order (User custom reordering or standard order)
        val resolvedCategories = if (settings.categoryOrder.isNotEmpty()) {
            val categoryMap = AppCategory.entries.associateBy { it.name }
            val ordered = settings.categoryOrder.mapNotNull { categoryMap[it] }.toMutableList()
            for (cat in AppCategory.entries) {
                if (!ordered.contains(cat)) ordered.add(cat)
            }
            ordered
        } else {
            AppCategory.entries.sortedBy { it.order }
        }

        // Group apps by category
        val grouped = mutableMapOf<AppCategory, MutableList<AppInfo>>()
        for (cat in resolvedCategories) {
            grouped[cat] = mutableListOf()
        }

        val hiddenSet = settings.hiddenAppPackages.toSet()
        val visibleApps = apps.filterNot { hiddenSet.contains(it.packageName) }

        for (app in visibleApps) {
            val category = appCategorizer.categorize(app)
            grouped[category]?.add(app)
        }

        // Sort each category by manual order if present, then alphabetically
        val finalMap = grouped.mapValues { (_, appList) ->
            appList.sortedWith(
                compareBy<AppInfo> { manualOrder[it.packageName] ?: Int.MAX_VALUE }
                    .thenBy { it.label.lowercase() }
            )
        }

        val allAlphabetical = visibleApps.sortedBy { it.label.lowercase() }

        DrawerUiState(
            selectedCategory = selectedCat,
            categories = resolvedCategories,
            categorizedApps = finalMap,
            currentCategoryApps = finalMap[selectedCat] ?: emptyList(),
            allAlphabeticalApps = allAlphabetical,
            sortMode = settings.drawerSortMode,
            totalAppsCount = visibleApps.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DrawerUiState(),
    )

    fun selectCategory(category: AppCategory) {
        _selectedCategory.value = category
    }

    fun setSortMode(mode: DrawerSortMode) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(drawerSortMode = mode) }
        }
    }

    /**
     * Direct Touch Reorder: Move category from [fromIndex] to [toIndex].
     */
    fun reorderCategories(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val currentList = uiState.value.categories.toMutableList()
            if (fromIndex in currentList.indices && toIndex in currentList.indices && fromIndex != toIndex) {
                val item = currentList.removeAt(fromIndex)
                currentList.add(toIndex, item)
                settingsRepository.update {
                    it.copy(categoryOrder = currentList.map { c -> c.name })
                }
            }
        }
    }

    fun moveCategory(category: AppCategory, direction: Int) {
        viewModelScope.launch {
            val currentList = uiState.value.categories.toMutableList()
            val index = currentList.indexOf(category)
            val newIndex = (index + direction).coerceIn(0, currentList.size - 1)
            if (index != newIndex) {
                currentList.removeAt(index)
                currentList.add(newIndex, category)
                settingsRepository.update {
                    it.copy(categoryOrder = currentList.map { c -> c.name })
                }
            }
        }
    }

    fun moveAppToCategory(app: AppInfo, newCategory: AppCategory) {
        viewModelScope.launch {
            appCategorizer.setOverride(app.packageName, newCategory)
            _manualOrder.value = _manualOrder.value.toMutableMap().also { it[app.packageName] = 0 }
        }
    }

    fun addAppToDock(app: AppInfo) {
        viewModelScope.launch {
            val currentFavorites = settingsRepository.settings.first().favoriteAppPackages.toMutableList()
            if (!currentFavorites.contains(app.packageName)) {
                currentFavorites.add(app.packageName)
                settingsRepository.update { it.copy(favoriteAppPackages = currentFavorites.take(5)) }
            }
        }
    }

    fun addAppToDesktop(app: AppInfo) {
        viewModelScope.launch {
            val currentDesktop = settingsRepository.settings.first().desktopAppPackages.toMutableList()
            if (!currentDesktop.contains(app.packageName)) {
                currentDesktop.add(app.packageName)
                settingsRepository.update { it.copy(desktopAppPackages = currentDesktop) }
            }
        }
    }

    fun openAppDetails(context: Context, app: AppInfo) {
        runCatching {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", app.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun requestUninstallApp(context: Context, app: AppInfo) {
        runCatching {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.fromParts("package", app.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun hideApp(app: AppInfo) {
        viewModelScope.launch {
            val currentHidden = settingsRepository.settings.first().hiddenAppPackages.toMutableList()
            if (!currentHidden.contains(app.packageName)) {
                currentHidden.add(app.packageName)
                settingsRepository.update { it.copy(hiddenAppPackages = currentHidden) }
            }
        }
    }

    fun launchApp(app: AppInfo) {
        appsRepository.launchApp(app)
    }
}
