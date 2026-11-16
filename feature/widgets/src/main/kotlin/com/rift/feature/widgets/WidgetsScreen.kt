package com.rift.feature.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.BatteryManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rift.data.workspace.WorkspaceItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetsScreen(
    onNavigateBack: () -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WidgetsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showWidgetPicker by remember { mutableStateOf(false) }
    var selectedWidgetForAction by remember { mutableStateOf<WorkspaceItemEntity?>(null) }
    var pendingBindWidget by remember { mutableStateOf<Pair<Int, AppWidgetProviderInfo>?>(null) }

    // Launcher for system AppWidget bind permissions dialog
    val bindPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            pendingBindWidget?.let { (id, provider) ->
                viewModel.onWidgetBindPermissionGranted(id, provider)
            }
        }
        pendingBindWidget = null
    }

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
                .navigationBarsPadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White,
                        )
                    }

                    Text(
                        text = "الأدوات والويدجيت (Widgets)",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )

                    IconButton(onClick = { showWidgetPicker = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "إضافة أداة",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Canvas for Widgets
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    // 1. Built-in Smart Glance / Clock Widget
                    item(key = "built_in_clock") {
                        BuiltInSmartGlanceWidget(
                            onSearchClick = onOpenSearch,
                            context = context,
                        )
                    }

                    // 2. Placed System AppWidgets
                    itemsIndexed(
                        items = uiState.placedWidgets,
                        key = { _, item -> item.id },
                    ) { _, widgetItem ->
                        SystemWidgetCard(
                            item = widgetItem,
                            viewModel = viewModel,
                            onLongPress = { selectedWidgetForAction = widgetItem },
                        )
                    }

                    // 3. Add Widget Button Card
                    item(key = "add_widget_button") {
                        Card(
                            onClick = { showWidgetPicker = true },
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Black.copy(alpha = 0.35f)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 18.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Widgets,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "إضافة أداة ذكية أو تطبيق مصغّر (Widget)",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White,
                                )
                            }
                        }
                    }
                }
            }

            // Widget Action Sheet (Delete, Move Up, Move Down)
            selectedWidgetForAction?.let { widgetItem ->
                WidgetActionBottomSheet(
                    item = widgetItem,
                    onMoveUp = {
                        viewModel.moveWidget(widgetItem, -1)
                        selectedWidgetForAction = null
                    },
                    onMoveDown = {
                        viewModel.moveWidget(widgetItem, 1)
                        selectedWidgetForAction = null
                    },
                    onDelete = {
                        viewModel.removeWidget(widgetItem)
                        selectedWidgetForAction = null
                    },
                    onDismiss = { selectedWidgetForAction = null },
                )
            }

            // Hierarchical Grouped Widget Picker Sheet
            if (showWidgetPicker) {
                WidgetPickerBottomSheet(
                    widgetGroups = uiState.widgetGroups,
                    context = context,
                    onSelectProvider = { provider ->
                        showWidgetPicker = false
                        viewModel.addAppWidget(provider) { id, p ->
                            pendingBindWidget = Pair(id, p)
                            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, p.provider)
                            }
                            bindPermissionLauncher.launch(intent)
                        }
                    },
                    onDismiss = { showWidgetPicker = false },
                )
            }
        }
    }
}

/**
 * Built-in Modern Smart Glance Widget: Live Time, Arabic Date, Battery Indicator, Search Pill.
 */
