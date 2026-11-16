package com.rift.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rift.feature.drawer.DrawerScreen
import com.rift.feature.home.HomeScreen
import com.rift.feature.home.HomeViewModel
import com.rift.feature.home.SetupScreen
import com.rift.feature.search.SearchScreen
import com.rift.feature.settings.SettingsScreen

enum class LauncherScreen {
    SETUP,
    HOME,
    DRAWER,
    SEARCH,
    SETTINGS,
}

/**
 * Root composable for Rift Launcher with full-screen setup wizard, predictive back handling,
 * and fluid animated transitions.
 */
@Composable
fun RiftLauncherRoot(
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()

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
        }
    }
}
