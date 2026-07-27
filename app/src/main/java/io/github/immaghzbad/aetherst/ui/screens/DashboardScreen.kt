package io.github.immaghzbad.aetherst.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.immaghzbad.aetherst.data.IpInfo
import io.github.immaghzbad.aetherst.data.PingState
import io.github.immaghzbad.aetherst.model.AetherConfig
import io.github.immaghzbad.aetherst.model.AetherProtocol
import io.github.immaghzbad.aetherst.model.ConnectionState
import io.github.immaghzbad.aetherst.model.SessionTraffic
import java.util.Locale

private val IosCardBg = Color(0xFF1C1C1E)
private val IosGroupBg = Color(0xFF2C2C2E)
private val IosSecondaryLabel = Color(0xFF8E8E93)
private val IosActiveGreen = Color(0xFF34C759)
private val IosActiveBlue = Color(0xFF007AFF)
private val IosScanningAmber = Color(0xFFFF9500)
private val IosErrorRed = Color(0xFFFF3B30)

@Composable
fun DashboardScreen(
    config: AetherConfig,
    connectionState: ConnectionState,
    elapsedSeconds: Long,
    sessionTraffic: SessionTraffic,
    ipInfo: IpInfo = IpInfo(),
    pingState: PingState = PingState(),
    onToggleVpn: () -> Unit,
    onUpdateProtocol: (AetherProtocol) -> Unit,
    onRefreshIpInfo: () -> Unit = {},
    onRefreshPing: () -> Unit = {},
    bottomContentPadding: Dp = 0.dp
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(
                start = 16.dp,
                top = 12.dp,
                end = 16.dp,
                bottom = bottomContentPadding + 12.dp
            ),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AetherST Tunnel",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "One tap. Private everywhere.",
                        style = MaterialTheme.typography.bodySmall,
                        color = IosSecondaryLabel
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = IosGroupBg
                ) {
                    Text(
                        text = "v1.0.0-BETA",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = IosActiveBlue
                    )
                }
            }

            IosStatusHeroCard(
                connectionState = connectionState,
                elapsedSeconds = elapsedSeconds,
                sessionTraffic = sessionTraffic,
                config = config,
                ipInfo = ipInfo,
                pingState = pingState,
                onRefreshIpInfo = onRefreshIpInfo,
                onRefreshPing = onRefreshPing
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            IosPowerButton(
                connectionState = connectionState,
                onToggle = onToggleVpn
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            IosProtocolSegmentedControl(
                selectedProtocol = config.protocol,
                onProtocolSelected = onUpdateProtocol,
                enabled = connectionState == ConnectionState.DISCONNECTED || connectionState == ConnectionState.ERROR
            )
        }
    }
}

