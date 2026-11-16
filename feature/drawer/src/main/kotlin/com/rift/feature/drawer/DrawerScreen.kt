package com.rift.feature.drawer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.scale
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
                // 1. Top Header Row (Matches Smart Launcher screenshot)
                TopDrawerHeader(
                    categoryTitle = uiState.selectedCategory.titleArabic,
                    onSearchClick = onOpenSearch,
                    onMenuClick = { menuExpanded = true },
                    menuExpanded = menuExpanded,
                    onDismissMenu = { menuExpanded = false },
                    onOpenSettings = onOpenSettings,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Main Categorized App Grid with AnimatedContent transition
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
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (uiState.totalAppsCount == 0) {
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

                // Space for bottom floating bar
                Spacer(modifier = Modifier.height(72.dp))
            }

            // 3. Floating Bottom Category Pill (Smart Launcher Style)
            BottomCategoryPill(
                categories = uiState.categories,
                selectedCategory = uiState.selectedCategory,
                onCategorySelect = viewModel::selectCategory,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
            )

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

/**
 * Header at top of drawer displaying Category Title, Search, and Options.
 */
@Composable
fun TopDrawerHeader(
    categoryTitle: String,
    onSearchClick: () -> Unit,
    onMenuClick: () -> Unit,
    menuExpanded: Boolean,
    onDismissMenu: () -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Category Title on the right (RTL)
        Text(
            text = categoryTitle,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )

        // Actions on the left
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "بحث",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

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
                        text = { Text("الإعدادات والتحديثات") },
                        onClick = {
                            onDismissMenu()
                            onOpenSettings()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("إعادة ترتيب التصنيفات") },
                        onClick = onDismissMenu,
                    )
                    DropdownMenuItem(
                        text = { Text("إخفاء التطبيقات") },
                        onClick = onDismissMenu,
                    )
                }
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
        // Real App Icon
        AppIconImage(
            app = app,
            modifier = Modifier.size(54.dp),
        )

        Spacer(modifier = Modifier.height(6.dp))

        // App Label
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
 * Distinctive Smart Launcher style.
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
            .padding(horizontal = 24.dp)
            .height(58.dp),
        shape = RoundedCornerShape(29.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            categories.forEach { category ->
                val isSelected = category == selectedCategory
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                    label = "CategoryScale",
                )

                Box(
                    modifier = Modifier
                        .scale(scale)
                        .clip(CircleShape)
                        .clickable { onCategorySelect(category) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = getCategoryIcon(category),
                        contentDescription = category.titleArabic,
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        },
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

/**
 * Maps each AppCategory to a clean icon matching the user's screenshot.
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
