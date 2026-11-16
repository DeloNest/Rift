package com.rift.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rift.core.icons.IconPackInfo
import com.rift.core.ui.AppIcon
import com.rift.data.apps.AppInfo
import com.rift.data.settings.DarkMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onOpenSetup: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val hiddenApps by viewModel.hiddenApps.collectAsState()
    val allApps by viewModel.allApps.collectAsState()
    val installedIconPacks by viewModel.installedIconPacks.collectAsState()
    val updateUiState by viewModel.updateUiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showHiddenAppsSheet by remember { mutableStateOf(false) }
    var showIconPacksSheet by remember { mutableStateOf(false) }

    // RTL for Arabic UI
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "الإعدادات",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Section: Updates & Obtainium (First priority)
                item {
                    SettingsSectionTitle("نظام التحديثات وتطبيق Obtainium")
                }

                item {
                    UpdateCard(
                        currentVersion = viewModel.currentVersionName,
                        updateState = updateUiState,
                        autoCheckEnabled = settings.autoCheckUpdates,
                        onCheckUpdates = { viewModel.checkForUpdates() },
                        onToggleAutoCheck = { viewModel.setAutoCheckUpdates(it) },
                        onOpenObtainium = { viewModel.openInObtainium(context) },
                        onOpenGitHub = { viewModel.openGitHubReleases(context) },
                    )
                }

                // Section: Appearance
                item {
                    SettingsSectionTitle("المظهر والواجهة")
                }

                item {
                    SettingsCard {
                        // Dark Mode selector
                        SettingsRow(
                            icon = Icons.Default.DarkMode,
                            title = "الوضع الداكن",
                            subtitle = when (settings.darkMode) {
                                DarkMode.SYSTEM -> "تلقائي (حسب النظام)"
                                DarkMode.DARK -> "داكن دائماً"
                                DarkMode.LIGHT -> "فاتح دائماً"
                            },
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                ModePill(
                                    label = "تلقائي",
                                    selected = settings.darkMode == DarkMode.SYSTEM,
                                    onClick = { viewModel.setDarkMode(DarkMode.SYSTEM) },
                                )
                                ModePill(
                                    label = "داكن",
                                    selected = settings.darkMode == DarkMode.DARK,
                                    onClick = { viewModel.setDarkMode(DarkMode.DARK) },
                                )
                                ModePill(
                                    label = "فاتح",
                                    selected = settings.darkMode == DarkMode.LIGHT,
                                    onClick = { viewModel.setDarkMode(DarkMode.LIGHT) },
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Wallpaper Mode
                        SettingsRow(
                            icon = Icons.Default.Wallpaper,
                            title = "خلفية الشاشة",
                            subtitle = if (settings.useSystemWallpaper) "خلفية النظام الحالية (شفافة)" else "خلفية Rift OLED الداكنة",
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                ModePill(
                                    label = "النظام",
                                    selected = settings.useSystemWallpaper,
                                    onClick = { viewModel.setUseSystemWallpaper(true) },
                                )
                                ModePill(
                                    label = "OLED",
                                    selected = !settings.useSystemWallpaper,
                                    onClick = { viewModel.setUseSystemWallpaper(false) },
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Dynamic Color
                        SettingsSwitchRow(
                            icon = Icons.Default.ColorLens,
                            title = "الألوان الديناميكية (Material You)",
                            subtitle = "استخراج ألوان الثيم تلقائياً من خلفية شاشتك",
                            checked = settings.dynamicColor,
                            onCheckedChange = { viewModel.setDynamicColor(it) },
                        )
                    }
                }

                // Section: Icon Packs & Shapes
                item {
                    SettingsSectionTitle("حزم وشكل الأيقونات (Icon Packs & Shapes)")
                }

                item {
                    SettingsCard {
                        // Icon Shape
                        val currentShape = settings.iconShape.uppercase()
                        SettingsRow(
                            icon = Icons.Default.Apps,
                            title = "شكل الأيقونات",
                            subtitle = when (currentShape) {
                                "CIRCLE" -> "دائري بالكامل"
                                "SQUIRCLE" -> "سكويركل انسيابي"
                                "ROUNDED_SQUARE" -> "مربع بزوايا دائرية"
                                "TEARDROP" -> "شكل قطرة ماء"
                                else -> "تلقائي (حسب النظام والشركة)"
                            },
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                ModePill(
                                    label = "النظام",
                                    selected = currentShape != "CIRCLE" && currentShape != "SQUIRCLE" && currentShape != "ROUNDED_SQUARE" && currentShape != "TEARDROP",
                                    onClick = { viewModel.setIconShape("SYSTEM") },
                                )
                                ModePill(
                                    label = "دائري",
                                    selected = currentShape == "CIRCLE",
                                    onClick = { viewModel.setIconShape("CIRCLE") },
                                )
                                ModePill(
                                    label = "سكويركل",
                                    selected = currentShape == "SQUIRCLE",
                                    onClick = { viewModel.setIconShape("SQUIRCLE") },
                                )
                                ModePill(
                                    label = "قطرة",
                                    selected = currentShape == "TEARDROP",
                                    onClick = { viewModel.setIconShape("TEARDROP") },
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Icon Pack Selector
                        val activePackName = remember(settings.iconPackPackage, installedIconPacks) {
                            if (settings.iconPackPackage.isNullOrBlank()) {
                                "أيقونات النظام الافتراضية"
                            } else {
                                installedIconPacks.find { it.packageName == settings.iconPackPackage }?.name
                                    ?: settings.iconPackPackage!!
                            }
                        }

                        SettingsRow(
                            icon = Icons.Default.ColorLens,
                            title = "حزمة الأيقونات (Icon Pack)",
                            subtitle = activePackName,
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    viewModel.loadInstalledIconPacks()
                                    showIconPacksSheet = true
                                },
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("تغيير الحزمة")
                            }
                        }
                    }
                }

                // Section: App Drawer & Home
                item {
                    SettingsSectionTitle("الشاشة الرئيسية ودرج التطبيقات")
                }

                item {
                    SettingsCard {
                        SettingsRow(
                            icon = Icons.Default.Widgets,
                            title = "نمط درج التطبيقات",
                            subtitle = when (settings.drawerSortMode) {
                                com.rift.data.settings.DrawerSortMode.CATEGORIZED -> "تصنيف ذكي (Smart Launcher)"
                                com.rift.data.settings.DrawerSortMode.ALPHABETICAL -> "ترتيب أبجدي (A-Z)"
                                com.rift.data.settings.DrawerSortMode.MANUAL -> "ترتيب يدوي مخصص"
                            },
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                ModePill(
                                    label = "ذكي",
                                    selected = settings.drawerSortMode == com.rift.data.settings.DrawerSortMode.CATEGORIZED,
                                    onClick = { viewModel.setDrawerSortMode(com.rift.data.settings.DrawerSortMode.CATEGORIZED) },
                                )
                                ModePill(
                                    label = "أبجدي",
                                    selected = settings.drawerSortMode == com.rift.data.settings.DrawerSortMode.ALPHABETICAL,
                                    onClick = { viewModel.setDrawerSortMode(com.rift.data.settings.DrawerSortMode.ALPHABETICAL) },
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Hidden Apps Manager
                        SettingsRow(
                            icon = Icons.Default.VisibilityOff,
                            title = "التطبيقات المخفية",
                            subtitle = if (settings.hiddenAppPackages.isEmpty()) {
                                "لا توجد تطبيقات مخفية من الدرج"
                            } else {
                                "${settings.hiddenAppPackages.size} تطبيق مخفي من درج التطبيقات"
                            },
                        ) {
                            FilledTonalButton(
                                onClick = { showHiddenAppsSheet = true },
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("إدارة")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Double tap to sleep info
                        SettingsRow(
                            icon = Icons.Default.CheckCircle,
                            title = "النقر المزدوج للقفل (Double Tap to Sleep)",
                            subtitle = "مفعّل تلقائياً: انقر مرتين في أي مكان فارغ على الشاشة الرئيسية لقفل الشاشة فوراً مع الحفاظ على الفتح بالبصمة.",
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        SettingsRow(
                            icon = Icons.Default.Refresh,
                            title = "معالج الإعداد الأولي (Setup Wizard)",
                            subtitle = "إعادة تشغيل معالج الإعداد لاختيار التطبيقات المفضلة ونمط العرض والخلفية",
                        ) {
                            TextButton(onClick = {
                                viewModel.resetSetup()
                                onOpenSetup()
                            }) {
                                Text("بدء")
                            }
                        }
                    }
                }

                // Section: About
                item {
                    SettingsSectionTitle("حول مشغّل Rift")
                }

                item {
                    SettingsCard {
                        SettingsRow(
                            icon = Icons.Default.Security,
                            title = "الخصوصية والأمان",
                            subtitle = "مشغّل Rift مبني ليكون خاصاً بالكامل، بدون إعلانات، وبدون أي تتبع.",
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        SettingsRow(
                            icon = Icons.Default.Info,
                            title = "الإصدار الحالي",
                            subtitle = "Rift v${viewModel.currentVersionName} (Build 1) — Material 3 Expressive",
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // Update Dialogs
            when (val state = updateUiState) {
                is UpdateUiState.Available -> {
                    AlertDialog(
                        onDismissRequest = { viewModel.resetUpdateState() },
                        icon = { Icon(Icons.Default.SystemUpdate, contentDescription = null) },
                        title = { Text("يتوفر تحديث جديد: ${state.update.newVersion}") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (state.update.releaseNotes.isNotBlank()) {
                                    Text(
                                        text = "ما الجديد:",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    )
                                    Text(
                                        text = state.update.releaseNotes,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                if (state.update.apkSize > 0) {
                                    val sizeMb = "%.1f".format(state.update.apkSize / (1024.0 * 1024.0))
                                    Text(
                                        text = "حجم التحديث: $sizeMb ميجابايت",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.downloadAndInstallUpdate(state.update.downloadUrl)
                                },
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تحديث وتثبيت الآن")
                            }
                        },
                        dismissButton = {
                            Row {
                                TextButton(onClick = { viewModel.openInObtainium(context) }) {
                                    Text("عبر Obtainium")
                                }
                                TextButton(onClick = { viewModel.resetUpdateState() }) {
                                    Text("لاحقاً")
                                }
                            }
                        },
                    )
                }

                is UpdateUiState.Downloading -> {
                    AlertDialog(
                        onDismissRequest = { /* Don't dismiss while downloading */ },
                        icon = { Icon(Icons.Default.Download, contentDescription = null) },
                        title = { Text("جاري تحميل التحديث...") },
                        text = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                LinearProgressIndicator(
                                    progress = { state.progress / 100f },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Text(
                                    text = "${state.progress}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        },
                        confirmButton = {},
                    )
                }

                is UpdateUiState.UpToDate -> {
                    LaunchedEffect(state) {
                        snackbarHostState.showSnackbar("أنت تستخدم أحدث إصدار من Rift بالفعل (v${viewModel.currentVersionName})")
                        viewModel.resetUpdateState()
                    }
                }

                is UpdateUiState.Error -> {
                    AlertDialog(
                        onDismissRequest = { viewModel.resetUpdateState() },
                        title = { Text("تعذر التحقق من التحديث") },
                        text = { Text(state.message) },
                        confirmButton = {
                            Button(onClick = { viewModel.checkForUpdates() }) {
                                Text("إعادة المحاولة")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { viewModel.resetUpdateState() }) {
                                Text("إغلاق")
                            }
                        },
                    )
                }

                else -> {}
            }

            if (showHiddenAppsSheet) {
                HiddenAppsModalSheet(
                    allApps = allApps,
                    hiddenPackageNames = settings.hiddenAppPackages,
                    onHideApp = { viewModel.hideApp(it) },
                    onUnhideApp = { viewModel.unhideApp(it) },
                    onDismiss = { showHiddenAppsSheet = false },
                )
            }

            if (showIconPacksSheet) {
                IconPacksModalSheet(
                    installedPacks = installedIconPacks,
                    currentPackPackage = settings.iconPackPackage,
                    onSelectPack = { packPkg ->
                        viewModel.setIconPack(packPkg)
                        showIconPacksSheet = false
                    },
                    onOpenStore = { viewModel.openPlayStoreForIconPacks(context) },
                    onDismiss = { showIconPacksSheet = false },
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun UpdateCard(
    currentVersion: String,
    updateState: UpdateUiState,
    autoCheckEnabled: Boolean,
    onCheckUpdates: () -> Unit,
    onToggleAutoCheck: (Boolean) -> Unit,
    onOpenObtainium: () -> Unit,
    onOpenGitHub: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
        ),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Version info header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "إصدار Rift الحالي",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                            text = "v$currentVersion",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Check button
                Button(
                    onClick = onCheckUpdates,
                    enabled = updateState !is UpdateUiState.Checking && updateState !is UpdateUiState.Downloading,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    if (updateState is UpdateUiState.Checking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("جاري الفحص...")
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("فحص التحديثات")
                    }
                }
            }

            // Auto check switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "فحص تلقائي عند التشغيل",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Text(
                        text = "إشعارك فور توفر إصدار جديد على GitHub",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = autoCheckEnabled,
                    onCheckedChange = onToggleAutoCheck,
                )
            }

            // Obtainium integration card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "التحديث التلقائي عبر تطبيق Obtainium",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                    Text(
                        text = "Obtainium هو أفضل تطبيق على أندرويد لتحديث التطبيقات مباشرة من GitHub بدون Google Play مع دعم التحديث بالخلفية.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = onOpenObtainium,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("فتح في Obtainium")
                        }

                        OutlinedButton(
                            onClick = onOpenGitHub,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("صفحة GitHub")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ModePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        modifier = Modifier.height(36.dp),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HiddenAppsModalSheet(
    allApps: List<AppInfo>,
    hiddenPackageNames: List<String>,
    onHideApp: (String) -> Unit,
    onUnhideApp: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var showOnlyHidden by remember { mutableStateOf(false) }

    val hiddenSet = remember(hiddenPackageNames) { hiddenPackageNames.toSet() }

    val filteredApps = remember(allApps, hiddenSet, searchQuery, showOnlyHidden) {
        allApps.filter { app ->
            val matchesFilter = if (showOnlyHidden) hiddenSet.contains(app.packageName) else true
            val matchesSearch = searchQuery.isBlank() ||
                app.label.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }.sortedWith(
            compareByDescending<AppInfo> { hiddenSet.contains(it.packageName) }
                .thenBy { it.label.lowercase() }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "إدارة التطبيقات المخفية",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = "${hiddenPackageNames.size} تطبيق مخفي حالياً من الدرج",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("بحث عن تطبيق بالاسم أو الحزمة...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "مسح")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )

            // Filter Chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !showOnlyHidden,
                    onClick = { showOnlyHidden = false },
                    label = { Text("جميع التطبيقات (${allApps.size})") },
                )
                FilterChip(
                    selected = showOnlyHidden,
                    onClick = { showOnlyHidden = true },
                    label = { Text("المخفية فقط (${hiddenPackageNames.size})") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
            }

            // Apps List
            if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (showOnlyHidden && hiddenPackageNames.isEmpty()) {
                            "لا توجد أي تطبيقات مخفية حالياً"
                        } else {
                            "لا توجد نتائج مطابقة للبحث"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(
                        count = filteredApps.size,
                        key = { filteredApps[it].packageName },
                    ) { index ->
                        val app = filteredApps[index]
                        val isHidden = hiddenSet.contains(app.packageName)
                        HiddenAppRowItem(
                            app = app,
                            isHidden = isHidden,
                            onToggleHide = {
                                if (isHidden) {
                                    onUnhideApp(app.packageName)
                                } else {
                                    onHideApp(app.packageName)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HiddenAppRowItem(
    app: AppInfo,
    isHidden: Boolean,
    onToggleHide: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isHidden) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(app = app, size = 42.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Text(
                        text = if (isHidden) "مخفي من درج التطبيقات" else app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isHidden) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (isHidden) {
                Button(
                    onClick = onToggleHide,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إظهار")
                }
            } else {
                OutlinedButton(
                    onClick = onToggleHide,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إخفاء")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IconPacksModalSheet(
    installedPacks: List<IconPackInfo>,
    currentPackPackage: String?,
    onSelectPack: (String?) -> Unit,
    onOpenStore: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "اختيار حزمة الأيقونات (Icon Pack)",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = "اختر من الحزم المثبتة على جهازك أو قم بتثبيت حزم جديدة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Item 1: System Default
                item {
                    val isSystemSelected = currentPackPackage.isNullOrBlank()
                    Surface(
                        onClick = { onSelectPack(null) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSystemSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Apps,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "أيقونات النظام الافتراضية",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    )
                                    Text(
                                        text = "الأيقونات الأصلية المدمجة في نظام جهازك",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (isSystemSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "محدد",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }

                if (installedPacks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "لم يتم العثور على أي حزم أيقونات خارجية مثبتة.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    // Installed Icon Packs
                    items(
                        count = installedPacks.size,
                        key = { installedPacks[it].packageName },
                    ) { index ->
                        val pack = installedPacks[index]
                        val isSelected = currentPackPackage == pack.packageName
                        Surface(
                            onClick = { onSelectPack(pack.packageName) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    val packIcon = pack.icon
                                    if (packIcon != null) {
                                        androidx.compose.foundation.Image(
                                            bitmap = packIcon.asImageBitmap(),
                                            contentDescription = pack.name,
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(12.dp)),
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ColorLens,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = pack.name,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        )
                                        Text(
                                            text = pack.packageName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "محدد",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom action: Download icon packs from store
            Button(
                onClick = onOpenStore,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("تثبيت حزم أيقونات إضافية من Google Play")
            }
        }
    }
}
