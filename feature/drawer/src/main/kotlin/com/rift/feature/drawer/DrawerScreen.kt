package com.rift.feature.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rift.core.ui.AppIcon
import com.rift.data.apps.AppCategory
import com.rift.data.apps.AppInfo
import com.rift.data.settings.DrawerSortMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerScreen(
    modifier: Modifier = Modifier,
    viewModel: DrawerViewModel = hiltViewModel(),
    onOpenSearch: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedAppForMove by remember { mutableStateOf<AppInfo?>(null) }
    var showReorderSheet by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var scrollJob by remember { mutableStateOf<Job?>(null) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val categories = uiState.categories
        val pagerState = rememberPagerState(
            initialPage = categories.indexOf(uiState.selectedCategory).coerceAtLeast(0),
        ) { categories.size }

        // Sync swipe gesture on the app grid with viewmodel selected category
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
                // 1. Top Header Row with Title in the CENTER
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
                    onOpenReorder = { showReorderSheet = true },
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

                // 2. Main App Content: HorizontalPager for ultra-fast 120Hz swiping
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
                                        onLongClick = { selectedAppForMove = app },
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
                                    onLongClick = { selectedAppForMove = app },
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

            // 3. Floating Bottom Category Pill with Continuous Drag Scrubber
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
                    onOpenReorder = { showReorderSheet = true },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp),
                )
            }

            // 4. Move App to Category Bottom Sheet
            selectedAppForMove?.let { app ->
                MoveCategoryBottomSheet(
                    app = app,
                    categories = categories,
                    currentCategory = uiState.selectedCategory,
                    onSelectNewCategory = { newCat ->
                        viewModel.moveAppToCategory(app, newCat)
                        selectedAppForMove = null
                    },
                    onDismiss = { selectedAppForMove = null },
                )
            }

            // 5. Reorder Categories Bottom Sheet
            if (showReorderSheet) {
                ReorderCategoriesBottomSheet(
                    categories = categories,
                    onMoveCategory = viewModel::moveCategory,
                    onDismiss = { showReorderSheet = false },
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
 * Header at top of drawer displaying Category Title in the CENTER, flanked by Search and Options.
 */
@Composable
fun TopDrawerHeader(
    categoryTitle: String,
    onSearchClick: () -> Unit,
    onMenuClick: () -> Unit,
    menuExpanded: Boolean,
    onDismissMenu: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenReorder: () -> Unit = {},
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
                    text = { Text("ترتيب مواضع الأقسام") },
                    onClick = {
                        onDismissMenu()
                        onOpenReorder()
                    },
                )
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
 * Single app grid cell with native high-res icon and long-press support.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun DrawerAppItem(
    app: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
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
 * Floating category scrubber bar anchored at bottom of screen.
 * Supports continuous finger dragging / sliding across icons for instant snappy switching,
 * as well as long-press to open the category reordering sheet.
 */
@Composable
fun BottomCategoryScrubberPill(
    categories: List<AppCategory>,
    selectedCategory: AppCategory,
    onCategorySelect: (AppCategory) -> Unit,
    onOpenReorder: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var pillWidthPx by remember { mutableFloatStateOf(0f) }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

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
                    var hasTriggeredLongPress = false

                    fun updateCategory(x: Float) {
                        if (pillWidthPx > 0 && categories.isNotEmpty()) {
                            val fraction = (x / pillWidthPx).coerceIn(0f, 0.999f)
                            val targetIndex = if (isRtl) {
                                ((1f - fraction) * categories.size).toInt()
                            } else {
                                (fraction * categories.size).toInt()
                            }.coerceIn(0, categories.size - 1)

                            val targetCategory = categories[targetIndex]
                            onCategorySelect(targetCategory)
                        }
                    }

                    // 1. Instant selection on touch-down
                    updateCategory(down.position.x)

                    // 2. Track finger as it continuously scrubs across categories
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break

                        val dist = (change.position - down.position).getDistance()
                        val elapsed = System.currentTimeMillis() - downTime
                        if (!hasTriggeredLongPress && elapsed > 450 && dist < 30f) {
                            hasTriggeredLongPress = true
                            onOpenReorder()
                        }

                        updateCategory(change.position.x)
                        change.consume()
                    }
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
            categories.forEach { category ->
                val isSelected = category == selectedCategory

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = getCategoryIcon(category),
                        contentDescription = category.titleArabic,
                        tint = if (isSelected) {
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
 * Maps each AppCategory to clean, modern, bespoke vector icons.
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
 * Bottom Sheet allowing user to manually move an app to a different category.
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

/**
 * Bottom Sheet allowing user to reorder categories directly with instant persistence.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReorderCategoriesBottomSheet(
    categories: List<AppCategory>,
    onMoveCategory: (AppCategory, Int) -> Unit,
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
                text = "إعادة ترتيب أقسام التصنيفات",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "استخدم الأسهم لتغيير موضع أي قسم في الشريط السفلي ودرج التطبيقات:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                itemsIndexed(categories) { index, category ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = getCategoryIcon(category),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp),
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = category.titleArabic,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = { onMoveCategory(category, -1) },
                                    enabled = index > 0,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "تحريك لأعلى",
                                        tint = if (index > 0) MaterialTheme.colorScheme.primary else Color.Gray,
                                    )
                                }
                                IconButton(
                                    onClick = { onMoveCategory(category, 1) },
                                    enabled = index < categories.size - 1,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "تحريك لأسفل",
                                        tint = if (index < categories.size - 1) MaterialTheme.colorScheme.primary else Color.Gray,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
