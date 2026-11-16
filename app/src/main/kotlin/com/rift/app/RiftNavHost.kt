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
import com.rift.feature.drawer.DrawerScreen
import com.rift.feature.home.HomeScreen
import com.rift.feature.search.SearchScreen

enum class LauncherScreen {
    HOME,
    DRAWER,
    SEARCH,
}

/**
 * Root composable for Rift Launcher with predictive back handling and smooth animated transitions.
 */
@Composable
fun RiftApp(modifier: Modifier = Modifier) {
    var currentScreen by remember { mutableStateOf(LauncherScreen.DRAWER) }

    BackHandler(enabled = currentScreen != LauncherScreen.HOME) {
        currentScreen = LauncherScreen.HOME
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "LauncherNavigation",
        modifier = modifier.fillMaxSize(),
    ) { screen ->
        when (screen) {
            LauncherScreen.HOME -> {
                HomeScreen(
                    modifier = Modifier.fillMaxSize(),
                    onOpenDrawer = { currentScreen = LauncherScreen.DRAWER },
                    onOpenSearch = { currentScreen = LauncherScreen.SEARCH },
                )
            }
            LauncherScreen.DRAWER -> {
                DrawerScreen(
                    modifier = Modifier.fillMaxSize(),
                    onOpenSearch = { currentScreen = LauncherScreen.SEARCH },
                )
            }
            LauncherScreen.SEARCH -> {
                SearchScreen(
                    modifier = Modifier.fillMaxSize(),
                    onClose = { currentScreen = LauncherScreen.DRAWER },
                )
            }
        }
    }
}
