package com.rift.feature.drawer

import android.content.Context
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.rift.data.apps.AppCategory
import com.rift.data.apps.AppInfo
import com.rift.data.settings.DrawerSortMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerScreen(
    modifier: Modifier = Modifier,
    viewModel: DrawerViewModel = hiltViewModel(),
    onOpenSearch: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedAppForAction by remember { mutableStateOf<AppInfo?>(null) }
    var showCategoryPickerForApp by remember { mutableStateOf<AppInfo?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var scrollJob by remember { mutableStateOf<Job?>(null) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val categories = uiState.categories
        val pagerState = rememberPagerState(
            initialPage = categories.indexOf(uiState.selectedCategory).coerceAtLeast(0),
        ) { categories.size }

        // Sync swipe gesture on app grid with viewmodel selected category
        LaunchedEffect(pagerState.currentPage) {
            if (pagerState.currentPage in categories.indices) {
                viewModel.selectCategory(categories[pagerState.currentPage])
            }
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.58f))
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. Top Header Row with Category Title in the exact CENTER
                TopDrawerHeader(
                    categoryTitle = if (uiState.sortMode == DrawerSortMode.CATEGORIZED) {
                        uiState.selectedCategory.titleArabic
                    } else {
                        "جميع التطبيقات"
                    },
                    onSearchClick = onOpenSearch,
                    onMenuClick = { menuExpanded = true },
                    menuExpanded = menuExpanded,
                    onDismissMenu = { menuExpanded = false },
                    onOpenSettings = onOpenSettings,
                    sortMode = uiState.sortMode,
                    onToggleSortMode = {
                        val nextMode = if (uiState.sortMode == DrawerSortMode.CATEGORIZED) {
                            DrawerSortMode.ALPHABETICAL
                        } else {
                            DrawerSortMode.CATEGORIZED
                        }
                        viewModel.setSortMode(nextMode)
                    },
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 2. Main App Content: HorizontalPager for ultra-fluid 120Hz swiping
                if (uiState.sortMode == DrawerSortMode.CATEGORIZED) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) { pageIndex ->
                        val currentCategory = categories.getOrNull(pageIndex) ?: AppCategory.GENERAL
                        val apps = uiState.categorizedApps[currentCategory] ?: emptyList()

                        if (apps.isEmpty()) {
                            EmptyStateNotice(totalAppsCount = uiState.totalAppsCount)
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(
                                    items = apps,
                                    key = { "${it.packageName}/${it.userSerial}" },
                                ) { app ->
                                    DrawerAppItem(
                                        app = app,
                                        onClick = { viewModel.launchApp(app) },
                                        onLongClick = { selectedAppForAction = app },
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Alphabetical Single Grid Mode
                    if (uiState.allAlphabeticalApps.isEmpty()) {
                        EmptyStateNotice(totalAppsCount = uiState.totalAppsCount)
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(
                                items = uiState.allAlphabeticalApps,
                                key = { "${it.packageName}/${it.userSerial}" },
                            ) { app ->
                                DrawerAppItem(
                                    app = app,
                                    onClick = { viewModel.launchApp(app) },
                                    onLongClick = { selectedAppForAction = app },
                                )
                            }
                        }
                    }
                }

                // Bottom spacing for category bar
                if (uiState.sortMode == DrawerSortMode.CATEGORIZED) {
                    Spacer(modifier = Modifier.height(78.dp))
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // 3. Floating Bottom Category Pill with Live Scrubber + Direct Drag-and-Drop Reordering!
            if (uiState.sortMode == DrawerSortMode.CATEGORIZED) {
                BottomCategoryScrubberPill(
                    categories = categories,
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelect = { cat ->
                        val targetIndex = categories.indexOf(cat)
                        if (targetIndex >= 0 && targetIndex != pagerState.currentPage) {
                            viewModel.selectCategory(cat)
                            scrollJob?.cancel()
                            scrollJob = coroutineScope.launch {
                                pagerState.scrollToPage(targetIndex)
                            }
                        }
                    },
                    onReorderCategories = viewModel::reorderCategories,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp),
                )
            }

            // 4. Quick App Context Actions Bottom Sheet (Pin to Desktop, Add to Dock, Move Category, Info, Uninstall)
            selectedAppForAction?.let { app ->
                DrawerAppActionBottomSheet(
                    app = app,
                    onAddToDock = {
                        viewModel.addAppToDock(app)
                        selectedAppForAction = null
                    },
                    onAddToDesktop = {
                        viewModel.addAppToDesktop(app)
                        selectedAppForAction = null
                    },
                    onChangeCategory = {
                        showCategoryPickerForApp = app
                        selectedAppForAction = null
                    },
                    onOpenAppDetails = {
                        viewModel.openAppDetails(context, app)
                        selectedAppForAction = null
                    },
                    onRequestUninstall = {
                        viewModel.requestUninstallApp(context, app)
                        selectedAppForAction = null
                    },
                    onDismiss = { selectedAppForAction = null },
                )
            }

            // 5. Move App to Category Picker
            showCategoryPickerForApp?.let { app ->
                MoveCategoryBottomSheet(
                    app = app,
                    categories = categories,
                    currentCategory = uiState.selectedCategory,
                    onSelectNewCategory = { newCat ->
                        viewModel.moveAppToCategory(app, newCat)
                        showCategoryPickerForApp = null
                    },
                    onDismiss = { showCategoryPickerForApp = null },
                )
            }
        }
    }
}

@Composable
private fun EmptyStateNotice(totalAppsCount: Int) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (totalAppsCount == 0) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
                Text(
                    text = "جاري قراءة وتصنيف التطبيقات...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Text(
                text = "لا توجد تطبيقات في هذا التصنيف",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Top Drawer Header with Category Title centered.
 */
@Composable
fun TopDrawerHeader(
    categoryTitle: String,
    onSearchClick: () -> Unit,
    onMenuClick: () -> Unit,
    menuExpanded: Boolean,
    onDismissMenu: () -> Unit,
    onOpenSettings: () -> Unit = {},
    sortMode: DrawerSortMode = DrawerSortMode.CATEGORIZED,
    onToggleSortMode: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onSearchClick) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "بحث",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }

        Text(
            text = categoryTitle,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )

        Box {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "خيارات",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = onDismissMenu,
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            if (sortMode == DrawerSortMode.CATEGORIZED) "التبديل إلى عرض أبجدي (A-Z)"
                            else "التبديل إلى تصنيف ذكي (Smart)"
                        )
                    },
                    onClick = {
                        onDismissMenu()
                        onToggleSortMode()
                    },
                )
                DropdownMenuItem(
                    text = { Text("الإعدادات والتحديثات") },
                    onClick = {
                        onDismissMenu()
                        onOpenSettings()
                    },
                )
            }
        }
    }
}