@Composable
fun IosStatusHeroCard(
    connectionState: ConnectionState,
    elapsedSeconds: Long,
    sessionTraffic: SessionTraffic,
    config: AetherConfig,
    ipInfo: IpInfo = IpInfo(),
    pingState: PingState = PingState(),
    onRefreshIpInfo: () -> Unit = {},
    onRefreshPing: () -> Unit = {}
) {
    val statusColor by animateColorAsState(
        targetValue = when (connectionState) {
            ConnectionState.CONNECTED -> IosActiveGreen
            ConnectionState.SCANNING, ConnectionState.VALIDATING, ConnectionState.RECONNECTING, ConnectionState.DISCONNECTING -> IosScanningAmber
            ConnectionState.ERROR -> IosErrorRed
            ConnectionState.DISCONNECTED -> IosSecondaryLabel
        },
        label = "statusColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("status_hero_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = IosCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            statusColor.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (connectionState) {
                                ConnectionState.CONNECTED -> "SECURELY CONNECTED"
                                ConnectionState.SCANNING -> "SCANNING GATEWAYS..."
                                ConnectionState.VALIDATING -> "VALIDATING PROBE..."
                                ConnectionState.RECONNECTING -> "RECONNECTING..."
                                ConnectionState.DISCONNECTING -> "DISCONNECTING..."
                                ConnectionState.ERROR -> "CONNECTION ERROR"
                                ConnectionState.DISCONNECTED -> "DISCONNECTED"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = statusColor
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = IosGroupBg
                    ) {
                        val protocolText = if (config.protocol == AetherProtocol.MASQUE) {
                            if (config.h2Mode) "MASQUE (HTTP/2)" else "MASQUE (HTTP/3)"
                        } else {
                            config.protocol.displayName
                        }
                        Text(
                            text = protocolText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = IosActiveBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = if (connectionState == ConnectionState.CONNECTED || connectionState == ConnectionState.RECONNECTING || elapsedSeconds > 0L) "Tunnel Uptime" else "Standby Duration",
                            style = MaterialTheme.typography.bodySmall,
                            color = IosSecondaryLabel
                        )
                        Text(
                            text = formatTime(elapsedSeconds),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    if (connectionState == ConnectionState.CONNECTED) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onRefreshPing() }
                                .padding(4.dp)
                        ) {
                            if (pingState.isPinging) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = IosActiveBlue,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "Ping",
                                    tint = if (pingState.error != null) IosErrorRed else IosActiveBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when {
                                    pingState.isPinging -> "..."
                                    pingState.error != null -> pingState.error
                                    pingState.ms >= 0 -> "${pingState.ms} ms"
                                    else -> "Ping"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (pingState.error != null) IosErrorRed else IosActiveBlue
                            )
                        }
                    } else {
                        Text(
                            text = if (connectionState == ConnectionState.RECONNECTING) "RETRYING" else "OFFLINE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (connectionState == ConnectionState.RECONNECTING) IosScanningAmber else IosSecondaryLabel,
                            modifier = Modifier.clickable { onRefreshPing() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(IosGroupBg)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TrafficValue(
                        label = "UPLOAD",
                        value = formatTrafficBytes(sessionTraffic.uploadedBytes),
                        color = IosActiveBlue,
                        alignment = Alignment.Start,
                        modifier = Modifier.weight(1f)
                    )
                    TrafficValue(
                        label = "DOWNLOAD",
                        value = formatTrafficBytes(sessionTraffic.downloadedBytes),
                        color = IosActiveGreen,
                        alignment = Alignment.End,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onRefreshIpInfo() },
                    color = IosGroupBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = ipInfo.flagEmoji.ifEmpty { "🌐" },
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = when {
                                        ipInfo.country.isNotEmpty() -> if (ipInfo.countryCode.isNotEmpty()) "${ipInfo.country} (${ipInfo.countryCode})" else ipInfo.country
                                        ipInfo.isLoading -> "Resolving Location..."
                                        ipInfo.error != null -> "Geo Query Failed (Tap to retry)"
                                        else -> "Unknown Location"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = when {
                                        ipInfo.ip.isNotEmpty() -> "IP: ${ipInfo.ip}"
                                        ipInfo.isLoading -> if (connectionState == ConnectionState.CONNECTED) "Querying via SOCKS5..." else "Querying directly..."
                                        ipInfo.error != null -> ipInfo.error
                                        else -> "Tap to refresh IP info"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (ipInfo.error != null) IosScanningAmber else IosSecondaryLabel,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (ipInfo.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = IosActiveBlue,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Location",
                                tint = IosSecondaryLabel,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(IosGroupBg)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    IosConfigChip(label = "NOISE", value = config.noise.displayName.split(" ")[0])
                    IosConfigChip(label = "SCAN", value = config.scanMode.name.lowercase().replaceFirstChar { it.uppercase() })
                    IosConfigChip(label = "IP STACK", value = config.ipMode.rawValue)
                }
            }
        }
    }
}

@Composable
private fun TrafficValue(label: String, value: String, color: Color, alignment: Alignment.Horizontal, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = alignment) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = IosSecondaryLabel,
            fontSize = 10.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun IosConfigChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = IosSecondaryLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(text = value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun IosPowerButton(
    connectionState: ConnectionState,
    onToggle: () -> Unit
) {
    val isConnected = connectionState == ConnectionState.CONNECTED
    val isWorking = connectionState == ConnectionState.SCANNING ||
                    connectionState == ConnectionState.VALIDATING ||
                    connectionState == ConnectionState.RECONNECTING ||
                    connectionState == ConnectionState.DISCONNECTING
    val canToggle = connectionState != ConnectionState.DISCONNECTING

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "buttonScale"
    )

    val cornerRadius by animateDpAsState(
        targetValue = if (isConnected || isWorking) 32.dp else 60.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cornerRadius"
    )

    val buttonColor by animateColorAsState(
        targetValue = when {
            isConnected -> IosActiveGreen
            isWorking -> IosScanningAmber
            else -> IosActiveBlue
        },
        animationSpec = tween(durationMillis = 600),
        label = "buttonColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "m3Pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isWorking) 1.35f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .padding(vertical = 12.dp)
            .graphicsLayer {
                scaleX = buttonScale
                scaleY = buttonScale
            },
        contentAlignment = Alignment.Center
    ) {
        if (isWorking || isConnected) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = if (isWorking) 0.25f else 0.12f
                    }
                    .background(buttonColor, CircleShape)
            )
        }

        Surface(
            modifier = Modifier
                .size(120.dp)
                .shadow(
                    elevation = if (isPressed) 4.dp else 16.dp,
                    shape = RoundedCornerShape(cornerRadius),
                    ambientColor = buttonColor.copy(alpha = 0.4f),
                    spotColor = buttonColor
                )
                .clip(RoundedCornerShape(cornerRadius))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = canToggle,
                    onClick = onToggle
                )
                .testTag("power_toggle_button"),
            color = buttonColor,
            tonalElevation = 10.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "Toggle Connection",
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }
        }
    }
}

@Composable
fun IosProtocolSegmentedControl(
    selectedProtocol: AetherProtocol,
    onProtocolSelected: (AetherProtocol) -> Unit,
    enabled: Boolean = true
) {
    Column {
        Text(
            text = "TRANSPORT PROTOCOL",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = IosSecondaryLabel,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(IosCardBg)
                .padding(3.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AetherProtocol.entries.forEach { proto ->
                val selected = proto == selectedProtocol
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (selected) IosActiveBlue else Color.Transparent
                        )
                        .clickable(enabled = enabled) { onProtocolSelected(proto) }
                        .padding(vertical = 10.dp)
                        .graphicsLayer { alpha = if (enabled || selected) 1f else 0.5f }
                        .testTag("protocol_${proto.rawValue}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = proto.displayName.split(" ")[0].uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) Color.White else IosSecondaryLabel
                    )
                }
            }
        }
    }
}

private fun formatTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return String.format(Locale.ROOT, "%02d:%02d:%02d", h, m, s)
}

private fun formatTrafficBytes(bytes: Long): String {
    val safeBytes = bytes.coerceAtLeast(0)
    val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
    var value = safeBytes.toDouble()
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex += 1
    }
    return if (unitIndex == 0) {
        "$safeBytes ${units[unitIndex]}"
    } else {
        String.format(Locale.ROOT, "%.2f %s", value, units[unitIndex])
    }
}
