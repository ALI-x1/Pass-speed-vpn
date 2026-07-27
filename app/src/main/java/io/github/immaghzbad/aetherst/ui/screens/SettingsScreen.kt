package io.github.immaghzbad.aetherst.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
    bottomContentPadding: Dp = 0.dp
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 12.dp,
            end = 16.dp,
            bottom = bottomContentPadding + 12.dp
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column {
                Text(
                    text = "AetherST Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Configure engine protocols, obfuscation & transport parameters",
                    style = MaterialTheme.typography.bodySmall,
                    color = IosSecondaryLabel
                )
            }
        }

        item {
            IosSectionHeader(title = "PRESET CONFIGURATION PROFILES")
            IosGroupCard {
                Column {
                    IosPresetItem(
                        icon = Icons.Default.Tune,
                        iconBg = Color(0xFF8E8E93),
                        title = "Custom Manual Tweaks",
                        subtitle = "Your own independent manual configuration",
                        isActive = config.presetId == "custom",
                        onClick = { onApplyPreset("custom") }
                    )

                    HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 52.dp))

                    IosPresetItem(
                        icon = Icons.Default.Lock,
                        iconBg = Color(0xFF5856D6),
                        title = "Bypass UDP / TLS",
                        subtitle = "MASQUE + H2 Fallback + Packet Fragmentation",
                        isActive = config.presetId == "bypass_udp",
                        onClick = { onApplyPreset("bypass_udp") }
                    )

                    HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 52.dp))

                    IosPresetItem(
                        icon = Icons.Default.Shield,
                        iconBg = Color(0xFF007AFF),
                        title = "Ironclad Stealth",
                        subtitle = "MASQUE + GFW Noise + Ironclad Probe Scan",
                        isActive = config.presetId == "ironclad_stealth",
                        onClick = { onApplyPreset("ironclad_stealth") }
                    )

                    HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 52.dp))

                    IosPresetItem(
                        icon = Icons.Default.Bolt,
                        iconBg = Color(0xFFFF9500),
                        title = "Turbo Speed",
                        subtitle = "WireGuard + Balanced Noise + Turbo Scan",
                        isActive = config.presetId == "turbo_wg",
                        onClick = { onApplyPreset("turbo_wg") }
                    )
                }
            }
        }

        item {
            IosSectionHeader(title = "CORE TRANSPORT & ENGINE")
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
                        }
                    )

                    HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 52.dp))

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
                        }
                    )

                    HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 52.dp))

                    IosPickerRow(
                        icon = Icons.Default.NetworkCheck,
                        iconBg = Color(0xFFFF9500),
                        title = "Scan Strategy",
                        value = config.scanMode.displayName,
                        options = AetherScanMode.entries.map { "${it.displayName} (${it.description})" },
                        onOptionSelected = { index ->
                            onUpdateConfig(config.copy(scanMode = AetherScanMode.entries[index]))
                        }
                    )

                    HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 52.dp))

                    IosPickerRow(
                        icon = Icons.AutoMirrored.Filled.AltRoute,
                        iconBg = Color(0xFF5856D6),
                        title = "IP Stack Mode",
                        value = config.ipMode.rawValue,
                        options = AetherIpMode.entries.map { it.displayName },
                        onOptionSelected = { index ->
                            onUpdateConfig(config.copy(ipMode = AetherIpMode.entries[index]))
                        }
                    )
                }
            }
        }

        item {
            IosSectionHeader(title = "DPI BYPASS & MASQUE TWEAKS")
            IosGroupCard {
                Column {
                    IosSwitchRow(
                        icon = Icons.Default.Http,
                        iconBg = Color(0xFF32ADE6),
                        title = "HTTP/2 Fallback Mode",
                        subtitle = "Use H2 over TCP when QUIC/UDP is throttled",
                        checked = config.h2Mode,
                        onCheckedChange = { onUpdateConfig(config.copy(h2Mode = it)) },
                        testTag = "switch_h2_mode"
                    )

                    HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 52.dp))

                    IosSwitchRow(
                        icon = Icons.Default.Security,
                        iconBg = Color(0xFFFF2D55),
                        title = "ClientHello Fragmentation",
                        subtitle = "Split ClientHello packet to bypass SNI inspection",
                        checked = config.h2Fragment,
                        onCheckedChange = { onUpdateConfig(config.copy(h2Fragment = it)) },
                        testTag = "switch_h2_fragment"
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
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IosInputField(
                                label = "Fragment Size Range (bytes)",
                                value = config.fragmentSize,
                                onValueChange = { onUpdateConfig(config.copy(fragmentSize = it)) },
                                placeholder = "e.g. 16-32",
                                testTag = "fragment_size_input"
                            )

                            IosInputField(
                                label = "Fragment Delay Range (ms)",
                                value = config.fragmentDelay,
                                onValueChange = { onUpdateConfig(config.copy(fragmentDelay = it)) },
                                placeholder = "e.g. 2-10",
                                keyboardType = KeyboardType.Number,
                                testTag = "fragment_delay_input"
                            )
                        }
                    }

                    HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 52.dp))

                    IosSwitchRow(
                        icon = Icons.Default.FlashOn,
                        iconBg = Color(0xFFFFCC00),
                        title = "Skip Gateway Data Check",
                        subtitle = "Bypass verification probe for rapid connection",
                        checked = config.noDataCheck,
                        onCheckedChange = { onUpdateConfig(config.copy(noDataCheck = it)) },
                        testTag = "switch_no_data_check"
                    )

                    HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 52.dp))

                    IosInputFieldRow(
                        icon = Icons.Default.Lock,
                        iconBg = Color(0xFF8E8E93),
                        label = "Custom TLS Groups",
                        value = config.tlsGroups,
                        onValueChange = { onUpdateConfig(config.copy(tlsGroups = it)) },
                        placeholder = "e.g. P-256:X25519",
                        testTag = "tls_groups_input"
                    )
                }
            }
        }

        item {
            IosSectionHeader(title = "SOCKS & SESSION RECONNECT")
            IosGroupCard {
                Column {
                    IosSwitchRow(
                        icon = Icons.Default.Storage,
                        iconBg = Color(0xFF30D158),
                        title = "Quick Gateway Reconnect",
                        subtitle = "Reuse fast active gateway cache on launch",
                        checked = config.quickReconnect,
                        onCheckedChange = { onUpdateConfig(config.copy(quickReconnect = it)) },
                        testTag = "switch_quick_reconnect"
                    )

                    HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 52.dp))

                    IosSwitchRow(
                        icon = Icons.Default.VpnLock,
                        iconBg = Color(0xFF5856D6),
                        title = "Enable VPN Tunnel",
                        subtitle = "Route all phone traffic through Aether",
                        checked = !config.proxyOnly,
                        onCheckedChange = { onUpdateConfig(config.copy(proxyOnly = !it)) },
                        testTag = "switch_proxy_only"
                    )

                    HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 52.dp))

                    IosInputFieldRow(
                        icon = Icons.Default.Language,
                        iconBg = Color(0xFF007AFF),
                        label = "SOCKS5 Local Bind Address",
                        value = config.socksAddress,
                        onValueChange = { onUpdateConfig(config.copy(socksAddress = it)) },
                        placeholder = "127.0.0.1:1819",
                        testTag = "socks_address_input"
                    )
                }
            }
        }

        item {
            IosSectionHeader(title = "ADVANCED TUNING")
            IosGroupCard {
                Column {
                    IosInputFieldRow(
                        icon = Icons.AutoMirrored.Filled.AltRoute,
                        iconBg = Color(0xFF5856D6),
                        label = "Forced Peer IP (Optional)",
                        value = config.peer,
                        onValueChange = { onUpdateConfig(config.copy(peer = it)) },
                        placeholder = "e.g. 1.2.3.4:443",
                        testTag = "peer_input"
                    )

                    HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 52.dp))

                    IosInputFieldRow(
                        icon = Icons.Default.Bolt,
                        iconBg = Color(0xFFFF9500),
                        label = "WG Keepalive (Seconds)",
                        value = config.keepalive.toString(),
                        onValueChange = { onUpdateConfig(config.copy(keepalive = it.toIntOrNull() ?: 5)) },
                        placeholder = "5",
                        keyboardType = KeyboardType.Number,
                        testTag = "keepalive_input"
                    )

                    HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 52.dp))

                    IosInputFieldRow(
                        icon = Icons.Default.Tune,
                        iconBg = Color(0xFF34C759),
                        label = "Custom MTU Size",
                        value = config.mtu.toString(),
                        onValueChange = { onUpdateConfig(config.copy(mtu = it.toIntOrNull() ?: 1100)) },
                        placeholder = "1100",
                        keyboardType = KeyboardType.Number,
                        testTag = "mtu_input"
                    )

                    HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 52.dp))

                    IosInputFieldRow(
                        icon = Icons.Default.NetworkCheck,
                        iconBg = Color(0xFF32ADE6),
                        label = "Probe Validation Timeout (Secs)",
                        value = config.validateSecs.toString(),
                        onValueChange = { onUpdateConfig(config.copy(validateSecs = it.toIntOrNull() ?: 10)) },
                        placeholder = "10",
                        keyboardType = KeyboardType.Number,
                        testTag = "validate_secs_input"
                    )

                    HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 52.dp))

                    IosSwitchRow(
                        icon = Icons.AutoMirrored.Filled.AltRoute,
                        iconBg = Color(0xFF8E8E93),
                        title = "No Profile Retry",
                        subtitle = "Disable automatic noise profile switching",
                        checked = config.noProfileRetry,
                        onCheckedChange = { onUpdateConfig(config.copy(noProfileRetry = it)) },
                        testTag = "switch_no_profile_retry"
                    )
                }
            }
        }

        item {
            IosSectionHeader(title = "DIAGNOSTICS & SYSTEM LOGS")
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
                        }
                    )

                    HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 52.dp))

                    IosPickerRow(
                        icon = Icons.Default.VpnLock,
                        iconBg = Color(0xFF8E8E93),
                        title = "Aether Core Logging",
                        value = config.coreLogLevel.displayName.substringBefore(" ("),
                        options = AetherLogLevel.entries.map { it.displayName },
                        onOptionSelected = { index ->
                            onUpdateConfig(config.copy(coreLogLevel = AetherLogLevel.entries[index]))
                        }
                    )
                }
            }
            Text(
                text = "App logging is enabled by default to track UI states. Core logging is OFF to eliminate binary RAM & CPU overhead. Set Core to Info/Debug only for troubleshooting.",
                style = MaterialTheme.typography.bodySmall,
                color = IosSecondaryLabel,
                modifier = Modifier.padding(start = 8.dp, top = 6.dp)
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun IosPresetItem(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IosIconBadge(icon = icon, backgroundColor = iconBg)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = IosSecondaryLabel,
                    fontSize = 12.sp
                )
            }
        }

        if (isActive) {
            Text(
                text = "Active",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = IosActiveSwitchGreen
            )
        } else {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = IosSecondaryLabel,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun IosSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = IosSecondaryLabel,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
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
    backgroundColor: Color
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
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
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IosIconBadge(icon = icon, backgroundColor = iconBg)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                if (!subtitle.isNullOrEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = IosSecondaryLabel,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
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
    onOptionSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IosIconBadge(icon = icon, backgroundColor = iconBg)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = IosSecondaryLabel,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = IosSecondaryLabel,
                    modifier = Modifier.size(18.dp)
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
                            style = MaterialTheme.typography.bodyMedium
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
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = IosSecondaryLabel,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = IosSecondaryLabel, fontSize = 13.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag(testTag),
            shape = RoundedCornerShape(10.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = IosGroupBackground,
                unfocusedContainerColor = IosGroupBackground,
                focusedBorderColor = IosActiveBlue,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
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
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IosIconBadge(icon = icon, backgroundColor = iconBg)
        Spacer(modifier = Modifier.width(12.dp))
        IosInputField(
            label = label,
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = placeholder,
            keyboardType = keyboardType,
            testTag = testTag
        )
    }
}