@Composable
fun BuiltInSmartGlanceWidget(
    onSearchClick: () -> Unit,
    context: Context,
    modifier: Modifier = Modifier,
) {
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }
    var batteryPct by remember { mutableIntStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            currentTime = SimpleDateFormat("hh:mm", Locale.getDefault()).format(now)
            currentDate = SimpleDateFormat("EEEE، d MMMM", Locale.forLanguageTag("ar")).format(now)

            // Battery status query
            runCatching {
                val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                val batteryStatus = context.registerReceiver(null, ifilter)
                val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                if (level >= 0 && scale > 0) {
                    batteryPct = (level * 100 / scale)
                }
            }

            delay(1000)
        }
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.Black.copy(alpha = 0.42f),
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Row 1: Digital Time & Battery Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = currentTime,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 44.sp,
                    ),
                    color = Color.White,
                )

                // Battery Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "$batteryPct%",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            // Row 2: Live Arabic Date
            Text(
                text = currentDate,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White.copy(alpha = 0.85f),
            )

            // Row 3: Quick Search Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onSearchClick),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "بحث",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "البحث في التطبيقات أو الويب...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

/**
 * System AppWidget Container with long-press management.
 */
@Composable
fun SystemWidgetCard(
    item: WorkspaceItemEntity,
    viewModel: WidgetsViewModel,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val hostView = remember(item.appWidgetId) {
        item.appWidgetId?.let { id ->
            viewModel.widgetHostManager.obtainView(context, id)
        }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.38f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(item.id) {
                detectTapGestures(
                    onLongPress = { onLongPress() }
                )
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (hostView != null) {
                AndroidView(
                    factory = { hostView },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 340.dp),
                )
            } else {
                Text(
                    text = item.title ?: "أداة ويدجيت",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

/**
 * Bottom Sheet for Widget Action (Move Up, Move Down, Delete).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetActionBottomSheet(
    item: WorkspaceItemEntity,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
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
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "خيارات الأداة: ${item.title ?: "ويدجيت"}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )

            Card(
                onClick = onMoveUp,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(14.dp))
                    Text("تحريك لأعلى في الصفحة", style = MaterialTheme.typography.bodyLarge)
                }
            }

            Card(
                onClick = onMoveDown,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(14.dp))
                    Text("تحريك لأسفل في الصفحة", style = MaterialTheme.typography.bodyLarge)
                }
            }

            Card(
                onClick = onDelete,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(14.dp))
                    Text("إزالة الأداة من الصفحة", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * Advanced Hierarchical Widget Picker:
 * - Groups all widgets under their parent application.
 * - Tap an app to expand/collapse and reveal all of its widgets.
 * - Displays the actual visual shape/preview image of each widget!
 * - Displays exact grid dimensions (e.g. 2×2, 4×1).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPickerBottomSheet(
    widgetGroups: List<AppWidgetGroup>,
    context: Context,
    onSelectProvider: (AppWidgetProviderInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var searchQuery by remember { mutableStateOf("") }
    var expandedPackages by remember { mutableStateOf(setOf<String>()) }

    // Filter groups and widgets based on search query
    val filteredGroups = remember(widgetGroups, searchQuery) {
        if (searchQuery.isBlank()) {
            widgetGroups
        } else {
            widgetGroups.mapNotNull { group ->
                val matchesApp = group.appName.contains(searchQuery, ignoreCase = true) ||
                    group.packageName.contains(searchQuery, ignoreCase = true)
                val matchingWidgets = group.widgets.filter {
                    it.loadLabel(context.packageManager).contains(searchQuery, ignoreCase = true)
                }

                if (matchesApp || matchingWidgets.isNotEmpty()) {
                    group.copy(widgets = if (matchesApp) group.widgets else matchingWidgets)
                } else {
                    null
                }
            }
        }
    }

    // Auto-expand all matching apps during search
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            expandedPackages = filteredGroups.map { it.packageName }.toSet()
        }
    }

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
                    text = "اختر أداة لإضافتها (Widgets)",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${widgetGroups.size} تطبيق",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("بحث عن تطبيق أو ويدجيت...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredGroups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "لا توجد أدوات مطابقة للبحث",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(
                        items = filteredGroups,
                        key = { it.packageName },
                    ) { group ->
                        val isExpanded = expandedPackages.contains(group.packageName)

                        AppWidgetAccordionItem(
                            group = group,
                            isExpanded = isExpanded,
                            context = context,
                            onToggleExpand = {
                                expandedPackages = if (isExpanded) {
                                    expandedPackages - group.packageName
                                } else {
                                    expandedPackages + group.packageName
                                }
                            },
                            onSelectWidget = onSelectProvider,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

/**
 * Accordion Card for an Application grouping all its widgets.
 */
@Composable
fun AppWidgetAccordionItem(
    group: AppWidgetGroup,
    isExpanded: Boolean,
    context: Context,
    onToggleExpand: () -> Unit,
    onSelectWidget: (AppWidgetProviderInfo) -> Unit,
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "ChevronRotation",
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // App Header Row (Clickable to expand/collapse)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // App Icon
                if (group.appIcon != null) {
                    Image(
                        bitmap = group.appIcon.asImageBitmap(),
                        contentDescription = group.appName,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp)),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = group.appName,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(42.dp),
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // App Title & Widget Count
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.appName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${group.widgets.size} ${if (group.widgets.size == 1) "أداة" else if (group.widgets.size == 2) "أداتين" else "أدوات"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                // Expand Chevron Arrow
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "طي" else "توسيع",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(rotationAngle),
                )
            }

            // Expanded Sub-Widgets Section
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    group.widgets.forEach { provider ->
                        WidgetPreviewItem(
                            provider = provider,
                            context = context,
                            onSelect = { onSelectWidget(provider) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual Widget Sub-Card displaying the Widget's actual visual preview, dimensions, and title.
 */
@Composable
fun WidgetPreviewItem(
    provider: AppWidgetProviderInfo,
    context: Context,
    onSelect: () -> Unit,
) {
    var previewBitmap by remember(provider.provider) { mutableStateOf<Bitmap?>(null) }
    val label = provider.loadLabel(context.packageManager)
    val dimensions = remember(provider.provider) { calculateWidgetDimensions(provider) }

    LaunchedEffect(provider.provider) {
        withContext(Dispatchers.IO) {
            val d = runCatching { provider.loadPreviewImage(context, 0) }.getOrNull()
            if (d != null) {
                val w = d.intrinsicWidth.takeIf { it > 0 } ?: 300
                val h = d.intrinsicHeight.takeIf { it > 0 } ?: 180
                previewBitmap = runCatching { d.toBitmap(width = w.coerceAtMost(600), height = h.coerceAtMost(400)) }.getOrNull()
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onSelect),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 1. Visual Shape / Preview Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp, max = 150.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                val bmp = previewBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = label,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                    )
                } else {
                    // Fallback to provider icon in clean centered frame
                    val iconBitmap = remember(provider.provider) {
                        runCatching {
                            provider.loadIcon(context, 160)?.toBitmap(width = 72, height = 72)
                        }.getOrNull()
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(12.dp),
                    ) {
                        if (iconBitmap != null) {
                            Image(
                                bitmap = iconBitmap.asImageBitmap(),
                                contentDescription = label,
                                modifier = Modifier.size(48.dp),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Widgets,
                                contentDescription = label,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp),
                            )
                        }
                        Text(
                            text = "(معاينة الأداة)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                }
            }

            // 2. Info Row (Title, Dimensions Badge, Add Button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    // Dimensions Badge (e.g. 2 × 2)
                    Text(
                        text = "المقاس: $dimensions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Button(
                    onClick = onSelect,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "إضافة", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

/**
 * Calculates standard launcher grid dimensions (e.g. 2 × 2, 4 × 1) for a widget provider.
 */
fun calculateWidgetDimensions(provider: AppWidgetProviderInfo): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (provider.targetCellWidth > 0 && provider.targetCellHeight > 0) {
            return "${provider.targetCellWidth} × ${provider.targetCellHeight}"
        }
    }
    val cols = ((provider.minWidth + 30) / 70).coerceIn(1, 5)
    val rows = ((provider.minHeight + 30) / 70).coerceIn(1, 6)
    return "$cols × $rows"
}
