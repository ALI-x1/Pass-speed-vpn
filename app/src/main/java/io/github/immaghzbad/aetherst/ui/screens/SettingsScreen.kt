package io.github.immaghzbad.aetherst.ui.screens

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import io.github.immaghzbad.aetherst.R
import io.github.immaghzbad.aetherst.core.NetworkUtils
import io.github.immaghzbad.aetherst.model.*

// تابع کمکی برای تغییر زبان برنامه‌ی اندروید
fun setAppLanguage(languageCode: String) {
    val localeList = if (languageCode.isEmpty()) {
        LocaleListCompat.getEmptyLocaleList()
    } else {
        LocaleListCompat.forLanguageTags(languageCode)
    }
    AppCompatDelegate.setApplicationLocales(localeList)
}

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
    val focusManager = LocalFocusManager.current
    var searchQuery by remember { mutableStateOf("") }
    
    // وضعیت‌های باز و بسته بودن منوهای فشرده (پاپ‌آپ‌ها)
    var showProfilesSheet by remember { mutableStateOf(false) }
    var showConnectionSheet by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }

    // اتصال رنگ‌ها به تم اصلی متریال دیزاین
    val bgColor = MaterialTheme.colorScheme.background
    val cardBg = MaterialTheme.colorScheme.surface
    val groupBg = MaterialTheme.colorScheme.surfaceVariant
    val primaryText = MaterialTheme.colorScheme.onBackground
    val secondaryText = MaterialTheme.colorScheme.onSurfaceVariant
    val dividerColor = MaterialTheme.colorScheme.outlineVariant
    val activeColor = MaterialTheme.colorScheme.primary

    // نمایش زبان فعلی انتخاب شده
    val currentLocale = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val currentLanguageDisplay = when {
        currentLocale.startsWith("fa") -> "فارسی"
        currentLocale.startsWith("en") -> "English"
        else -> stringResource(R.string.language_system)
    }

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

            // بخش تم‌ها و زبان (APPEARANCE & LANGUAGE)
            if (searchQuery.isEmpty() || "Themes Appearance Language زبان".contains(searchQuery, ignoreCase = true)) {
                item {
                    IosSectionHeader(title = "APPEARANCE & LANGUAGE", scaleFactor = scaleFactor, color = secondaryText)
                    IosGroupCard(cardBg = cardBg) {
                        Column {
                            // ۱. گزینه تم‌ها
                            IosPresetItem(
                                icon = Icons.Default.Palette, iconBg = Color(0xFF007AFF),
                                title = "Themes", subtitle = "Customize application appearance",
                                isActive = false, onClick = onOpenThemes, scaleFactor = scaleFactor,
                                textColor = primaryText, subTextColor = secondaryText
                            )

                            HorizontalDivider(color = dividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 64.dp))

                            // ۲. گزینه زبان (دقیقاً زیر تم‌ها)
                            IosPresetItem(
                                icon = Icons.Default.Language, iconBg = Color(0xFFFF2D55),
                                title = stringResource(R.string.language_title), subtitle = currentLanguageDisplay,
                                isActive = false, onClick = { showLanguageSheet = true }, scaleFactor = scaleFactor,
                                textColor = primaryText, subTextColor = secondaryText
                            )
                        }
                    }
                }
            }

            // گزینه‌های پیکربندی (CONFIGURATION)
            if (searchQuery.isEmpty() || "Preset Profiles Custom Manual Tweaks Connection".contains(searchQuery, ignoreCase = true)) {
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
                        }
                    }
                }
            }

            // سایر گزینه‌ها (APP SETTINGS & INFO)
            if (searchQuery.isEmpty() || "Logs About".contains(searchQuery, ignoreCase = true)) {
                item {
                    IosSectionHeader(title = "APP SETTINGS & INFO", scaleFactor = scaleFactor, color = secondaryText)
                    IosGroupCard(cardBg = cardBg) {
                        Column {
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
                        text = stringResource(R.string.language_title),
                        color = primaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        textAlign = TextAlign.Center
                    )

                    // ۱. پیش‌فرض سیستم
                    IosPresetItem(
                        icon = Icons.Default.SettingsSuggest, iconBg = Color(0xFF8E8E93),
                        title = stringResource(R.string.language_system), subtitle = "Follow system settings",
                        isActive = currentLocale.isEmpty(),
                        onClick = {
                            setAppLanguage("")
                            showLanguageSheet = false
                        },
                        scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText
                    )

                    // ۲. انگلیسی
                    IosPresetItem(
                        icon = Icons.Default.Language, iconBg = Color(0xFF007AFF),
                        title = stringResource(R.string.language_english), subtitle = "English (US)",
                        isActive = currentLocale.startsWith("en"),
                        onClick = {
                            setAppLanguage("en")
                            showLanguageSheet = false
                        },
                        scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText
                    )

                    // ۳. فارسی
                    IosPresetItem(
                        icon = Icons.Default.Language, iconBg = Color(0xFF34C759),
                        title = stringResource(R.string.language_persian), subtitle = "فارسی",
                        isActive = currentLocale.startsWith("fa"),
                        onClick = {
                            setAppLanguage("fa")
                            showLanguageSheet = false
                        },
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
                        text = "Preset Profiles",
                        color = primaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        textAlign = TextAlign.Center
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

        // ====== پاپ آپ تنظیمات اتصال ======
        if (showConnectionSheet) {
            ModalBottomSheet(
                onDismissRequest = { showConnectionSheet = false },
                containerColor = bgColor,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                LazyColumn(modifier = Modifier.padding(bottom = 24.dp)) {
                    item {
                        Text(
                            text = "Connection & Routing",
                            color = primaryText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            textAlign = TextAlign.Center
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
                        }
                        item {
                            IosSwitchRow(
                                icon = Icons.Default.VerticalSplit, iconBg = Color(0xFF5856D6),
                                title = "Packet Fragmentation", subtitle = "Bypass SNI filters (H2 mode only)",
                                checked = config.h2Fragment, onCheckedChange = { onUpdateConfig(config.copy(h2Fragment = it)) },
                                testTag = "switch_fragment", scaleFactor = scaleFactor, textColor = primaryText, subTextColor = secondaryText
                            )
                        }
                        item {
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
                    }
                }
            }
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
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, color = subTextColor, fontSize = (12 * scaleFactor).sp)
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, top = 8.dp)
            ) {
                Text(
                    text = "Select $title",
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
                
                options.forEachIndexed { index, option ->
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
                            text = option,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else textColor,
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
    checked: Boolean, onCheckedChange: (Boolean) -> Unit, testTag: String, scaleFactor: Float,
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
            Text(title, color = textColor, fontSize = (15 * scaleFactor).sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, color = subTextColor, fontSize = (12 * scaleFactor).sp)
        }
        Switch(
            checked = checked,
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
                .width(80.dp)
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
