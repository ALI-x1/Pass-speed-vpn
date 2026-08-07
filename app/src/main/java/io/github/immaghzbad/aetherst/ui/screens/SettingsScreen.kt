package io.github.immaghzbad.aetherst.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.LocaleManagerCompat
import androidx.core.os.LocaleListCompat
import androidx.appcompat.app.AppCompatDelegate
import io.github.immaghzbad.aetherst.core.NetworkUtils
import io.github.immaghzbad.aetherst.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    config: AetherConfig,
    isBatteryOptimized: Boolean,
    onBack: () -> Unit = {},
    scrollToSection: Boolean = false,
    onSectionScrolled: () -> Unit = {},
    onUpdateConfig: (AetherConfig) -> Unit,
    onUpdateTunnelEngine: (TunnelEngine) -> Unit,
    onApplyPreset: (String) -> Unit,
    onOpenSplitTunneling: () -> Unit,
    onOpenRoutingRules: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onResetAll: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: (android.net.Uri) -> Unit,
    onOptimizeMtu: () -> Unit,
    isOptimizingMtu: Boolean = false,
    onShowToast: (String, Boolean) -> Unit = { _, _ -> },
    onOpenThemes: () -> Unit = {},
    onOpenLogs: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    bottomContentPadding: Dp = 0.dp,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var searchQuery by remember { mutableStateOf("") }
    
    // وضعیت‌های باز و بسته بودن منوهای فشرده (پاپ‌آپ‌ها)
    var showProfilesSheet by remember { mutableStateOf(false) }
    var showConnectionSheet by remember { mutableStateOf(false) }
    var showZeroTrustSheet by remember { mutableStateOf(false) }
    var showSecuritySheet by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    // توابع و متغیرهای مربوط به تغییر زبان
    val currentLocale = LocaleManagerCompat.getApplicationLocales(context).toLanguageTags()
    val currentLanguageDisplay = when {
        currentLocale.startsWith("fa") -> "فارسی"
        currentLocale.startsWith("en") -> "English"
        else -> "System Default"
    }

    fun setAppLanguage(languageCode: String) {
        val localeList = if (languageCode.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageCode)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    // اتصال رنگ‌ها به تم اصلی متریال دیزاین
    val bgColor = MaterialTheme.colorScheme.background
    val cardBg = MaterialTheme.colorScheme.surface
    val groupBg = MaterialTheme.colorScheme.surfaceVariant
    val primaryText = MaterialTheme.colorScheme.onBackground
    val secondaryText = MaterialTheme.colorScheme.onSurfaceVariant
    val dividerColor = MaterialTheme.colorScheme.outlineVariant
    val activeColor = MaterialTheme.colorScheme.primary

    val fullBackupPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent(),
    ) { uri -> uri?.let { onImportBackup(it) } }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() }
    ) {
        val screenWidth = this.maxWidth
        val scaleFactor = (screenWidth.value / 411f).coerceIn(0.7f, 1.1f)
        val horizontalPadding = 16.dp
        val lazyListState = rememberLazyListState()

        LaunchedEffect(scrollToSection) {
            if (scrollToSection) {
                lazyListState.animateScrollToItem(4)
                onSectionScrolled()
            }
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = horizontalPadding,
                top = 0.dp,
                end = horizontalPadding,
                bottom = bottomContentPadding + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy((20 * scaleFactor).dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(top = 16.dp, bottom = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size((40 * scaleFactor).dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(groupBg)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = primaryText,
                            modifier = Modifier.size((22 * scaleFactor).dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = primaryText,
                            fontSize = (28 * scaleFactor).sp,
                            lineHeight = (32 * scaleFactor).sp
                        )
                        Text(
                            text = "Configure engine protocols & transport",
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryText,
                            fontSize = (13 * scaleFactor).sp,
                        )
                    }
                }
            }

            item {
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((48 * scaleFactor).dp)
                        .background(cardBg, RoundedCornerShape(14.dp))
                        .border(1.dp, dividerColor, RoundedCornerShape(14.dp)),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = primaryText, fontSize = (15 * scaleFactor).sp),
                    singleLine = true,
                    cursorBrush = SolidColor(activeColor),
                    decorationBox = { innerTextField ->
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, null, tint = secondaryText, modifier = Modifier.size((22 * scaleFactor).dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (searchQuery.isEmpty()) {
                                    Text("Search settings...", color = secondaryText, fontSize = (15 * scaleFactor).sp)
                                }
                                innerTextField()
                            }
                        }
                    }
                )
            }

            // Appearance & Language
            if (searchQuery.isEmpty() || "Themes Appearance Language زبان".contains(searchQuery, ignoreCase = true)) {
                item {
                    IosSectionHeader(title = "APPEARANCE & LANGUAGE", scaleFactor = scaleFactor, color = secondaryText)
                    IosGroupCard(cardBg = cardBg) {
                        Column {
                            IosPresetItem(
                                icon = Icons.Default.Palette, iconBg = Color(0xFF007AFF),
                                title = "Themes", subtitle = "Customize application appearance",
                                isActive = false, onClick = onOpenThemes, scaleFactor = scaleFactor,
                                textColor = primaryText, subTextColor = secondaryText
                            )
                            HorizontalDivider(color = dividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 64.dp))
                            IosPresetItem(
                                icon = Icons.Default.Language, iconBg = Color(0xFFFF2D55),
                                title = "Language / زبان برنامه", subtitle = currentLanguageDisplay,
                                isActive = false, onClick = { showLanguageSheet = true },
                                scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText
                            )
                        }
                    }
                }
            }

            // Configuration Group
            if (searchQuery.isEmpty() || "Preset Profiles Custom Manual Tweaks Connection Engine Transport Protocol Bypass Obfuscation Speed Strategy Network Stack Whole Device Split Tunneling Domain Routing VPN Tunnel Mode SOCKS5 HTTP Host Port MTU Keepalive Peer Advanced Security Kill Switch IPv6 Leak Protection Smart Reconnect Cloudflare Zero Trust Team Access Gateway ID Secret Token".contains(searchQuery, ignoreCase = true)) {
                item {
                    IosSectionHeader(title = "CONFIGURATION", scaleFactor = scaleFactor, color = secondaryText)
                    IosGroupCard(cardBg = cardBg) {
                        Column {
                            IosPresetItem(
                                icon = Icons.Default.Tune, iconBg = Color(0xFF5856D6),
                                title = "Preset Profiles", subtitle = "Select configuration presets",
                                isActive = false, onClick = { showProfilesSheet = true },
                                scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText
                            )
                            HorizontalDivider(color = dividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 64.dp))
                            IosPresetItem(
                                icon = Icons.Default.Router, iconBg = Color(0xFFFF9500),
                                title = "Connection & Routing", subtitle = "Engine, Protocols & Transport",
                                isActive = false, onClick = { showConnectionSheet = true },
                                scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText
                            )
                            if (config.protocol == AetherProtocol.ZERO_TRUST) {
                                HorizontalDivider(color = dividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 64.dp))
                                IosPresetItem(
                                    icon = Icons.Default.Business, iconBg = Color(0xFF5856D6),
                                    title = "Cloudflare Zero Trust", subtitle = "Organization and Access settings",
                                    isActive = false, onClick = { showZeroTrustSheet = true },
                                    scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText
                                )
                            }
                            if (config.connectionMode == ConnectionMode.TUNNEL) {
                                HorizontalDivider(color = dividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 64.dp))
                                IosPresetItem(
                                    icon = Icons.Default.Security, iconBg = Color(0xFF34C759),
                                    title = "Advanced Security", subtitle = "Kill Switch & Auto-Recovery",
                                    isActive = false, onClick = { showSecuritySheet = true },
                                    scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText
                                )
                            }
                        }
                    }
                }
            }

            // Diagnostics & App Settings
            if (searchQuery.isEmpty() || "Logging System Logs App Core Level Diagnostics About".contains(searchQuery, ignoreCase = true)) {
                item {
                    IosSectionHeader(title = "APP SETTINGS & DIAGNOSTICS", scaleFactor = scaleFactor, color = secondaryText)
                    IosGroupCard(cardBg = cardBg) {
                        Column {
                            IosPickerRow(
                                icon = Icons.Default.BugReport, iconBg = Color(0xFF64D2FF),
                                title = "App System Logging", value = config.appLogLevel.displayName.substringBefore(" ("),
                                options = AetherLogLevel.entries.map { it.displayName },
                                onOptionSelected = { onUpdateConfig(config.copy(appLogLevel = AetherLogLevel.entries[it])) },
                                scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText, sheetBg = groupBg
                            )
                            HorizontalDivider(color = dividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 64.dp))
                            IosPickerRow(
                                icon = Icons.Default.VpnLock, iconBg = Color(0xFF8E8E93),
                                title = "Aether Core Logging", value = config.coreLogLevel.displayName.substringBefore(" ("),
                                options = AetherLogLevel.entries.map { it.displayName },
                                onOptionSelected = { onUpdateConfig(config.copy(coreLogLevel = AetherLogLevel.entries[it])) },
                                scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText, sheetBg = groupBg
                            )
                            HorizontalDivider(color = dividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 64.dp))
                            IosPresetItem(
                                icon = Icons.Default.Code, iconBg = Color(0xFF34C759),
                                title = "Application Logs", subtitle = "View connection and system logs",
                                isActive = false, onClick = onOpenLogs, scaleFactor = scaleFactor,
                                textColor = primaryText, subTextColor = secondaryText
                            )
                            HorizontalDivider(color = dividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 64.dp))
                            IosPresetItem(
                                icon = Icons.Default.Info, iconBg = Color(0xFF8E8E93),
                                title = "About Us", subtitle = "Version, license, and information",
                                isActive = false, onClick = onOpenAbout, scaleFactor = scaleFactor,
                                textColor = primaryText, subTextColor = secondaryText
                            )
                        }
                    }
                }
            }

            // Backup & System Maintenance
            if (searchQuery.isEmpty() || "Backup Restore Reset System Defaults".contains(searchQuery, ignoreCase = true)) {
                item {
                    IosSectionHeader(title = "BACKUP & MAINTENANCE", scaleFactor = scaleFactor, color = secondaryText)
                    IosGroupCard(cardBg = cardBg) {
                        Column {
                            IosPresetItem(
                                icon = Icons.Default.CloudUpload, iconBg = Color(0xFF5856D6),
                                title = "Full Configuration Backup", subtitle = "Export all settings to .astf file",
                                isActive = false, onClick = onExportBackup, scaleFactor = scaleFactor,
                                textColor = primaryText, subTextColor = secondaryText
                            )
                            HorizontalDivider(color = dividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 64.dp))
                            IosPresetItem(
                                icon = Icons.Default.CloudDownload, iconBg = Color(0xFF34C759),
                                title = "Restore from Backup", subtitle = "Import settings from an .astf file",
                                isActive = false, onClick = { fullBackupPicker.launch("*/*") }, scaleFactor = scaleFactor,
                                textColor = primaryText, subTextColor = secondaryText
                            )
                            HorizontalDivider(color = dividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 64.dp))
                            IosPresetItem(
                                icon = Icons.Default.DeleteForever, iconBg = Color(0xFFFF3B30),
                                title = "Reset to Factory Defaults", subtitle = "Wipe all custom tweaks and restart",
                                isActive = false, onClick = { showResetDialog = true }, scaleFactor = scaleFactor,
                                textColor = Color(0xFFFF3B30), subTextColor = secondaryText
                            )
                        }
                    }
                }
            }

            // System Stability
            item {
                IosSectionHeader(title = "SYSTEM STABILITY", scaleFactor = scaleFactor, color = secondaryText)
                IosGroupCard(cardBg = cardBg) {
                    Column {
                        IosSwitchRow(
                            icon = Icons.Default.BatteryAlert, iconBg = Color(0xFFFF3B30),
                            title = "Battery Optimization", subtitle = "Allow AetherST to run without restrictions",
                            checked = isBatteryOptimized, enabled = !isBatteryOptimized,
                            onCheckedChange = { if (it) onRequestBatteryOptimization() },
                            testTag = "switch_battery_opt", scaleFactor = scaleFactor,
                            textColor = primaryText, subTextColor = secondaryText
                        )
                    }
                }
            }
        }
        
        // ====== پاپ آپ انتخاب زبان ======
        if (showLanguageSheet) {
            ModalBottomSheet(
                onDismissRequest = { showLanguageSheet = false },
                containerColor = bgColor,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(modifier = Modifier.padding(bottom = 24.dp)) {
                    Text(
                        text = "زبان / Language", color = primaryText, fontWeight = FontWeight.Bold,
                        fontSize = 20.sp, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), textAlign = TextAlign.Center
                    )
                    IosPresetItem(
                        icon = Icons.Default.SettingsSuggest, iconBg = Color(0xFF8E8E93),
                        title = "System Default", subtitle = "زبان سیستم گوشی", isActive = currentLocale.isEmpty(),
                        onClick = { setAppLanguage(""); showLanguageSheet = false },
                        scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText
                    )
                    IosPresetItem(
                        icon = Icons.Default.Language, iconBg = Color(0xFF007AFF),
                        title = "English", subtitle = "انگلیسی", isActive = currentLocale.startsWith("en"),
                        onClick = { setAppLanguage("en"); showLanguageSheet = false },
                        scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText
                    )
                    IosPresetItem(
                        icon = Icons.Default.Language, iconBg = Color(0xFF34C759),
                        title = "فارسی", subtitle = "Persian", isActive = currentLocale.startsWith("fa"),
                        onClick = { setAppLanguage("fa"); showLanguageSheet = false },
                        scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText
                    )
                }
            }
        }

        // ====== پاپ آپ پروفایل‌ها ======
        if (showProfilesSheet) {
            ModalBottomSheet(
                onDismissRequest = { showProfilesSheet = false },
                containerColor = bgColor,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(modifier = Modifier.padding(bottom = 24.dp)) {
                    Text(
                        text = "Preset Profiles", color = primaryText, fontWeight = FontWeight.Bold,
                        fontSize = 20.sp, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), textAlign = TextAlign.Center
                    )
                    IosPresetItem(
                        icon = Icons.Default.Tune, iconBg = Color(0xFF8E8E93),
                        title = "Custom Manual Tweaks", subtitle = "Your own independent manual configuration",
                        isActive = config.presetId == "custom",
                        onClick = { onApplyPreset("custom"); showProfilesSheet = false; onShowToast("Applied manual configuration", false) },
                        scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText
                    )
                    IosPresetItem(
                        icon = Icons.Default.Lock, iconBg = Color(0xFF5856D6),
                        title = "Bypass UDP / TLS", subtitle = "MASQUE + H2 Fallback + Fragmentation",
                        isActive = config.presetId == "bypass_udp",
                        onClick = { onApplyPreset("bypass_udp"); showProfilesSheet = false; onShowToast("Applied UDP/TLS Bypass", false) },
                        scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText
                    )
                    IosPresetItem(
                        icon = Icons.Default.Shield, iconBg = Color(0xFF007AFF),
                        title = "Ironclad Stealth", subtitle = "MASQUE + GFW Noise + Probe Scan",
                        isActive = config.presetId == "ironclad_stealth",
                        onClick = { onApplyPreset("ironclad_stealth"); showProfilesSheet = false; onShowToast("Applied Ironclad Stealth", false) },
                        scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText
                    )
                    IosPresetItem(
                        icon = Icons.Default.Bolt, iconBg = Color(0xFFFF9500),
                        title = "Turbo Speed", subtitle = "WireGuard + Balanced Noise + Turbo Scan",
                        isActive = config.presetId == "turbo_wg",
                        onClick = { onApplyPreset("turbo_wg"); showProfilesSheet = false; onShowToast("Applied Turbo Speed", false) },
                        scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText
                    )
                }
            }
        }

        // ====== پاپ آپ تنظیمات اتصال (کامل شده با قابلیت‌های فایل قبلی) ======
        if (showConnectionSheet) {
            ModalBottomSheet(
                onDismissRequest = { showConnectionSheet = false },
                containerColor = bgColor,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                LazyColumn(modifier = Modifier.padding(bottom = 24.dp)) {
                    item {
                        Text(
                            text = "Connection & Routing", color = primaryText, fontWeight = FontWeight.Bold,
                            fontSize = 20.sp, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), textAlign = TextAlign.Center
                        )
                    }
                    item {
                        IosPickerRow(
                            icon = Icons.Default.VpnLock, iconBg = Color(0xFF34C759),
                            title = "Connection Mode", value = if (config.connectionMode == ConnectionMode.TUNNEL) "Tunnel" else "Proxy Only",
                            options = listOf("Tunnel", "Proxy Only"),
                            onOptionSelected = { index -> onUpdateConfig(config.copy(connectionMode = if (index == 0) ConnectionMode.TUNNEL else ConnectionMode.PROXY_ONLY)) },
                            scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText, sheetBg = groupBg
                        )
                    }
                    if (config.connectionMode == ConnectionMode.TUNNEL) {
                        item {
                            IosPickerRow(
                                icon = Icons.Default.VpnLock, iconBg = Color(0xFF5856D6),
                                title = "Tunnel Engine", value = config.tunnelEngine.displayName,
                                options = TunnelEngine.entries.map { it.displayName },
                                onOptionSelected = { index -> onUpdateTunnelEngine(TunnelEngine.entries[index]) },
                                scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText, sheetBg = groupBg
                            )
                        }
                    }
                    item {
                        IosPickerRow(
                            icon = Icons.Default.VpnLock, iconBg = Color(0xFF007AFF),
                            title = "Transport Protocol", value = config.protocol.displayName,
                            options = AetherProtocol.entries.map { it.displayName },
                            onOptionSelected = { index -> onUpdateConfig(config.copy(protocol = AetherProtocol.entries[index])) },
                            scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText, sheetBg = groupBg
                        )
                    }
                    if (config.protocol == AetherProtocol.MASQUE) {
                        item {
                            IosSwitchRow(
                                icon = Icons.Default.Http, iconBg = Color(0xFF007AFF),
                                title = "HTTP/2 Fallback Mode", subtitle = "Force MASQUE over TCP/TLS instead of QUIC",
                                checked = config.h2Mode, onCheckedChange = { onUpdateConfig(config.copy(h2Mode = it)) },
                                testTag = "switch_h2_mode", scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText
                            )
                            IosSwitchRow(
                                icon = Icons.Default.VerticalSplit, iconBg = Color(0xFF5856D6),
                                title = "Packet Fragmentation", subtitle = "Bypass SNI filters (H2 mode only)",
                                checked = config.h2Fragment, onCheckedChange = { onUpdateConfig(config.copy(h2Fragment = it)) },
                                testTag = "switch_fragment", scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText
                            )
                            AnimatedVisibility(
                                visible = config.h2Fragment,
                                enter = expandVertically(animationSpec = tween(300)),
                                exit = shrinkVertically(animationSpec = tween(300))
                            ) {
                                Column(modifier = Modifier.background(groupBg.copy(alpha = 0.3f))) {
                                    IosInputFieldRow(
                                        icon = Icons.Default.Straighten, iconBg = Color(0xFF8E8E93),
                                        label = "Fragment Size", value = config.fragmentSize,
                                        onValueChange = { onUpdateConfig(config.copy(fragmentSize = it)) },
                                        placeholder = "16-32", testTag = "fragment_size_input", scaleFactor = scaleFactor,
                                        textColor = primaryText, subTextColor = secondaryText, inputBg = cardBg
                                    )
                                    IosInputFieldRow(
                                        icon = Icons.Default.Timer, iconBg = Color(0xFF8E8E93),
                                        label = "Fragment Delay (ms)", value = config.fragmentDelay,
                                        onValueChange = { onUpdateConfig(config.copy(fragmentDelay = it)) },
                                        placeholder = "2-10", testTag = "fragment_delay_input", scaleFactor = scaleFactor,
                                        textColor = primaryText, subTextColor = secondaryText, inputBg = cardBg
                                    )
                                }
                            }
                        }
                    }
                    item {
                        IosSwitchRow(
                            icon = Icons.AutoMirrored.Filled.Rule, iconBg = Color(0xFF8E8E93),
                            title = "Skip Data Plane Check", subtitle = "Trust gateway after handshake only",
                            checked = config.noDataCheck, onCheckedChange = { onUpdateConfig(config.copy(noDataCheck = it)) },
                            testTag = "switch_no_data_check", scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText
                        )
                        IosSwitchRow(
                            icon = Icons.Default.FlashOn, iconBg = Color(0xFFFF9500),
                            title = "Quick Gateway Reconnect", subtitle = "Reuse last working endpoint on start",
                            checked = config.quickReconnect, onCheckedChange = { onUpdateConfig(config.copy(quickReconnect = it)) },
                            testTag = "switch_quick_reconnect", scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText
                        )
                        IosSwitchRow(
                            icon = Icons.Default.Refresh, iconBg = Color(0xFF8E8E93),
                            title = "No Profile Retry", subtitle = "Do not fallback to other noise profiles",
                            checked = config.noProfileRetry, onCheckedChange = { onUpdateConfig(config.copy(noProfileRetry = it)) },
                            testTag = "switch_no_profile_retry", scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText
                        )
                    }
                    item {
                        val availableNoise = if (config.protocol == AetherProtocol.MASQUE) {
                            listOf(AetherNoise.FIREWALL, AetherNoise.GFW, AetherNoise.OFF)
                        } else {
                            listOf(AetherNoise.BALANCED, AetherNoise.AGGRESSIVE, AetherNoise.LIGHT, AetherNoise.OFF)
                        }
                        IosPickerRow(
                            icon = Icons.Default.Tune, iconBg = Color(0xFFAF52DE),
                            title = "Bypass Obfuscation", value = config.noise.displayName.substringBefore(" ("),
                            options = availableNoise.map { it.displayName },
                            onOptionSelected = { index -> onUpdateConfig(config.copy(noise = availableNoise[index])) },
                            scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText, sheetBg = groupBg
                        )
                        IosPickerRow(
                            icon = Icons.Default.NetworkCheck, iconBg = Color(0xFFFF9500),
                            title = "Speed Strategy", value = config.scanMode.name.lowercase().replaceFirstChar { it.uppercase() },
                            options = AetherScanMode.entries.map { mode -> "${mode.name.lowercase().replaceFirstChar { it.uppercase() }} (${mode.description})" },
                            onOptionSelected = { index -> onUpdateConfig(config.copy(scanMode = AetherScanMode.entries[index])) },
                            scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText, sheetBg = groupBg
                        )
                        IosPickerRow(
                            icon = Icons.AutoMirrored.Filled.AltRoute, iconBg = Color(0xFF5856D6),
                            title = "Network Stack", value = config.ipMode.rawValue,
                            options = AetherIpMode.entries.map { it.displayName },
                            onOptionSelected = { index -> onUpdateConfig(config.copy(ipMode = AetherIpMode.entries[index])) },
                            scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText, sheetBg = groupBg
                        )
                    }
                    if (config.connectionMode == ConnectionMode.TUNNEL) {
                        item {
                            IosSwitchRow(
                                icon = Icons.Default.AllInclusive, iconBg = Color(0xFF007AFF),
                                title = "Tunnel Whole Device", subtitle = "Route all application traffic through VPN",
                                checked = config.tunnelAllApps, onCheckedChange = { onUpdateConfig(config.copy(tunnelAllApps = it)) },
                                testTag = "switch_tunnel_all", scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText
                            )
                            IosPresetItem(
                                icon = Icons.Default.Tune, iconBg = Color(0xFF5856D6),
                                title = "Split Tunneling", subtitle = if (config.tunnelAllApps) "All Apps Tunneled" else "${config.excludedPackages.size + config.blockedPackages.size} Apps",
                                isActive = false, onClick = onOpenSplitTunneling, scaleFactor = scaleFactor,
                                textColor = primaryText, subTextColor = secondaryText
                            )
                            IosPresetItem(
                                icon = Icons.AutoMirrored.Filled.AltRoute, iconBg = Color(0xFF007AFF),
                                title = "Domain & IP Routing", subtitle = "${config.routingRules.size} Rules",
                                isActive = false, onClick = onOpenRoutingRules, scaleFactor = scaleFactor,
                                textColor = primaryText, subTextColor = secondaryText
                            )
                        }
                    }
                    item {
                        IosSwitchRow(
                            icon = Icons.Default.Share, iconBg = Color(0xFFAF52DE),
                            title = "Share via Hotspot", subtitle = "Allow other devices to connect to proxy",
                            checked = config.shareHotspot, onCheckedChange = { onUpdateConfig(config.copy(shareHotspot = it)) },
                            testTag = "switch_share_hotspot", scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText
                        )
                        if (config.shareHotspot) {
                            Column(
                                modifier = Modifier.fillMaxWidth().background(groupBg.copy(alpha = 0.4f)).padding((14 * scaleFactor).dp)
                            ) {
                                var localIp by remember { mutableStateOf<String?>(null) }
                                val clipboardManager = LocalClipboardManager.current
                                LaunchedEffect(Unit) { localIp = NetworkUtils.getLocalIpAddress() }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Info, contentDescription = null,
                                            tint = if (localIp != null) activeColor else Color(0xFFFF3B30), modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (localIp != null) "LAN IP: $localIp" else "Hotspot is off",
                                            color = primaryText, fontWeight = FontWeight.Bold, fontSize = (13 * scaleFactor).sp
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                val newIp = NetworkUtils.getLocalIpAddress()
                                                localIp = newIp
                                                if (newIp != null) onShowToast("Hotspot IP detected!", false)
                                                else onShowToast("Hotspot is off. Please enable it and test again.", true)
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, null, tint = activeColor, modifier = Modifier.size(18.dp))
                                        }
                                        if (localIp != null) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(localIp!!))
                                                    onShowToast("IP address copied!", false)
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, null, tint = secondaryText, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (localIp != null) "Devices on your Hotspot can use this IP with the ports below." else "Please enable your Hotspot and tap the refresh button.",
                                    color = secondaryText, fontSize = (11 * scaleFactor).sp, lineHeight = 16.sp
                                )
                            }
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = (16 * scaleFactor).dp, vertical = (12 * scaleFactor).dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size((34 * scaleFactor).dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF007AFF)),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.Language, null, tint = Color.White, modifier = Modifier.size((20 * scaleFactor).dp)) }
                            Spacer(modifier = Modifier.width((12 * scaleFactor).dp))
                            IosInputField(
                                label = "SOCKS5 Host", value = config.socksHost,
                                onValueChange = { onUpdateConfig(config.copy(socksHost = it)) },
                                modifier = Modifier.weight(1f), placeholder = "127.0.0.1", testTag = "socks_host_input", scaleFactor = scaleFactor,
                                textColor = primaryText, subTextColor = secondaryText, inputBg = cardBg
                            )
                            Spacer(modifier = Modifier.width((10 * scaleFactor).dp))
                            IosInputField(
                                label = "SOCKS Port", value = config.socksPort,
                                onValueChange = { onUpdateConfig(config.copy(socksPort = it)) },
                                modifier = Modifier.width((75 * scaleFactor).dp), placeholder = "1819", keyboardType = KeyboardType.Number,
                                testTag = "socks_port_input", scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText, inputBg = cardBg
                            )
                            Spacer(modifier = Modifier.width((8 * scaleFactor).dp))
                            IosInputField(
                                label = "HTTP Port", value = config.httpPort,
                                onValueChange = { onUpdateConfig(config.copy(httpPort = it)) },
                                modifier = Modifier.width((75 * scaleFactor).dp), placeholder = "1820", keyboardType = KeyboardType.Number,
                                testTag = "http_port_input", scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText, inputBg = cardBg
                            )
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = (16 * scaleFactor).dp, vertical = (12 * scaleFactor).dp),
                            verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy((10 * scaleFactor).dp)
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size((34 * scaleFactor).dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF34C759)),
                                    contentAlignment = Alignment.Center
                                ) { Icon(Icons.Default.Tune, null, tint = Color.White, modifier = Modifier.size((20 * scaleFactor).dp)) }
                                Spacer(modifier = Modifier.width((12 * scaleFactor).dp))
                                IosInputField(
                                    label = "Custom MTU Size", value = config.mtu.toString(),
                                    onValueChange = { onUpdateConfig(config.copy(mtu = it.toIntOrNull() ?: 1100)) },
                                    modifier = Modifier.weight(1f), placeholder = "1100", keyboardType = KeyboardType.Number,
                                    testTag = "mtu_input", scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText, inputBg = cardBg
                                )
                            }
                            Button(
                                onClick = onOptimizeMtu, enabled = !isOptimizingMtu,
                                modifier = Modifier.height((46 * scaleFactor).dp), shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = activeColor.copy(alpha = 0.15f), contentColor = activeColor,
                                    disabledContainerColor = activeColor.copy(alpha = 0.05f), disabledContentColor = activeColor.copy(alpha = 0.3f)
                                ),
                                contentPadding = PaddingValues(horizontal = (16 * scaleFactor).dp)
                            ) {
                                if (isOptimizingMtu) {
                                    CircularProgressIndicator(modifier = Modifier.size((18 * scaleFactor).dp), color = activeColor, strokeWidth = 2.dp)
                                } else {
                                    Text("Optimize", fontSize = (13 * scaleFactor).sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = (16 * scaleFactor).dp, vertical = (12 * scaleFactor).dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size((34 * scaleFactor).dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFFF9500)),
                                    contentAlignment = Alignment.Center
                                ) { Icon(Icons.Default.Bolt, null, tint = Color.White, modifier = Modifier.size((20 * scaleFactor).dp)) }
                                Spacer(modifier = Modifier.width((12 * scaleFactor).dp))
                                IosInputField(
                                    label = "Keepalive (Secs)", value = config.keepalive.toString(),
                                    onValueChange = { onUpdateConfig(config.copy(keepalive = it.toIntOrNull() ?: 5)) },
                                    placeholder = "5", keyboardType = KeyboardType.Number, testTag = "keepalive_input",
                                    scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText, inputBg = cardBg
                                )
                            }
                            Spacer(modifier = Modifier.width((12 * scaleFactor).dp))
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                IosInputField(
                                    label = "Validation (Secs)", value = config.validateSecs.toString(),
                                    onValueChange = { onUpdateConfig(config.copy(validateSecs = it.toIntOrNull() ?: 10)) },
                                    placeholder = "10", keyboardType = KeyboardType.Number, testTag = "validate_secs_input",
                                    scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText, inputBg = cardBg
                                )
                            }
                        }
                        IosInputFieldRow(
                            icon = Icons.Default.Code, iconBg = Color(0xFF8E8E93),
                            label = "TLS Key Groups", value = config.tlsGroups,
                            onValueChange = { onUpdateConfig(config.copy(tlsGroups = it)) },
                            placeholder = "P-256:X25519:P-384", testTag = "tls_groups_input", scaleFactor = scaleFactor,
                            textColor = primaryText, subTextColor = secondaryText, inputBg = cardBg
                        )
                        if (config.connectionMode == ConnectionMode.TUNNEL) {
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = (16 * scaleFactor).dp, vertical = (12 * scaleFactor).dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size((34 * scaleFactor).dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF007AFF)),
                                        contentAlignment = Alignment.Center
                                    ) { Icon(Icons.Default.Dns, null, tint = Color.White, modifier = Modifier.size((20 * scaleFactor).dp)) }
                                    Spacer(modifier = Modifier.width((12 * scaleFactor).dp))
                                    IosInputField(
                                        label = "Tunnel DNS Servers", value = config.dnsList,
                                        onValueChange = { onUpdateConfig(config.copy(dnsList = it.replace(Regex("\\s*,\\s*"), ","))) },
                                        modifier = Modifier.weight(1f), placeholder = "1.1.1.1,1.0.0.1", testTag = "dns_list_input", scaleFactor = scaleFactor,
                                        textColor = primaryText, subTextColor = secondaryText, inputBg = cardBg
                                    )
                                }
                                Text(
                                    text = "Separate multiple DNS IPs with a comma (no spaces).",
                                    style = MaterialTheme.typography.bodySmall, color = secondaryText.copy(alpha = 0.8f),
                                    fontSize = (10 * scaleFactor).sp, modifier = Modifier.padding(start = (46 * scaleFactor).dp, top = 4.dp)
                                )
                            }
                        }
                        IosInputFieldRow(
                            icon = Icons.AutoMirrored.Filled.AltRoute, iconBg = Color(0xFF5856D6),
                            label = "Forced Peer IP", value = config.peer,
                            onValueChange = { onUpdateConfig(config.copy(peer = it)) },
                            placeholder = "e.g. 1.2.3.4:443", testTag = "peer_input", scaleFactor = scaleFactor,
                            textColor = primaryText, subTextColor = secondaryText, inputBg = cardBg
                        )
                    }
                }
            }
        }

        // ====== پاپ آپ Zero Trust ======
        if (showZeroTrustSheet) {
            ModalBottomSheet(
                onDismissRequest = { showZeroTrustSheet = false },
                containerColor = bgColor,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                LazyColumn(modifier = Modifier.padding(bottom = 24.dp)) {
                    item {
                        Text(
                            text = "Cloudflare Zero Trust", color = primaryText, fontWeight = FontWeight.Bold,
                            fontSize = 20.sp, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), textAlign = TextAlign.Center
                        )
                    }
                    item {
                        IosInputFieldRow(
                            icon = Icons.Default.Business, iconBg = Color(0xFF5856D6),
                            label = "Organization Team Name", value = config.teamName,
                            onValueChange = { onUpdateConfig(config.copy(teamName = it)) },
                            placeholder = "e.g. my-org", testTag = "zt_team_input", scaleFactor = scaleFactor,
                            textColor = primaryText, subTextColor = secondaryText, inputBg = cardBg
                        )
                        IosInputFieldRow(
                            icon = Icons.Default.Language, iconBg = Color(0xFF007AFF),
                            label = "Cloudflare Access Email", value = config.accessEmail,
                            onValueChange = { onUpdateConfig(config.copy(accessEmail = it)) },
                            placeholder = "user@example.com", testTag = "zt_email_input", scaleFactor = scaleFactor,
                            textColor = primaryText, subTextColor = secondaryText, inputBg = cardBg
                        )
                        IosSwitchRow(
                            icon = Icons.Default.Shield, iconBg = Color(0xFF34C759),
                            title = "Gateway Filtering Proxy", subtitle = "Route via org Gateway for filtering & logs",
                            checked = config.useGateway, onCheckedChange = { onUpdateConfig(config.copy(useGateway = it)) },
                            testTag = "switch_zt_gateway", scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText
                        )
                        
                        var showAdvancedZT by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAdvancedZT = !showAdvancedZT }
                                .padding(horizontal = (16 * scaleFactor).dp, vertical = (14 * scaleFactor).dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size((34 * scaleFactor).dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF8E8E93)),
                                    contentAlignment = Alignment.Center
                                ) { Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size((20 * scaleFactor).dp)) }
                                Spacer(modifier = Modifier.width((14 * scaleFactor).dp))
                                Text(
                                    text = "Advanced Authentication", style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium, color = primaryText, fontSize = (15 * scaleFactor).sp
                                )
                            }
                            Icon(
                                imageVector = if (showAdvancedZT) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null, tint = secondaryText, modifier = Modifier.size((18 * scaleFactor).dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = showAdvancedZT,
                            enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().background(groupBg.copy(alpha = 0.4f)).padding((14 * scaleFactor).dp),
                                verticalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp)
                            ) {
                                IosInputField(
                                    label = "Access Client ID", value = config.accessId,
                                    onValueChange = { onUpdateConfig(config.copy(accessId = it)) },
                                    placeholder = "Required for Service Tokens", testTag = "zt_access_id", scaleFactor = scaleFactor,
                                    textColor = primaryText, subTextColor = secondaryText, inputBg = cardBg
                                )
                                IosInputField(
                                    label = "Access Client Secret", value = config.accessSecret,
                                    onValueChange = { onUpdateConfig(config.copy(accessSecret = it)) },
                                    placeholder = "Required for Service Tokens", testTag = "zt_access_secret", scaleFactor = scaleFactor,
                                    textColor = primaryText, subTextColor = secondaryText, inputBg = cardBg
                                )
                                IosInputField(
                                    label = "Manual JWT Access Token", value = config.accessToken,
                                    onValueChange = { onUpdateConfig(config.copy(accessToken = it)) },
                                    placeholder = "Optional overrides auth", testTag = "zt_access_token", scaleFactor = scaleFactor,
                                    textColor = primaryText, subTextColor = secondaryText, inputBg = cardBg
                                )
                            }
                        }
                    }
                }
            }
        }

        // ====== پاپ آپ Security ======
        if (showSecuritySheet) {
            ModalBottomSheet(
                onDismissRequest = { showSecuritySheet = false },
                containerColor = bgColor,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(modifier = Modifier.padding(bottom = 24.dp)) {
                    Text(
                        text = "Advanced Security", color = primaryText, fontWeight = FontWeight.Bold,
                        fontSize = 20.sp, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), textAlign = TextAlign.Center
                    )
                    IosSwitchRow(
                        icon = Icons.Default.VpnLock, iconBg = Color(0xFF5856D6),
                        title = "Strict Kill Switch", subtitle = "Prevent any leak even during manual stop",
                        checked = config.strictKillSwitch, onCheckedChange = { onUpdateConfig(config.copy(strictKillSwitch = it)) },
                        testTag = "switch_strict_kill_switch", scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText
                    )
                    IosSwitchRow(
                        icon = Icons.Default.Lock, iconBg = Color(0xFFFF3B30),
                        title = "Kill Switch", subtitle = "Block traffic when VPN is disconnected",
                        checked = config.killSwitch, onCheckedChange = { onUpdateConfig(config.copy(killSwitch = it)) },
                        testTag = "switch_kill_switch", scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText
                    )
                    IosSwitchRow(
                        icon = Icons.Default.Security, iconBg = Color(0xFF5856D6),
                        title = "IPv6 Leak Protection", subtitle = "Force all IPv6 traffic through tunnel",
                        checked = config.ipv6Leak, onCheckedChange = { onUpdateConfig(config.copy(ipv6Leak = it)) },
                        testTag = "switch_ipv6_leak", scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText
                    )
                    IosSwitchRow(
                        icon = Icons.Default.Restore, iconBg = Color(0xFF34C759),
                        title = "Smart Reconnect", subtitle = "Attempt auto-recovery on network failure",
                        checked = config.smartReconnect, onCheckedChange = { onUpdateConfig(config.copy(smartReconnect = it)) },
                        testTag = "switch_smart_reconnect", scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText
                    )
                    if (config.smartReconnect) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = (16 * scaleFactor).dp, vertical = (12 * scaleFactor).dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size((34 * scaleFactor).dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF8E8E93)),
                                    contentAlignment = Alignment.Center
                                ) { Icon(Icons.Default.Repeat, null, tint = Color.White, modifier = Modifier.size((20 * scaleFactor).dp)) }
                                Spacer(modifier = Modifier.width((12 * scaleFactor).dp))
                                IosInputField(
                                    label = "Max Retries", value = config.reconnectRetryLimit.toString(),
                                    onValueChange = { onUpdateConfig(config.copy(reconnectRetryLimit = it.toIntOrNull() ?: 10)) },
                                    placeholder = "10", keyboardType = KeyboardType.Number, testTag = "reconnect_limit_input",
                                    scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText, inputBg = cardBg
                                )
                            }
                            Spacer(modifier = Modifier.width((12 * scaleFactor).dp))
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                IosInputField(
                                    label = "Delay (Secs)", value = config.reconnectSecs.toString(),
                                    onValueChange = { onUpdateConfig(config.copy(reconnectSecs = it.toIntOrNull() ?: 2)) },
                                    placeholder = "2", keyboardType = KeyboardType.Number, testTag = "reconnect_secs_input",
                                    scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText, inputBg = cardBg
                                )
                            }
                        }
                    }
                }
            }
        }

        // Dialog for Reset
        if (showResetDialog) {
            IosConfirmationDialog(
                title = "Reset All Settings?",
                message = "This will restore all protocols, engine tweaks, and security settings to their factory defaults. This action cannot be undone.",
                confirmText = "Reset Everything",
                confirmColor = Color(0xFFFF3B30),
                onConfirm = {
                    onResetAll()
                    showResetDialog = false
                    onShowToast("System restored to defaults", false)
                },
                onDismiss = { showResetDialog = false },
                scaleFactor = scaleFactor,
                cardBg = cardBg, primaryText = primaryText, secondaryText = secondaryText
            )
        }
    }
}

