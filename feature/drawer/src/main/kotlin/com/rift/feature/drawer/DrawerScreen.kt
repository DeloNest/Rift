package com.rift.feature.drawer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Build
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rift.data.apps.AppCategory
import com.rift.data.apps.AppInfo
import com.rift.data.settings.DrawerSortMode

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
    var menuExpanded by remember { mutableStateOf(false) }

    // Enforce RTL for Arabic-first experience
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. Top Header Row with Title in the Center
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

                // 2. Main App Grid (Categorized or Alphabetical)
                if (uiState.sortMode == DrawerSortMode.CATEGORIZED) {
                    AnimatedContent(
                        targetState = uiState.selectedCategory,
                        transitionSpec = {
                            fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                                    fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        label = "CategoryAppsGrid",
                    ) { currentCategory ->
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
                    // Alphabetical Grid Mode
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

                // Space for bottom floating bar when in categorized mode
                if (uiState.sortMode == DrawerSortMode.CATEGORIZED) {
                    Spacer(modifier = Modifier.height(76.dp))
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // 3. Floating Bottom Category Pill (Only in Categorized mode, well spaced & styled)
            if (uiState.sortMode == DrawerSortMode.CATEGORIZED) {
                BottomCategoryPill(
                    categories = uiState.categories,
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelect = viewModel::selectCategory,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp),
                )
            }

            // 4. Move App to Category Bottom Sheet
            selectedAppForMove?.let { app ->
                MoveCategoryBottomSheet(
                    app = app,
                    categories = uiState.categories,
                    currentCategory = uiState.selectedCategory,
                    onSelectNewCategory = { newCat ->
                        viewModel.moveAppToCategory(app, newCat)
                        selectedAppForMove = null
                    },
                    onDismiss = { selectedAppForMove = null },
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
        // Search Button on one side
        IconButton(onClick = onSearchClick) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "بحث",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }

        // Category Title in the CENTER
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

        // Options Menu on the other side
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
 * App Icon Image Composable with system icon caching and stylized fallback.
 */
@Composable
fun AppIconImage(
    app: AppInfo,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val imageBitmap = remember(app.packageName) {
        runCatching {
            val pm = context.packageManager
            val drawable = pm.getActivityIcon(app.componentName)
            val bmp = drawable.toBitmap(width = 120, height = 120)
            bmp.asImageBitmap()
        }.getOrNull()
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = app.label,
            modifier = modifier.clip(RoundedCornerShape(16.dp)),
        )
    } else {
        Surface(
            modifier = modifier.clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = app.label.take(1),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

/**
 * Single app grid cell with real icon, label, and long-press support.
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
        AppIconImage(
            app = app,
            modifier = Modifier.size(54.dp),
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
 * Floating category pill bar anchored at bottom of screen.
 * Distinctive Smart Launcher style with generous spacing and active pill indicator.
 */
@Composable
fun BottomCategoryPill(
    categories: List<AppCategory>,
    selectedCategory: AppCategory,
    onCategorySelect: (AppCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .height(64.dp),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
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
                        )
                        .clickable { onCategorySelect(category) },
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
 * Maps each AppCategory to a clean icon matching Smart Launcher style.
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
                text = "نقل تطبيق \"${app.label}\" إلى تصنيف:",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(16.dp))

            categories.forEach { category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectNewCategory(category) }
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = getCategoryIcon(category),
                        contentDescription = category.titleArabic,
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
