package com.orbit.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Primary brand colors (fallback when Dynamic Color is unavailable)
private val PrimaryLight = Color(0xFF1A73E8)
private val OnPrimaryLight = Color(0xFFFFFFFF)
private val PrimaryContainerLight = Color(0xFFD3E3FD)
private val OnPrimaryContainerLight = Color(0xFF041E49)

private val SecondaryLight = Color(0xFF5F6368)
private val OnSecondaryLight = Color(0xFFFFFFFF)
private val SecondaryContainerLight = Color(0xFFE8EAED)
private val OnSecondaryContainerLight = Color(0xFF1F2023)

private val SurfaceLight = Color(0xFFFEFEFE)
private val OnSurfaceLight = Color(0xFF1F1F1F)
private val SurfaceVariantLight = Color(0xFFF1F3F4)

private val PrimaryDark = Color(0xFF8AB4F8)
private val OnPrimaryDark = Color(0xFF062E6F)
private val PrimaryContainerDark = Color(0xFF1A73E8)
private val OnPrimaryContainerDark = Color(0xFFD3E3FD)

private val SecondaryDark = Color(0xFFBDC1C6)
private val OnSecondaryDark = Color(0xFF303134)
private val SecondaryContainerDark = Color(0xFF5F6368)
private val OnSecondaryContainerDark = Color(0xFFE8EAED)

private val SurfaceDark = Color(0xFF1F1F1F)
private val OnSurfaceDark = Color(0xFFE8EAED)
private val SurfaceVariantDark = Color(0xFF303134)

val OrbitLightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
)

val OrbitDarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
)
