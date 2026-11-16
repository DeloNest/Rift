package com.rift.feature.home

import android.content.Context
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rift.core.ui.AppIcon
import com.rift.data.apps.AppInfo
import kotlin.math.abs
import kotlin.math.roundToInt

enum class AppSourceLocation { DOCK, DESKTOP }

data class SelectedAppAction(
    val app: AppInfo,
    val source: AppSourceLocation,
)

/**
 * Ultra-clean, intuitive home desktop with direct touch Drag-and-Drop.
 * - Long-press & drag horizontally directly reorders dock apps in real-time with fluid spring animations.
 * - Pinned desktop apps support direct drag-and-drop reordering.
 * - Long-press on an app reveals quick contextual actions (Pin to Desktop, Move to Dock, Info, Uninstall).
 * - Distraction-free: NO top bar, NO center clock; wallpaper is 100% visible.
 * - Swipe UP anywhere opens the App Drawer; Swipe DOWN opens Search.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    onOpenDrawer: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenWidgets: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedAppForActions by remember { mutableStateOf<SelectedAppAction?>(null) }
    var showDesktopMenuSheet by remember { mutableStateOf(false) }

    val bgModifier = if (uiState.useSystemWallpaper) {
        Modifier.background(Color.Transparent)
    } else {
        Modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0E0E12),
                    Color(0xFF13141C),
                    Color(0xFF181926),
                )
            )
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .then(bgModifier)
                .statusBarsPadding()
                .navigationBarsPadding()
                // Long press on empty wallpaper area opens quick desktop menu
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            showDesktopMenuSheet = true
                        }
                    )
                }
                // Gestures: Swipe Up -> Drawer, Swipe Down -> Search
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount < -35) {
                            onOpenDrawer()
                        } else if (dragAmount > 35) {
                            onOpenSearch()
                        }
                    }
                }
                // Gestures: Horizontal Swipe -> Open Widgets Screen
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        if (abs(dragAmount) > 35) {
                            onOpenWidgets()
                        }
                    }
                },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Top area: Desktop Pinned Apps (if user pinned apps to home screen)
                if (uiState.desktopApps.isNotEmpty()) {
                    DesktopAppsGrid(
                        apps = uiState.desktopApps,
                        onLaunchApp = viewModel::launchApp,
                        onReorder = viewModel::reorderDesktopApps,
                        onAppLongClick = { app ->
                            selectedAppForActions = SelectedAppAction(app, AppSourceLocation.DESKTOP)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(top = 16.dp),
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Bottom Dock: Direct Drag-and-Drop Reorderable Favorite Apps
                DirectReorderableDock(
                    apps = uiState.favoriteApps.take(5),
                    onLaunchApp = viewModel::launchApp,
                    onReorder = viewModel::reorderFavoriteApps,
                    onAppActionMenu = { app ->
                        selectedAppForActions = SelectedAppAction(app, AppSourceLocation.DOCK)
                    },
                )
            }

            // Quick Contextual App Action Sheet
            selectedAppForActions?.let { actionItem ->
                AppActionBottomSheet(
                    actionItem = actionItem,
                    context = context,
                    onPinToDesktop = {
                        viewModel.addAppToDesktop(actionItem.app)
                        selectedAppForActions = null
                    },
                    onRemoveFromDesktop = {
                        viewModel.removeAppFromDesktop(actionItem.app)
                        selectedAppForActions = null
                    },
                    onMoveToDock = {
                        viewModel.addAppToDock(actionItem.app)
                        selectedAppForActions = null
                    },
                    onRemoveFromDock = {
                        viewModel.removeAppFromDock(actionItem.app)
                        selectedAppForActions = null
                    },
                    onOpenAppDetails = {
                        viewModel.openAppDetails(context, actionItem.app)
                        selectedAppForActions = null
                    },
                    onRequestUninstall = {
                        viewModel.requestUninstallApp(context, actionItem.app)
                        selectedAppForActions = null
                    },
                    onDismiss = { selectedAppForActions = null },
                )
            }

            // Desktop Long-Press Menu (Settings, Search, Widgets)
            if (showDesktopMenuSheet) {
                DesktopMenuBottomSheet(
                    onOpenWidgets = {
                        showDesktopMenuSheet = false
                        onOpenWidgets()
                    },
                    onOpenSettings = {
                        showDesktopMenuSheet = false
                        onOpenSettings()
                    },
                    onOpenSearch = {
                        showDesktopMenuSheet = false
                        onOpenSearch()
                    },
                    onDismiss = { showDesktopMenuSheet = false },
                )
            }
        }
    }
}

/**
 * Direct Touch Reorderable Dock.
 * Users grab an app by long-pressing and drag it horizontally; adjacent apps slide smoothly out of the way.
 */
