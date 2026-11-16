package com.rift.feature.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import com.rift.data.apps.AppInfo

/**
 * Ultra-clean, distraction-free home desktop.
 * - NO top bar, NO center clock: user's wallpaper is completely visible and unobscured.
 * - Bottom dock contains ONLY the user's favorite apps with native high-res icons.
 * - Swipe UP anywhere opens the App Drawer.
 * - Swipe DOWN anywhere opens Search.
 * - Long-press on empty wallpaper opens Quick Desktop Menu (Settings, Search, Customize Dock).
 * - Long-press on any dock app opens the Dock Customization Sheet to reorder, replace, or remove apps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    onOpenDrawer: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCustomizeDockSheet by remember { mutableStateOf(false) }
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
                // Long press on desktop triggers quick menu
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
                },
        ) {
            // Desktop Content: completely unobstructed view of wallpaper
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                // Bottom Dock: ONLY the favorite apps
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = Color.Black.copy(alpha = 0.38f),
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val appsToShow = uiState.favoriteApps.take(5)
                        if (appsToShow.isNotEmpty()) {
                            appsToShow.forEach { app ->
                                FavoriteDockItem(
                                    app = app,
                                    onClick = { viewModel.launchApp(app) },
                                    onLongClick = { showCustomizeDockSheet = true },
                                )
                            }
                        } else {
                            // Empty placeholder button allowing user to pick favorites
                            TextButton(onClick = { showCustomizeDockSheet = true }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "إضافة تطبيقات للشريط",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }

            // Quick Desktop Long-Press Menu
            if (showDesktopMenuSheet) {
                DesktopMenuBottomSheet(
                    onOpenSettings = {
                        showDesktopMenuSheet = false
                        onOpenSettings()
                    },
                    onOpenSearch = {
                        showDesktopMenuSheet = false
                        onOpenSearch()
                    },
                    onOpenCustomizeDock = {
                        showDesktopMenuSheet = false
                        showCustomizeDockSheet = true
                    },
                    onDismiss = { showDesktopMenuSheet = false },
                )
            }

            // Dock Customization Bottom Sheet
            if (showCustomizeDockSheet) {
                CustomizeDockBottomSheet(
                    favoriteApps = uiState.favoriteApps,
                    allInstalledApps = uiState.allInstalledApps,
                    onMoveApp = viewModel::moveFavoriteApp,
                    onRemoveApp = viewModel::removeFavoriteApp,
                    onAddApp = viewModel::addFavoriteApp,
                    onReplaceApp = viewModel::replaceFavoriteApp,
                    onDismiss = { showCustomizeDockSheet = false },
                )
            }
        }
    }
}

/**
 * Individual favorite app button on the Home Screen dock, rendered with its real device icon.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteDockItem(
    app: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(
            app = app,
            size = 52.dp,
        )
    }
}

/**
 * Quick desktop action bottom sheet displayed when user long-presses wallpaper.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopMenuBottomSheet(
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenCustomizeDock: () -> Unit,
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
                onClick = onOpenCustomizeDock,
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
                        text = "تخصيص شريط التطبيقات المفضلة (الترتيب والإضافة)",
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

/**
 * Bottom Sheet allowing user to reorder favorite apps, replace them, or add new ones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeDockBottomSheet(
    favoriteApps: List<AppInfo>,
    allInstalledApps: List<AppInfo>,
    onMoveApp: (AppInfo, Int) -> Unit,
    onRemoveApp: (AppInfo) -> Unit,
    onAddApp: (AppInfo) -> Unit,
    onReplaceApp: (AppInfo, AppInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var appToReplace by remember { mutableStateOf<AppInfo?>(null) }
    var showAppPickerForAdd by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "تخصيص شريط المفضلة",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${favoriteApps.size} / 5 تطبيقات",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Text(
                text = "يمكنك تغيير مواضع التطبيقات باستخدام الأسهم، أو استبدالها، أو إضافتها.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )

            // Current Favorites List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(favoriteApps) { index, app ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppIcon(app = app, size = 44.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )

                            // Move Left / Right
                            IconButton(
                                onClick = { onMoveApp(app, -1) },
                                enabled = index > 0,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "تحريك لليمين",
                                    tint = if (index > 0) MaterialTheme.colorScheme.primary else Color.Gray,
                                )
                            }
                            IconButton(
                                onClick = { onMoveApp(app, 1) },
                                enabled = index < favoriteApps.size - 1,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "تحريك لليسار",
                                    tint = if (index < favoriteApps.size - 1) MaterialTheme.colorScheme.primary else Color.Gray,
                                )
                            }

                            // Replace
                            IconButton(onClick = { appToReplace = app }) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "استبدال التطبيق",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }

                            // Remove
                            IconButton(onClick = { onRemoveApp(app) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إزالة من الشريط",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add App Button
            if (favoriteApps.size < 5) {
                Button(
                    onClick = { showAppPickerForAdd = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إضافة تطبيق جديد للشريط")
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        // Sub-dialog: App Picker for Replace or Add
        val pickerTarget = appToReplace
        if (pickerTarget != null || showAppPickerForAdd) {
            val isReplacing = pickerTarget != null
            val excludedPackages = favoriteApps.map { it.packageName }
            val selectableApps = allInstalledApps.filter {
                (!excludedPackages.contains(it.packageName) || (isReplacing && it.packageName == pickerTarget?.packageName)) &&
                    it.label.contains(searchQuery, ignoreCase = true)
            }

            ModalBottomSheet(
                onDismissRequest = {
                    appToReplace = null
                    showAppPickerForAdd = false
                    searchQuery = ""
                },
                sheetState = rememberModalBottomSheetState(),
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = if (isReplacing) "اختر تطبيقاً بديلاً لـ ${pickerTarget?.label}" else "اختر تطبيقاً لإضافته للشريط",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("بحث عن تطبيق...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(selectableApps.size) { i ->
                            val app = selectableApps[i]
                            Card(
                                onClick = {
                                    if (isReplacing && pickerTarget != null) {
                                        onReplaceApp(pickerTarget, app)
                                    } else {
                                        onAddApp(app)
                                    }
                                    appToReplace = null
                                    showAppPickerForAdd = false
                                    searchQuery = ""
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    AppIcon(app = app, size = 40.dp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = app.label,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }
    }
}
