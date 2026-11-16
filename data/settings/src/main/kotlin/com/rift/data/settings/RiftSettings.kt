package com.rift.data.settings

import kotlinx.serialization.Serializable

/**
 * All user-configurable settings, stored via Proto DataStore.
 * Uses kotlinx.serialization for the schema (plan section 7.11).
 */
@Serializable
data class RiftSettings(
    // Grid
    val gridColumns: Int = 4,
    val gridRows: Int = 5,
    val dockSize: Int = 5,

    // Appearance
    val darkMode: DarkMode = DarkMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val themedIcons: Boolean = true,
    val iconShape: String = "circle",
    val iconPackPackage: String? = null,

    // Gestures
    val swipeDownAction: GestureAction = GestureAction.SEARCH,
    val doubleTapAction: GestureAction = GestureAction.NONE,

    // Smart
    val smartSuggestionsEnabled: Boolean = true,
    val autoCategorizeEnabled: Boolean = true,

    // Layout
    val layoutLocked: Boolean = false,

    // Accessibility
    val reduceMotion: Boolean = false,
)

@Serializable
enum class DarkMode { LIGHT, DARK, SYSTEM }

@Serializable
enum class GestureAction { NONE, SEARCH, NOTIFICATIONS, LOCK_SCREEN, APP_DRAWER }
