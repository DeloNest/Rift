package com.rift.app

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rift.app.service.RiftAccessibilityService
import com.rift.feature.drawer.DrawerScreen
import com.rift.feature.home.HomeScreen
import com.rift.feature.home.HomeViewModel
import com.rift.feature.home.SetupScreen
import com.rift.feature.search.SearchScreen
import com.rift.feature.settings.SettingsScreen
import com.rift.feature.widgets.WidgetsScreen

enum class LauncherScreen {
    SETUP,
    HOME,
    DRAWER,
    SEARCH,
    SETTINGS,
    WIDGETS,
}

/**
 * Root composable for Rift Launcher with full-screen setup wizard, predictive back handling,
 * and fluid animated transitions across all launcher screens.
 */
@Composable
fun RiftLauncherRoot(
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAccessibilityDialog by remember { mutableStateOf(false) }

    var currentScreen by remember(homeUiState.hasCompletedSetup) {
        mutableStateOf(
            if (!homeUiState.hasCompletedSetup) LauncherScreen.SETUP else LauncherScreen.HOME
        )
    }

    BackHandler(enabled = currentScreen != LauncherScreen.HOME && currentScreen != LauncherScreen.SETUP) {
        currentScreen = LauncherScreen.HOME
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "LauncherNavigation",
        modifier = modifier.fillMaxSize(),
    ) { screen ->
        when (screen) {
            LauncherScreen.SETUP -> {
                SetupScreen(
                    onFinish = { currentScreen = LauncherScreen.HOME },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            LauncherScreen.HOME -> {
                HomeScreen(
                    modifier = Modifier.fillMaxSize(),
                    viewModel = homeViewModel,
                    onOpenDrawer = { currentScreen = LauncherScreen.DRAWER },
                    onOpenSearch = { currentScreen = LauncherScreen.SEARCH },
                    onOpenSettings = { currentScreen = LauncherScreen.SETTINGS },
                    onOpenWidgets = { currentScreen = LauncherScreen.WIDGETS },
                    onDoubleTapWallpaper = {
                        if (RiftAccessibilityService.isEnabled()) {
                            RiftAccessibilityService.lockScreen()
                        } else {
                            showAccessibilityDialog = true
                        }
                    },
                )
            }
            LauncherScreen.DRAWER -> {
                DrawerScreen(
                    modifier = Modifier.fillMaxSize(),
                    onOpenSearch = { currentScreen = LauncherScreen.SEARCH },
                    onOpenSettings = { currentScreen = LauncherScreen.SETTINGS },
                )
            }
            LauncherScreen.SEARCH -> {
                SearchScreen(
                    modifier = Modifier.fillMaxSize(),
                    onClose = { currentScreen = LauncherScreen.HOME },
                )
            }
            LauncherScreen.SETTINGS -> {
                SettingsScreen(
                    modifier = Modifier.fillMaxSize(),
                    onNavigateBack = { currentScreen = LauncherScreen.HOME },
                    onOpenSetup = { currentScreen = LauncherScreen.SETUP },
                )
            }
            LauncherScreen.WIDGETS -> {
                WidgetsScreen(
                    onNavigateBack = { currentScreen = LauncherScreen.HOME },
                    onOpenSearch = { currentScreen = LauncherScreen.SEARCH },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    if (showAccessibilityDialog) {
        AlertDialog(
            onDismissRequest = { showAccessibilityDialog = false },
            title = { Text("النقر المزدوج لإغلاق الشاشة") },
            text = {
                Text("يتطلب قفل الشاشة بنقرتين تفعيل خدمة إمكانية الوصول الخاصة بمشغّل Rift في إعدادات النظام، للحفاظ على فتح القفل بالبصمة دون مشاكل.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showAccessibilityDialog = false
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    }
                }) {
                    Text("تفعيل الآن (فتح الإعدادات)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccessibilityDialog = false }) {
                    Text("إلغاء")
                }
            },
        )
    }
}