@Composable
private fun IosSectionHeader(title: String, scaleFactor: Float, color: Color) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        fontSize = (13 * scaleFactor).sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp, top = 8.dp)
    )
}

@Composable
private fun IosGroupCard(cardBg: Color, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        shadowElevation = 4.dp
    ) {
        content()
    }
}

@Composable
private fun IosPresetItem(
    icon: ImageVector, iconBg: Color, title: String, subtitle: String,
    isActive: Boolean, onClick: () -> Unit, scaleFactor: Float,
    textColor: Color, subTextColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size((34 * scaleFactor).dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size((20 * scaleFactor).dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = textColor, fontSize = (15 * scaleFactor).sp, fontWeight = FontWeight.SemiBold)
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, color = subTextColor, fontSize = (12 * scaleFactor).sp)
            }
        }
        if (isActive) {
            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size((22 * scaleFactor).dp))
        } else {
            Icon(Icons.Default.ChevronRight, null, tint = subTextColor.copy(alpha = 0.5f), modifier = Modifier.size((18 * scaleFactor).dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IosPickerRow(
    icon: ImageVector, iconBg: Color, title: String, value: String,
    options: List<String>, onOptionSelected: (Int) -> Unit, scaleFactor: Float,
    textColor: Color, subTextColor: Color, sheetBg: Color
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showSheet = true }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size((34 * scaleFactor).dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size((20 * scaleFactor).dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(title, color = textColor, fontSize = (15 * scaleFactor).sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(value, color = subTextColor, fontSize = (14 * scaleFactor).sp)
        Spacer(modifier = Modifier.width(6.dp))
        Icon(Icons.Default.ExpandMore, null, tint = subTextColor, modifier = Modifier.size((18 * scaleFactor).dp))
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = sheetBg
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, top = 8.dp)
            ) {
                item {
                    Text(
                        text = "Select $title", color = textColor, fontWeight = FontWeight.Bold,
                        fontSize = 18.sp, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), textAlign = TextAlign.Center
                    )
                }
                items(options.size) { index ->
                    val option = options[index]
                    val isSelected = value == option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOptionSelected(index)
                                showSheet = false
                            }
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = option, color = if (isSelected) MaterialTheme.colorScheme.primary else textColor,
                            fontSize = 16.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IosSwitchRow(
    icon: ImageVector, iconBg: Color, title: String, subtitle: String,
    checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit, testTag: String, scaleFactor: Float,
    textColor: Color, subTextColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size((34 * scaleFactor).dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size((20 * scaleFactor).dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = textColor.copy(alpha = if (enabled) 1f else 0.5f), fontSize = (15 * scaleFactor).sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, color = subTextColor.copy(alpha = if (enabled) 1f else 0.5f), fontSize = (12 * scaleFactor).sp)
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun IosInputFieldRow(
    icon: ImageVector, iconBg: Color, label: String, value: String,
    onValueChange: (String) -> Unit, placeholder: String, testTag: String, scaleFactor: Float,
    textColor: Color, subTextColor: Color, inputBg: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size((34 * scaleFactor).dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size((20 * scaleFactor).dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(label, color = textColor, fontSize = (15 * scaleFactor).sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .width(140.dp)
                .height(34.dp)
                .background(inputBg, RoundedCornerShape(8.dp))
                .border(1.dp, subTextColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .testTag(testTag),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor, fontSize = (14 * scaleFactor).sp, textAlign = TextAlign.Center),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (value.isEmpty()) {
                        Text(placeholder, color = subTextColor.copy(alpha = 0.5f), fontSize = (13 * scaleFactor).sp, textAlign = TextAlign.Center)
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun IosInputField(
    label: String, value: String, onValueChange: (String) -> Unit, testTag: String,
    modifier: Modifier = Modifier, placeholder: String = "", keyboardType: KeyboardType = KeyboardType.Text,
    scaleFactor: Float = 1f, textColor: Color, subTextColor: Color, inputBg: Color
) {
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Text(
            text = label, style = MaterialTheme.typography.labelSmall, color = subTextColor,
            fontSize = (10 * scaleFactor).sp, modifier = Modifier.padding(bottom = 2.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height((46 * scaleFactor).dp)
                .background(inputBg, RoundedCornerShape(10.dp))
                .border(
                    width = 1.dp,
                    color = if (isFocused) MaterialTheme.colorScheme.primary else subTextColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(10.dp)
                )
                .onFocusChanged { isFocused = it.isFocused }
                .testTag(testTag),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor, fontSize = (14 * scaleFactor).sp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.padding(horizontal = (12 * scaleFactor).dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(placeholder, color = subTextColor.copy(alpha = 0.5f), fontSize = (13 * scaleFactor).sp)
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
fun IosConfirmationDialog(
    title: String, message: String, confirmText: String, confirmColor: Color,
    onConfirm: () -> Unit, onDismiss: () -> Unit, scaleFactor: Float,
    cardBg: Color, primaryText: Color, secondaryText: Color
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null, onClick = onDismiss
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().clickable(enabled = false) { },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                        color = primaryText, fontSize = (18 * scaleFactor).sp, textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = message, style = MaterialTheme.typography.bodyMedium, color = secondaryText,
                        fontSize = (14 * scaleFactor).sp, textAlign = TextAlign.Center, lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height((50 * scaleFactor).dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(text = "Cancel", fontWeight = FontWeight.Medium, fontSize = (14 * scaleFactor).sp, color = primaryText)
                        }
                        Button(
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f).height((50 * scaleFactor).dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = confirmColor, contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(text = confirmText, fontWeight = FontWeight.Bold, fontSize = (14 * scaleFactor).sp)
                        }
                    }
                }
            }
        }
    }
}