@Composable
fun DirectReorderableDock(
    apps: List<AppInfo>,
    onLaunchApp: (AppInfo) -> Unit,
    onReorder: (Int, Int) -> Unit,
    onAppActionMenu: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dockWidthPx by remember { mutableFloatStateOf(0f) }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Surface(
        shape = RoundedCornerShape(32.dp),
        color = Color.Black.copy(alpha = 0.38f),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .onGloballyPositioned { coordinates ->
                dockWidthPx = coordinates.size.width.toFloat()
            },
    ) {
        val slotWidth = if (apps.isNotEmpty() && dockWidthPx > 0) dockWidthPx / apps.size else 1f

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            apps.forEachIndexed { index, app ->
                val isBeingDragged = draggedIndex == index

                // Calculate where dragged item is hovering
                val currentShift = if (isRtl) {
                    (-dragOffsetX / slotWidth).roundToInt()
                } else {
                    (dragOffsetX / slotWidth).roundToInt()
                }
                val targetSlot = if (draggedIndex != null) {
                    (draggedIndex!! + currentShift).coerceIn(0, apps.size - 1)
                } else {
                    index
                }

                // Dynamic live displacement for other items
                val targetDisplacement = if (draggedIndex != null && !isBeingDragged) {
                    val from = draggedIndex!!
                    val to = targetSlot
                    if (from < to && index in (from + 1)..to) {
                        if (isRtl) slotWidth else -slotWidth
                    } else if (from > to && index in to until from) {
                        if (isRtl) -slotWidth else slotWidth
                    } else {
                        0f
                    }
                } else {
                    0f
                }

                val animatedDisplacement by animateFloatAsState(
                    targetValue = targetDisplacement,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "DockDisplacementAnimation",
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            if (isBeingDragged) {
                                translationX = dragOffsetX
                                scaleX = 1.22f
                                scaleY = 1.22f
                                shadowElevation = 16f
                            } else {
                                translationX = animatedDisplacement
                            }
                        }
                        .zIndex(if (isBeingDragged) 10f else 1f)
                        .pointerInput(app.packageName, apps.size) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggedIndex = index
                                    dragOffsetX = 0f
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetX += dragAmount.x
                                },
                                onDragEnd = {
                                    val from = draggedIndex
                                    if (from != null && dockWidthPx > 0) {
                                        val shift = if (isRtl) {
                                            (-dragOffsetX / slotWidth).roundToInt()
                                        } else {
                                            (dragOffsetX / slotWidth).roundToInt()
                                        }
                                        val to = (from + shift).coerceIn(0, apps.size - 1)
                                        if (from != to) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onReorder(from, to)
                                        } else if (abs(dragOffsetX) < 12f) {
                                            // Pure long-press without dragging -> Open Action Menu
                                            onAppActionMenu(app)
                                        }
                                    }
                                    draggedIndex = null
                                    dragOffsetX = 0f
                                },
                                onDragCancel = {
                                    draggedIndex = null
                                    dragOffsetX = 0f
                                },
                            )
                        }
                        .clickable {
                            if (draggedIndex == null) {
                                onLaunchApp(app)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    AppIcon(app = app, size = 52.dp)
                }
            }
        }
    }
}

/**
 * Pinned Desktop Apps Grid with direct touch long-press & drag reordering.
 */
@Composable
fun DesktopAppsGrid(
    apps: List<AppInfo>,
    onLaunchApp: (AppInfo) -> Unit,
    onReorder: (Int, Int) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(
            items = apps,
            key = { _, app -> "${app.packageName}/${app.userSerial}" },
        ) { index, app ->
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .pointerInput(app.packageName) {
                        detectTapGestures(
                            onTap = { onLaunchApp(app) },
                            onLongPress = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAppLongClick(app)
                            },
                        )
                    }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AppIcon(app = app, size = 56.dp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * Quick Contextual Action Bottom Sheet for any app on the Home Desktop or Dock.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionBottomSheet(
    actionItem: SelectedAppAction,
    context: Context,
    onPinToDesktop: () -> Unit,
    onRemoveFromDesktop: () -> Unit,
    onMoveToDock: () -> Unit,
    onRemoveFromDock: () -> Unit,
    onOpenAppDetails: () -> Unit,
    onRequestUninstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val app = actionItem.app

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // App Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(app = app, size = 48.dp)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Context-sensitive actions
            if (actionItem.source == AppSourceLocation.DOCK) {
                ActionCard(
                    icon = Icons.Default.PushPin,
                    title = "تثبيت في الشاشة الرئيسية",
                    onClick = onPinToDesktop,
                )
                ActionCard(
                    icon = Icons.Default.Delete,
                    title = "إزالة من الشريط السفلي",
                    onClick = onRemoveFromDock,
                    isDestructive = true,
                )
            } else if (actionItem.source == AppSourceLocation.DESKTOP) {
                ActionCard(
                    icon = Icons.Default.Star,
                    title = "نقل إلى الشريط السفلي (المفضلة)",
                    onClick = onMoveToDock,
                )
                ActionCard(
                    icon = Icons.Default.Delete,
                    title = "إزالة من الشاشة الرئيسية",
                    onClick = onRemoveFromDesktop,
                    isDestructive = true,
                )
            }

            // Universal actions
            ActionCard(
                icon = Icons.Default.Info,
                title = "معلومات التطبيق",
                onClick = onOpenAppDetails,
            )

            ActionCard(
                icon = Icons.Default.Delete,
                title = "إلغاء التثبيت",
                onClick = onRequestUninstall,
                isDestructive = true,
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * Quick desktop action bottom sheet displayed when user long-presses wallpaper.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopMenuBottomSheet(
    onOpenWidgets: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "خيارات الشاشة الرئيسية",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )

            Card(
                onClick = onOpenWidgets,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "صفحة الأدوات والويدجيت (Widgets)",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Card(
                onClick = onOpenSettings,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "إعدادات المشغّل والخلفية",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Card(
                onClick = onOpenSearch,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "البحث السريع عن تطبيق",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
