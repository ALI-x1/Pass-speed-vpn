package io.github.immaghzbad.aetherst.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.immaghzbad.aetherst.model.AetherConfig
import io.github.immaghzbad.aetherst.model.AetherIpMode
import io.github.immaghzbad.aetherst.model.AetherLogLevel
import io.github.immaghzbad.aetherst.model.AetherNoise
import io.github.immaghzbad.aetherst.model.AetherProtocol
import io.github.immaghzbad.aetherst.model.AetherScanMode

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
    onUpdateConfig: (AetherConfig) -> Unit,
    onApplyPreset: (String) -> Unit,
    onOpenSplitTunneling: () -> Unit,
    bottomContentPadding: Dp = 0.dp
) {
    val focusManager = LocalFocusManager.current
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
        val horizontalPadding = if (screenWidth < 360.dp) 10.dp else 16.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = horizontalPadding,
                top = 12.dp,
                end = horizontalPadding,
                bottom = bottomContentPadding + 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy((18 * scaleFactor).dp)
        ) {
            item {
                Column {
                    Text(
                        text = "AetherST Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = (26 * scaleFactor).sp
                    )
                    Text(
                        text = "Configure engine protocols, obfuscation & transport parameters",
                        style = MaterialTheme.typography.bodySmall,
                        color = IosSecondaryLabel,
                        fontSize = (11 * scaleFactor).sp
                    )
                }
            }

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
                            onClick = { onApplyPreset("custom") },
                            scaleFactor = scaleFactor
                        )

                        HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = (50 * scaleFactor).dp))

                        IosPresetItem(
                            icon = Icons.Default.Lock,
                            iconBg = Color(0xFF5856D6),
                            title = "Bypass UDP / TLS",
                            subtitle = "MASQUE + H2 Fallback + Packet Fragmentation",
                            isActive = config.presetId == "bypass_udp",
                            onClick = { onApplyPreset("bypass_udp") },
                            scaleFactor = scaleFactor
                        )

                        HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = (50 * scaleFactor).dp))

                        IosPresetItem(
                            icon = Icons.Default.Shield,
                            iconBg = Color(0xFF007AFF),
                            title = "Ironclad Stealth",
                            subtitle = "MASQUE + GFW Noise + Ironclad Probe Scan",
                            isActive = config.presetId == "ironclad_stealth",
                            onClick = { onApplyPreset("ironclad_stealth") },
                            scaleFactor = scaleFactor
                        )

                        HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = (50 * scaleFactor).dp))

                        IosPresetItem(
                            icon = Icons.Default.Bolt,
                            iconBg = Color(0xFFFF9500),
                            title = "Turbo Speed",
                            subtitle = "WireGuard + Balanced Noise + Turbo Scan",
                            isActive = config.presetId == "turbo_wg",
                            onClick = { onApplyPreset("turbo_wg") },
                            scaleFactor = scaleFactor
                        )
                    }
                }
            }

            item {
                IosSectionHeader(title = "CORE TRANSPORT & ENGINE", scaleFactor = scaleFactor)
                IosGroupCard {
                    Column {
                        IosPickerRow(
                            icon = Icons.Default.VpnLock,
                            iconBg = Color(0xFF007AFF),
                            title = "Transport Protocol",
                            value = config.protocol.displayName,
                            options = AetherProtocol.entries.map { it.displayName },
                            onOptionSelected = { index ->
                                onUpdateConfig(config.copy(protocol = AetherProtocol.entries[index]))
                            },
                            scaleFactor = scaleFactor
                        )

                        HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = (50 * scaleFactor).dp))

                        val availableNoise = if (config.protocol == AetherProtocol.MASQUE) {
                            listOf(AetherNoise.FIREWALL, AetherNoise.GFW, AetherNoise.OFF)
                        } else {
                            listOf(AetherNoise.BALANCED, AetherNoise.AGGRESSIVE, AetherNoise.LIGHT, AetherNoise.OFF)
                        }

                        IosPickerRow(
                            icon = Icons.Default.Tune,
                            iconBg = Color(0xFFAF52DE),
                            title = "Noise Obfuscation",
                            value = config.noise.displayName.substringBefore(" ("),
                            options = availableNoise.map { it.displayName },
                            onOptionSelected = { index ->
                                onUpdateConfig(config.copy(noise = availableNoise[index]))
                            },
                            scaleFactor = scaleFactor
                        )

                        HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = (50 * scaleFactor).dp))

                        IosPickerRow(
                            icon = Icons.Default.NetworkCheck,
                            iconBg = Color(0xFFFF9500),
                            title = "Scan Strategy",
                            value = config.scanMode.displayName,
                            options = AetherScanMode.entries.map { "${it.displayName} (${it.description})" },
                            onOptionSelected = { index ->
                                onUpdateConfig(config.copy(scanMode = AetherScanMode.entries[index]))
                            },
                            scaleFactor = scaleFactor
                        )

                        HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = (50 * scaleFactor).dp))

                        IosPickerRow(
                            icon = Icons.AutoMirrored.Filled.AltRoute,
                            iconBg = Color(0xFF5856D6),
                            title = "IP Stack Mode",
                            value = config.ipMode.rawValue,
                            options = AetherIpMode.entries.map { it.displayName },
                            onOptionSelected = { index ->
                                onUpdateConfig(config.copy(ipMode = AetherIpMode.entries[index]))
                            },
                            scaleFactor = scaleFactor
                        )
                    }
                }
            }

            item {
                IosSectionHeader(title = "DPI BYPASS & MASQUE TWEAKS", scaleFactor = scaleFactor)
                IosGroupCard {
                    Column {
                        IosSwitchRow(
                            icon = Icons.Default.Http,
                            iconBg = Color(0xFF32ADE6),
                            title = "HTTP/2 Fallback Mode",
                            subtitle = "Use H2 over TCP when QUIC/UDP is throttled",
                            checked = config.h2Mode,
                            onCheckedChange = { onUpdateConfig(config.copy(h2Mode = it)) },
                            testTag = "switch_h2_mode",
                            scaleFactor = scaleFactor
                        )

                        HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = (50 * scaleFactor).dp))

                        IosSwitchRow(
                            icon = Icons.Default.Security,
                            iconBg = Color(0xFFFF2D55),
                            title = "ClientHello Fragmentation",
                            subtitle = "Split ClientHello packet to bypass SNI inspection",
                            checked = config.h2Fragment,
                            onCheckedChange = { onUpdateConfig(config.copy(h2Fragment = it)) },
                            testTag = "switch_h2_fragment",
                            scaleFactor = scaleFactor
                        )

                        AnimatedVisibility(
                            visible = config.h2Fragment,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(IosGroupBackground.copy(alpha = 0.4f))
                                    .padding((14 * scaleFactor).dp),
                                verticalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp)
                            ) {
                                IosInputField(
                                    label = "Fragment Size Range (bytes)",
                                    value = config.fragmentSize,
                                    onValueChange = { onUpdateConfig(config.copy(fragmentSize = it)) },
                                    placeholder = "e.g. 16-32",
                                    testTag = "fragment_size_input",
                                    scaleFactor = scaleFactor
                                )

                                IosInputField(
                                    label = "Fragment Delay Range (ms)",
                                    value = config.fragmentDelay,
                                    onValueChange = { onUpdateConfig(config.copy(fragmentDelay = it)) },
                                    placeholder = "e.g. 2-10",
                                    keyboardType = KeyboardType.Number,
                                    testTag = "fragment_delay_input",
                                    scaleFactor = scaleFactor
                                )
                            }
                        }

                        HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = (50 * scaleFactor).dp))

                        IosSwitchRow(
                            icon = Icons.Default.FlashOn,
                            iconBg = Color(0xFFFFCC00),
                            title = "Skip Gateway Data Check",
                            subtitle = "Bypass verification probe for rapid connection",
                            checked = config.noDataCheck,
                            onCheckedChange = { onUpdateConfig(config.copy(noDataCheck = it)) },
                            testTag = "switch_no_data_check",
                            scaleFactor = scaleFactor
                        )

                        HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = (50 * scaleFactor).dp))

                        IosInputFieldRow(
                            icon = Icons.Default.Lock,
                            iconBg = Color(0xFF8E8E93),
                            label = "Custom TLS Groups",
                            value = config.tlsGroups,
                            onValueChange = { onUpdateConfig(config.copy(tlsGroups = it)) },
                            placeholder = "e.g. P-256:X25519",
                            testTag = "tls_groups_input",
                            scaleFactor = scaleFactor
                        )
                    }
                }
            }

            item {
                IosSectionHeader(title = "SOCKS & SESSION RECONNECT", scaleFactor = scaleFactor)
                IosGroupCard {
                    Column {
                        IosSwitchRow(
                            icon = Icons.Default.Storage,
                            iconBg = Color(0xFF30D158),
                            title = "Quick Gateway Reconnect",
                            subtitle = "Reuse fast active gateway cache on launch",
                            checked = config.quickReconnect,
                            onCheckedChange = { onUpdateConfig(config.copy(quickReconnect = it)) },
                            testTag = "switch_quick_reconnect",
                            scaleFactor = scaleFactor
                        )

                        HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = (50 * scaleFactor).dp))

                        IosPickerRow(
                        icon = Icons.Default.VpnLock,
                        iconBg = Color(0xFF5856D6),
                        title = "Split Tunneling",
                        value = "${config.excludedPackages.size} Apps",
                        options = emptyList(),
                        onOptionSelected = { },
                        scaleFactor = scaleFactor,
                        onClickOverride = onOpenSplitTunneling
                    )

                    HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = (50 * scaleFactor).dp))

                    IosSwitchRow(
                        icon = Icons.Default.VpnLock,
                        iconBg = Color(0xFF5856D6),
                        title = "VPN Tunnel Mode",
                        subtitle = "Route all phone traffic through Aether",
                        checked = !config.proxyOnly,
                        onCheckedChange = { 
                            onUpdateConfig(config.copy(proxyOnly = !it))
                        },
                        testTag = "switch_proxy_only",
                        scaleFactor = scaleFactor
                    )

                    HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = (50 * scaleFactor).dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = (16 * scaleFactor).dp, vertical = (12 * scaleFactor).dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IosIconBadge(icon = Icons.Default.Language, backgroundColor = Color(0xFF007AFF), scaleFactor = scaleFactor)
                        Spacer(modifier = Modifier.width((12 * scaleFactor).dp))
                        IosInputField(
                            label = "SOCKS5 Host",
                            value = config.socksHost,
                            onValueChange = { onUpdateConfig(config.copy(socksHost = it)) },
                            modifier = Modifier.weight(1f),
                            placeholder = "127.0.0.1",
                            testTag = "socks_host_input",
                            scaleFactor = scaleFactor
                        )
                        Spacer(modifier = Modifier.width((10 * scaleFactor).dp))
                        IosInputField(
                            label = "Port",
                            value = config.socksPort,
                            onValueChange = { onUpdateConfig(config.copy(socksPort = it)) },
                            modifier = Modifier.width((80 * scaleFactor).dp),
                            placeholder = "1819",
                            keyboardType = KeyboardType.Number,
                            testTag = "socks_port_input",
                            scaleFactor = scaleFactor
                        )
                    }

                    if (!config.proxyOnly && config.socksHost == "127.0.0.1") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = (50 * scaleFactor).dp, end = (16 * scaleFactor).dp, bottom = (12 * scaleFactor).dp)
                                .background(Color(0xFFFF9500).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding((8 * scaleFactor).dp)
                        ) {
                            Text(
                                text = "Aether Core is active on 127.0.0.1. In Tunnel Mode, all other device traffic is automatically bridged via 198.18.0.1 for full compatibility.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFF9500),
                                fontSize = (11 * scaleFactor).sp,
                                lineHeight = (15 * scaleFactor).sp
                            )
                        }
                    }
                    }
                }
            }

            item {
                IosSectionHeader(title = "ADVANCED TUNING", scaleFactor = scaleFactor)
                IosGroupCard {
                    Column {
                        IosInputFieldRow(
                            icon = Icons.AutoMirrored.Filled.AltRoute,
                            iconBg = Color(0xFF5856D6),
                            label = "Forced Peer IP (Optional)",
                            value = config.peer,
                            onValueChange = { onUpdateConfig(config.copy(peer = it)) },
                            placeholder = "e.g. 1.2.3.4:443",
                            testTag = "peer_input",
                            scaleFactor = scaleFactor
                        )

                        HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = (50 * scaleFactor).dp))

                        IosInputFieldRow(
                            icon = Icons.Default.Bolt,
                            iconBg = Color(0xFFFF9500),
                            label = "WG Keepalive (Seconds)",
                            value = config.keepalive.toString(),
                            onValueChange = { onUpdateConfig(config.copy(keepalive = it.toIntOrNull() ?: 5)) },
                            placeholder = "5",
                            keyboardType = KeyboardType.Number,
                            testTag = "keepalive_input",
                            scaleFactor = scaleFactor
                        )

                        HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = (50 * scaleFactor).dp))

                        IosInputFieldRow(
                            icon = Icons.Default.Tune,
                            iconBg = Color(0xFF34C759),
                            label = "Custom MTU Size",
                            value = config.mtu.toString(),
                            onValueChange = { onUpdateConfig(config.copy(mtu = it.toIntOrNull() ?: 1100)) },
                            placeholder = "1100",
                            keyboardType = KeyboardType.Number,
                            testTag = "mtu_input",
                            scaleFactor = scaleFactor
                        )

                        HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = (50 * scaleFactor).dp))

                        IosInputFieldRow(
                            icon = Icons.Default.NetworkCheck,
                            iconBg = Color(0xFF32ADE6),
                            label = "Probe Validation Timeout (Secs)",
                            value = config.validateSecs.toString(),
                            onValueChange = { onUpdateConfig(config.copy(validateSecs = it.toIntOrNull() ?: 10)) },
                            placeholder = "10",
                            keyboardType = KeyboardType.Number,
                            testTag = "validate_secs_input",
                            scaleFactor = scaleFactor
                        )

                        HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = (50 * scaleFactor).dp))

                        IosSwitchRow(
                            icon = Icons.AutoMirrored.Filled.AltRoute,
                            iconBg = Color(0xFF8E8E93),
                            title = "No Profile Retry",
                            subtitle = "Disable automatic noise profile switching",
                            checked = config.noProfileRetry,
                            onCheckedChange = { onUpdateConfig(config.copy(noProfileRetry = it)) },
                            testTag = "switch_no_profile_retry",
                            scaleFactor = scaleFactor
                        )
                    }
                }
            }

            item {
                IosSectionHeader(title = "DIAGNOSTICS & SYSTEM LOGS", scaleFactor = scaleFactor)
                IosGroupCard {
                    Column {
                        IosPickerRow(
                            icon = Icons.Default.BugReport,
                            iconBg = Color(0xFF64D2FF),
                            title = "App System Logging",
                            value = config.appLogLevel.displayName.substringBefore(" ("),
                            options = AetherLogLevel.entries.map { it.displayName },
                            onOptionSelected = { index ->
                                onUpdateConfig(config.copy(appLogLevel = AetherLogLevel.entries[index]))
                            },
                            scaleFactor = scaleFactor
                        )

                        HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = (50 * scaleFactor).dp))

                        IosPickerRow(
                            icon = Icons.Default.VpnLock,
                            iconBg = Color(0xFF8E8E93),
                            title = "Aether Core Logging",
                            value = config.coreLogLevel.displayName.substringBefore(" ("),
                            options = AetherLogLevel.entries.map { it.displayName },
                            onOptionSelected = { index ->
                                onUpdateConfig(config.copy(coreLogLevel = AetherLogLevel.entries[index]))
                            },
                            scaleFactor = scaleFactor
                        )
                    }
                }
                Text(
                    text = "App logging is enabled by default to track UI states. Core logging is OFF to eliminate binary RAM & CPU overhead. Set Core to Info/Debug only for troubleshooting.",
                    style = MaterialTheme.typography.bodySmall,
                    color = IosSecondaryLabel,
                    modifier = Modifier.padding(start = 8.dp, top = 6.dp),
                    fontSize = (10 * scaleFactor).sp
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun IosPresetItem(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    isActive: Boolean,
    onClick: () -> Unit,
    scaleFactor: Float = 1f
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = (16 * scaleFactor).dp, vertical = (14 * scaleFactor).dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IosIconBadge(icon = icon, backgroundColor = iconBg, scaleFactor = scaleFactor)
            Spacer(modifier = Modifier.width((12 * scaleFactor).dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    fontSize = (15 * scaleFactor).sp
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = IosSecondaryLabel,
                    fontSize = (11 * scaleFactor).sp
                )
            }
        }

        if (isActive) {
            Text(
                text = "Active",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = IosActiveSwitchGreen,
                fontSize = (11 * scaleFactor).sp
            )
        } else {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = IosSecondaryLabel,
                modifier = Modifier.size((18 * scaleFactor).dp)
            )
        }
    }
}