/**
 * Single app grid item in drawer.
 */
@Composable
fun DrawerAppItem(
    app: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .pointerInput(app.packageName) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    },
                )
            }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(
            app = app,
            size = 56.dp,
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Floating Category Scrubber Pill.
 * - Live continuous finger scrubbing across icons switches categories instantly (Smart Launcher 6 style).
 * - Press & hold (> 350ms) grabs the category for direct touch Drag-and-Drop Reordering along the pill!
 */
@Composable
fun BottomCategoryScrubberPill(
    categories: List<AppCategory>,
    selectedCategory: AppCategory,
    onCategorySelect: (AppCategory) -> Unit,
    onReorderCategories: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pillWidthPx by remember { mutableFloatStateOf(0f) }
    var isReordering by remember { mutableStateOf(false) }
    var draggedCatIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val slotWidth = if (categories.isNotEmpty() && pillWidthPx > 0) pillWidthPx / categories.size else 1f

    Surface(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .height(64.dp)
            .onGloballyPositioned { coordinates ->
                pillWidthPx = coordinates.size.width.toFloat()
            }
            .pointerInput(categories, isRtl) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downTime = System.currentTimeMillis()
                    val startX = down.position.x
                    var reorderActivated = false

                    fun getCategoryIndexAt(x: Float): Int {
                        if (pillWidthPx <= 0 || categories.isEmpty()) return 0
                        val fraction = (x / pillWidthPx).coerceIn(0f, 0.999f)
                        return if (isRtl) {
                            ((1f - fraction) * categories.size).toInt()
                        } else {
                            (fraction * categories.size).toInt()
                        }.coerceIn(0, categories.size - 1)
                    }

                    val initialIndex = getCategoryIndexAt(startX)

                    // Immediate category selection on down
                    onCategorySelect(categories[initialIndex])

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break

                        val elapsed = System.currentTimeMillis() - downTime
                        val dist = abs(change.position.x - startX)

                        // If held for > 350ms with minimal movement, activate Direct Reorder Mode!
                        if (!reorderActivated && elapsed > 350 && dist < 25f) {
                            reorderActivated = true
                            isReordering = true
                            draggedCatIndex = initialIndex
                            dragOffsetX = 0f
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }

                        if (reorderActivated) {
                            dragOffsetX = change.position.x - startX
                            change.consume()
                        } else {
                            // Live scrubbing across categories!
                            val targetIndex = getCategoryIndexAt(change.position.x)
                            onCategorySelect(categories[targetIndex])
                            change.consume()
                        }
                    }

                    // On finger lift:
                    if (reorderActivated && draggedCatIndex != null) {
                        val shift = if (isRtl) {
                            (-dragOffsetX / slotWidth).roundToInt()
                        } else {
                            (dragOffsetX / slotWidth).roundToInt()
                        }
                        val targetIndex = (draggedCatIndex!! + shift).coerceIn(0, categories.size - 1)
                        if (draggedCatIndex != targetIndex) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onReorderCategories(draggedCatIndex!!, targetIndex)
                        }
                    }

                    isReordering = false
                    draggedCatIndex = null
                    dragOffsetX = 0f
                }
            },
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            categories.forEachIndexed { index, category ->
                val isSelected = category == selectedCategory
                val isBeingDragged = isReordering && draggedCatIndex == index

                // Calculate displacement for other categories while dragging
                val currentShift = if (isRtl) {
                    (-dragOffsetX / slotWidth).roundToInt()
                } else {
                    (dragOffsetX / slotWidth).roundToInt()
                }
                val targetSlot = if (draggedCatIndex != null) {
                    (draggedCatIndex!! + currentShift).coerceIn(0, categories.size - 1)
                } else {
                    index
                }

                val targetDisplacement = if (isReordering && draggedCatIndex != null && !isBeingDragged) {
                    val from = draggedCatIndex!!
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
                    label = "CategoryDisplacementAnimation",
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .graphicsLayer {
                            if (isBeingDragged) {
                                translationX = dragOffsetX
                                scaleX = 1.25f
                                scaleY = 1.25f
                                shadowElevation = 16f
                            } else {
                                translationX = animatedDisplacement
                            }
                        }
                        .zIndex(if (isBeingDragged) 10f else 1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isBeingDragged) MaterialTheme.colorScheme.primary
                            else if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = getCategoryIcon(category),
                        contentDescription = category.titleArabic,
                        tint = if (isBeingDragged) {
                            MaterialTheme.colorScheme.onPrimary
                        } else if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        },
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

/**
 * Quick contextual actions sheet for an app in the drawer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerAppActionBottomSheet(
    app: AppInfo,
    onAddToDock: () -> Unit,
    onAddToDesktop: () -> Unit,
    onChangeCategory: () -> Unit,
    onOpenAppDetails: () -> Unit,
    onRequestUninstall: () -> Unit,
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

            // Actions
            DrawerActionCard(
                icon = Icons.Default.Star,
                title = "وضع في الشريط السفلي (المفضلة)",
                onClick = onAddToDock,
            )

            DrawerActionCard(
                icon = Icons.Default.PushPin,
                title = "تثبيت في الشاشة الرئيسية",
                onClick = onAddToDesktop,
            )

            DrawerActionCard(
                icon = Icons.Default.DriveFileMove,
                title = "تغيير تصنيف التطبيق",
                onClick = onChangeCategory,
            )

            DrawerActionCard(
                icon = Icons.Default.Info,
                title = "معلومات التطبيق",
                onClick = onOpenAppDetails,
            )

            DrawerActionCard(
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
private fun DrawerActionCard(
    icon: ImageVector,
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
 * Maps each AppCategory to bespoke vector icons.
 */
fun getCategoryIcon(category: AppCategory): ImageVector = when (category) {
    AppCategory.GENERAL -> Icons.Default.Apps
    AppCategory.COMMUNICATION -> Icons.AutoMirrored.Filled.Chat
    AppCategory.INTERNET -> Icons.Default.Language
    AppCategory.GAMES -> Icons.Default.SportsEsports
    AppCategory.MEDIA -> Icons.Default.PlayArrow
    AppCategory.UTILITIES -> Icons.Default.Build
}

/**
 * Modal Bottom Sheet allowing user to manually move an app to a different category.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveCategoryBottomSheet(
    app: AppInfo,
    categories: List<AppCategory>,
    currentCategory: AppCategory,
    onSelectNewCategory: (AppCategory) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text(
                text = "نقل \"${app.label}\" إلى تصنيف آخر",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(16.dp))

            categories.forEach { category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectNewCategory(category) }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = getCategoryIcon(category),
                        contentDescription = null,
                        tint = if (category == currentCategory) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp),
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = category.titleArabic,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (category == currentCategory) FontWeight.Bold else FontWeight.Normal,
                        ),
                        color = if (category == currentCategory) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
