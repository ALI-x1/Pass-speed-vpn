package io.github.immaghzbad.aetherst.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
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
import io.github.immaghzbad.aetherst.core.NetworkUtils
import io.github.immaghzbad.aetherst.model.*

private val IosCardBackground = Color(0xFF1C1C1E)
private val IosGroupBackground = Color(0xFF2C2C2E)
private val IosSecondaryLabel = Color(0xFF8E8E93)
private val IosActiveBlue = Color(0xFF007AFF)
private val IosDividerColor = Color(0xFF2C2C2E)
private val IosActiveSwitchGreen = Color(0xFF34C759)
private val IosInactiveSwitchTrack = Color(0xFF3A3A3C)

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
    bottomContentPadding: Dp = 0.dp,
) {
    val focusManager = LocalFocusManager.current
    var searchQuery by remember { mutableStateOf("") }
    var showAdvancedZeroTrust by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    
    val fullBackupPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent(),
    ) { uri -> uri?.let { onImportBackup(it) } }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
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
                bottom = bottomContentPadding + 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy((18 * scaleFactor).dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size((36 * scaleFactor).dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size((22 * scaleFactor).dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "AetherST Settings",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = (26 * scaleFactor).sp,
                            lineHeight = (30 * scaleFactor).sp
                        )
                        Text(
                            text = "Configure engine protocols, obfuscation & transport parameters",
                            style = MaterialTheme.typography.bodySmall,
                            color = IosSecondaryLabel,
                            fontSize = (12 * scaleFactor).sp,
                            lineHeight = (16 * scaleFactor).sp
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
                        .height((44 * scaleFactor).dp)
                        .background(IosCardBackground, RoundedCornerShape(12.dp)),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontSize = (14 * scaleFactor).sp),
                    singleLine = true,
                    cursorBrush = SolidColor(IosActiveBlue),
                    decorationBox = { innerTextField ->
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, null, tint = IosSecondaryLabel, modifier = Modifier.size((20 * scaleFactor).dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (searchQuery.isEmpty()) {
                                    Text("Search settings...", color = IosSecondaryLabel, fontSize = (13 * scaleFactor).sp)
                                }
                                innerTextField()
                            }
                        }
                    }
                )
            }

            if (searchQuery.isEmpty() || "Preset Profiles Custom Manual Tweaks Bypass UDP TLS Ironclad Stealth Turbo Speed".contains(searchQuery, ignoreCase = true)) {
                item {
                    IosSectionHeader(title = "PRESET CONFIGURATION PROFILES", scaleFactor = scaleFactor)
                    IosGroupCard {
                        Column {
                            IosPresetItem(
                                icon = Icons.Default.Tune,
                                iconBg = Color(0xFF8E8E93),
                                title = "Custom Manual Tweaks",
                                subtitle = "Your own independent manual configuration",
                                isActive = config.presetId == "custom",
                                onClick = { 
                                    onApplyPreset("custom")
                                    onShowToast("Applied manual configuration", false)
                                },
                                scaleFactor = scaleFactor
                            )
                            HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = (50 * scaleFactor).dp))
                            IosPresetItem(
                                icon = Icons.Default.Lock,
                                iconBg = Color(0xFF5856D6),
                                title = "Bypass UDP / TLS",
                                subtitle = "MASQUE + H2 Fallback + Packet Fragmentation",
                                isActive = config.presetId == "bypass_udp",
                                onClick = { 
                                    onApplyPreset("bypass_udp")
                                    onShowToast("Applied UDP/TLS Bypass preset", false)
                                },
                                scaleFactor = scaleFactor
                            )
                            HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = (50 * scaleFactor).dp))
                            IosPresetItem(
                                icon = Icons.Default.Shield,
                                iconBg = Color(0xFF007AFF),
                                title = "Ironclad Stealth",
                                subtitle = "MASQUE + GFW Noise + Ironclad Probe Scan",
                                isActive = config.presetId == "ironclad_stealth",
                                onClick = { 
                                    onApplyPreset("ironclad_stealth")
                                    onShowToast("Applied Ironclad Stealth preset", false)
                                },
                                scaleFactor = scaleFactor
                            )
                            HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = (50 * scaleFactor).dp))
                            IosPresetItem(
                                icon = Icons.Default.Bolt,
                                iconBg = Color(0xFFFF9500),
                                title = "Turbo Speed",
                                subtitle = "WireGuard + Balanced Noise + Turbo Scan",
                                isActive = config.presetId == "turbo_wg",
                                onClick = { 
                                    onApplyPreset("turbo_wg")
                                    onShowToast("Applied Turbo Speed preset", false)
                                },
                                scaleFactor = scaleFactor
                            )
                        }
                    }
                }
            }

            if (searchQuery.isEmpty() || "Engine Transport Protocol Bypass Obfuscation Speed Strategy Network Stack Whole Device Split Tunneling Domain Routing VPN Tunnel Mode SOCKS5 HTTP Host Port MTU Keepalive Peer".contains(searchQuery, ignoreCase = true)) {
                item {
                    IosSectionHeader(title = "CONNECTION & ROUTING", scaleFactor = scaleFactor)
                    IosGroupCard {
                        Column {
                            IosPickerRow(
                                icon = Icons.Default.VpnLock,
                                iconBg = Color(0xFF34C759),
                                title = "Connection Mode",
                                value = if (config.connectionMode == ConnectionMode.TUNNEL) "Tunnel" else "Proxy Only",
                                options = listOf("Tunnel", "Proxy Only"),
                                onOptionSelected = { index -> 
                                    onUpdateConfig(config.copy(connectionMode = if (index == 0) ConnectionMode.TUNNEL else ConnectionMode.PROXY_ONLY))
                                },
                                scaleFactor = scaleFactor
                            )
                            if (config.connectionMode == ConnectionMode.TUNNEL) {
                                HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = (50 * scaleFactor).dp))
                                IosPickerRow(
                                    icon = Icons.Default.VpnLock,
                                    iconBg = Color(0xFF5856D6),
                                    title = "Tunnel Engine",
                                    value = config.tunnelEngine.displayName,
                                    options = TunnelEngine.entries.map { it.displayName },
                                    onOptionSelected = { index -> onUpdateTunnelEngine(TunnelEngine.entries[index]) },
                                    scaleFactor = scaleFactor
                                )
                            }
                            HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = (50 * scaleFactor).dp))
                            IosPickerRow(
                                icon = Icons.Default.VpnLock,
                                iconBg = Color(0xFF007AFF),
                                title = "Transport Protocol",
                                value = config.protocol.displayName,
                                options = AetherProtocol.entries.map { it.displayName },
                                onOptionSelected = { index -> onUpdateConfig(config.copy(protocol = AetherProtocol.entries[index])) },
                                scaleFactor = scaleFactor
                            )
                            HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = (50 * scaleFactor).dp))
                            if (config.protocol == AetherProtocol.MASQUE) {
                                IosSwitchRow(
                                    icon = Icons.Default.Http,
                                    iconBg = Color(0xFF007AFF),
                                    title = "HTTP/2 Fallback Mode",
                                    subtitle = "Force MASQUE over TCP/TLS instead of QUIC",
                                    checked = config.h2Mode,
                                    onCheckedChange = { onUpdateConfig(config.copy(h2Mode = it)) },
                                    testTag = "switch_h2_mode",
                                    scaleFactor = scaleFactor
                                )
                                HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = (50 * scaleFactor).dp))
                                IosSwitchRow(
                                    icon = Icons.Default.VerticalSplit,
                                    iconBg = Color(0xFF5856D6),
                                    title = "Packet Fragmentation",
                                    subtitle = "Bypass SNI filters (H2 mode only)",
                                    checked = config.h2Fragment,
                                    onCheckedChange = { onUpdateConfig(config.copy(h2Fragment = it)) },
                                    testTag = "switch_fragment",
                                    scaleFactor = scaleFactor
                                )
                                AnimatedVisibility(visible = config.h2Fragment) {
                                    Column(modifier = Modifier.background(IosGroupBackground.copy(alpha = 0.3f))) {
                                        IosInputFieldRow(
                                            icon = Icons.Default.Straighten,
                                            iconBg = Color(0xFF8E8E93),
                                            label = "Fragment Size (Bytes)",
                                            value = config.fragmentSize,
                                            onValueChange = { onUpdateConfig(config.copy(fragmentSize = it)) },
                                            placeholder = "16-32",
                                            testTag = "fragment_size_input",
                                            scaleFactor = scaleFactor
                                        )
                                        HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = (50 * scaleFactor).dp))
                                        IosInputFieldRow(
                                            icon = Icons.Default.Timer,
                                            iconBg = Color(0xFF8E8E93),
                                            label = "Fragment Delay (ms)",
                                            value = config.fragmentDelay,
                                            onValueChange = { onUpdateConfig(config.copy(fragmentDelay = it)) },
                                            placeholder = "2-10",
                                            testTag = "fragment_delay_input",
                                            scaleFactor = scaleFactor
                                        )
                                    }
                                }
                                HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = (50 * scaleFactor).dp))
                            }
                            IosSwitchRow(
                                icon = Icons.AutoMirrored.Filled.Rule,
                                iconBg = Color(0xFF8E8E93),
                                title = "Skip Data Plane Check",
                                subtitle = "Trust gateway after handshake only",
                                checked = config.noDataCheck,
                                onCheckedChange = { onUpdateConfig(config.copy(noDataCheck = it)) },
                                testTag = "switch_no_data_check",
                                scaleFactor = scaleFactor
                            )
                            HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = (50 * scaleFactor).dp))
                            IosSwitchRow(
                                icon = Icons.Default.FlashOn,
                                iconBg = Color(0xFFFF9500),
                                title = "Quick Gateway Reconnect",
                                subtitle = "Reuse last working endpoint on start",
                                checked = config.quickReconnect,
                                onCheckedChange = { onUpdateConfig(config.copy(quickReconnect = it)) },
                                testTag = "switch_quick_reconnect",
                                scaleFactor = scaleFactor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IosSectionHeader(title: String, scaleFactor: Float) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = IosSecondaryLabel,
        fontSize = (12 * scaleFactor).sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
}

