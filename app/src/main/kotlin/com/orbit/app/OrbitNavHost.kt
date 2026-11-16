package com.orbit.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Root composable for Orbit Launcher.
 * Will host navigation between Home, Drawer, Search, Settings, etc.
 */
@Composable
fun OrbitApp(modifier: Modifier = Modifier) {
    // TODO: Implement navigation host with Home as start destination
    com.orbit.feature.home.HomeScreen(modifier = modifier.fillMaxSize())
}
