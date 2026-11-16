package com.rift.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun RiftTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= 31 && darkTheme -> runCatching { dynamicDarkColorScheme(context) }.getOrDefault(RiftDarkColorScheme)
        dynamicColor && Build.VERSION.SDK_INT >= 31 -> runCatching { dynamicLightColorScheme(context) }.getOrDefault(RiftLightColorScheme)
        darkTheme -> RiftDarkColorScheme
        else -> RiftLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = RiftShapes,
        typography = RiftTypography,
        content = content,
    )
}