@Composable
fun IosSectionHeader(title: String, scaleFactor: Float = 1f) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = IosSecondaryLabel,
        fontSize = (11 * scaleFactor).sp,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(start = 8.dp, bottom = (6 * scaleFactor).dp)
    )
}

@Composable
fun IosGroupCard(
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = IosCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        content()
    }
}

@Composable
fun IosIconBadge(
    icon: ImageVector,
    backgroundColor: Color,
    scaleFactor: Float = 1f
) {
    Box(
        modifier = Modifier
            .size((30 * scaleFactor).dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size((18 * scaleFactor).dp)
        )
    }
}

@Composable
fun IosSwitchRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    scaleFactor: Float = 1f
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = (16 * scaleFactor).dp, vertical = (12 * scaleFactor).dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IosIconBadge(icon = icon, backgroundColor = iconBg, scaleFactor = scaleFactor)
            Spacer(modifier = Modifier.width((12 * scaleFactor).dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    fontSize = (15 * scaleFactor).sp
                )
                if (!subtitle.isNullOrEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = IosSecondaryLabel,
                        fontSize = (11 * scaleFactor).sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier
                .testTag(testTag)
                .graphicsLayer {
                    scaleX = scaleFactor * 0.9f
                    scaleY = scaleFactor * 0.9f
                },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = IosActiveSwitchGreen,
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = IosInactiveSwitchTrack,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun IosPickerRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    value: String,
    options: List<String>,
    onOptionSelected: (Int) -> Unit,
    scaleFactor: Float = 1f,
    onClickOverride: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { 
                    if (onClickOverride != null) onClickOverride() else expanded = true 
                }
                .padding(horizontal = (16 * scaleFactor).dp, vertical = (14 * scaleFactor).dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IosIconBadge(icon = icon, backgroundColor = iconBg, scaleFactor = scaleFactor)
                Spacer(modifier = Modifier.width((12 * scaleFactor).dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    fontSize = (15 * scaleFactor).sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = IosSecondaryLabel,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    fontSize = (13 * scaleFactor).sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = IosSecondaryLabel,
                    modifier = Modifier.size((18 * scaleFactor).dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(IosGroupBackground)
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = (14 * scaleFactor).sp
                        )
                    },
                    onClick = {
                        onOptionSelected(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun IosInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    scaleFactor: Float = 1f
) {
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = IosSecondaryLabel,
            fontSize = (10 * scaleFactor).sp,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height((46 * scaleFactor).dp)
                .background(IosGroupBackground, RoundedCornerShape(10.dp))
                .border(
                    width = 1.dp,
                    color = if (isFocused) IosActiveBlue else Color.Transparent,
                    shape = RoundedCornerShape(10.dp)
                )
                .onFocusChanged { isFocused = it.isFocused }
                .testTag(testTag),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontSize = (14 * scaleFactor).sp),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            singleLine = true,
            cursorBrush = SolidColor(IosActiveBlue),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.padding(horizontal = (12 * scaleFactor).dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(placeholder, color = IosSecondaryLabel, fontSize = (13 * scaleFactor).sp)
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
fun IosInputFieldRow(
    icon: ImageVector,
    iconBg: Color,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    testTag: String,
    scaleFactor: Float = 1f
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = (16 * scaleFactor).dp, vertical = (12 * scaleFactor).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IosIconBadge(icon = icon, backgroundColor = iconBg, scaleFactor = scaleFactor)
        Spacer(modifier = Modifier.width((12 * scaleFactor).dp))
        IosInputField(
            label = label,
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = placeholder,
            keyboardType = keyboardType,
            testTag = testTag,
            scaleFactor = scaleFactor
        )
    }
}