@Composable
private fun IosGroupCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = IosCardBackground
    ) {
        content()
    }
}

@Composable
private fun IosPresetItem(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    isActive: Boolean,
    onClick: () -> Unit,
    scaleFactor: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size((32 * scaleFactor).dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size((18 * scaleFactor).dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = (14 * scaleFactor).sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = IosSecondaryLabel, fontSize = (11 * scaleFactor).sp)
        }
        if (isActive) {
            Icon(Icons.Default.Check, null, tint = IosActiveBlue, modifier = Modifier.size((20 * scaleFactor).dp))
        }
    }
}

@Composable
private fun IosPickerRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    value: String,
    options: List<String>,
    onOptionSelected: (Int) -> Unit,
    scaleFactor: Float
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size((32 * scaleFactor).dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size((18 * scaleFactor).dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, color = Color.White, fontSize = (14 * scaleFactor).sp, modifier = Modifier.weight(1f))
            Text(value, color = IosSecondaryLabel, fontSize = (13 * scaleFactor).sp)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Default.ChevronRight, null, tint = IosSecondaryLabel, modifier = Modifier.size((16 * scaleFactor).dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        onOptionSelected(index)
                    }
                )
            }
        }
    }
}

@Composable
private fun IosSwitchRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    scaleFactor: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size((32 * scaleFactor).dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size((18 * scaleFactor).dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = (14 * scaleFactor).sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = IosSecondaryLabel, fontSize = (11 * scaleFactor).sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = IosActiveSwitchGreen,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = IosInactiveSwitchTrack
            )
        )
    }
}

@Composable
private fun IosInputFieldRow(
    icon: ImageVector,
    iconBg: Color,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    testTag: String,
    scaleFactor: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size((32 * scaleFactor).dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size((18 * scaleFactor).dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = Color.White, fontSize = (14 * scaleFactor).sp, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .width(80.dp)
                .height(32.dp)
                .background(IosGroupBackground, RoundedCornerShape(8.dp))
                .border(1.dp, IosDividerColor, RoundedCornerShape(8.dp))
                .testTag(testTag),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontSize = (13 * scaleFactor).sp, textAlign = TextAlign.Center),
            singleLine = true,
            cursorBrush = SolidColor(IosActiveBlue),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (value.isEmpty()) {
                        Text(placeholder, color = IosSecondaryLabel.copy(alpha = 0.5f), fontSize = (12 * scaleFactor).sp, textAlign = TextAlign.Center)
                    }
                    innerTextField()
                }
            }
        )
    }
}
