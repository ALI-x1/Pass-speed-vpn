package io.github.immaghzbad.aetherst.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.immaghzbad.aetherst.data.IpInfo
import io.github.immaghzbad.aetherst.data.PingState
import io.github.immaghzbad.aetherst.model.*
import io.github.immaghzbad.aetherst.ui.theme.LocalAppTheme
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun DashboardScreen(
    config: AetherConfig,
    connectionStatus: ConnectionStatus,
    elapsedSeconds: Long,
    sessionTraffic: SessionTraffic,
    ipInfo: IpInfo = IpInfo(),
    pingState: PingState = PingState(),
    onToggleVpn: () -> Unit,
    onUpdateProtocol: (AetherProtocol) -> Unit,
    onOpenSettings: () -> Unit = {},
    onRefreshIpInfo: () -> Unit = {},
    onRefreshPing: () -> Unit = {},
    onShowToast: (String, Boolean) -> Unit = { _, _ -> },
    bottomContentPadding: Dp = 0.dp
) {
    var showProxyOverlay by remember { mutableStateOf(true) }

    LaunchedEffect(connectionStatus) {
        if (connectionStatus != ConnectionStatus.RUNNING) {
            showProxyOverlay = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val screenWidth = this.maxWidth
        val screenHeight = this.maxHeight
        val scaleFactor = (screenWidth.value / 411f).coerceIn(0.7f, 1.1f)
        val isCompactHeight = screenHeight < 640.dp
        val isVeryCompactHeight = screenHeight < 580.dp
        val horizontalPadding = if (screenWidth < 360.dp) 12.dp else 16.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    bottom = bottomContentPadding + 12.dp
                ),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy((14 * scaleFactor).dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "Warden VPN",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = (26 * scaleFactor).sp,
                            lineHeight = (30 * scaleFactor).sp
                        )
                        Text(
                            text = if (config.connectionMode == ConnectionMode.TUNNEL) "Secure & Private" else "High-Performance Local Proxy",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = (12 * scaleFactor).sp,
                            lineHeight = (16 * scaleFactor).sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (config.connectionMode == ConnectionMode.PROXY_ONLY && connectionStatus == ConnectionStatus.RUNNING) {
                            IconButton(
                                onClick = { showProxyOverlay = true },
                                modifier = Modifier.size((32 * scaleFactor).dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Proxy Info",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size((22 * scaleFactor).dp)
                                )
                            }
                            Spacer(modifier = Modifier.width((8 * scaleFactor).dp))
                        }
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.size((36 * scaleFactor).dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size((24 * scaleFactor).dp)
                            )
                        }
                    }
                }

                WardenStatusHeroCard(
                    connectionStatus = connectionStatus,
                    elapsedSeconds = elapsedSeconds,
                    sessionTraffic = sessionTraffic,
                    config = config,
                    ipInfo = ipInfo,
                    pingState = pingState,
                    onRefreshIpInfo = onRefreshIpInfo,
                    onRefreshPing = onRefreshPing,
                    onShowToast = onShowToast,
                    hideConfigChips = isCompactHeight,
                    scaleFactor = scaleFactor
                )

                if (!isVeryCompactHeight && connectionStatus == ConnectionStatus.ERROR) {
                    val errorColor = LocalAppTheme.current.error
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = errorColor.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Refresh, null, tint = errorColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Connection failed. Please try reconnecting.",
                                color = errorColor,
                                fontSize = (11 * scaleFactor).sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                val minDim = if (screenWidth < screenHeight) screenWidth else screenHeight
                val buttonSize = (minDim * 0.35f).coerceIn(100.dp, 160.dp)
                
                WardenPowerButton(
                    connectionStatus = connectionStatus,
                    onToggle = onToggleVpn,
                    size = buttonSize
                )
            }

            if (!isVeryCompactHeight) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                ) {
                    WardenProtocolSegmentedControl(
                        selectedProtocol = config.protocol,
                        onProtocolSelected = onUpdateProtocol,
                        enabled = connectionStatus == ConnectionStatus.STOPPED || connectionStatus == ConnectionStatus.ERROR,
                        scaleFactor = scaleFactor
                    )
                }
            }
        }

        val offsetY = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(showProxyOverlay) {
            if (showProxyOverlay) {
                offsetY.snapTo(0f)
            }
        }

        AnimatedVisibility(
            visible = config.connectionMode == ConnectionMode.PROXY_ONLY && connectionStatus == ConnectionStatus.RUNNING && showProxyOverlay,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp)
                .graphicsLayer { translationY = offsetY.value }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (offsetY.value < -100f) {
                                    showProxyOverlay = false
                                } else {
                                    offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                }
                            }
                        },
                        onVerticalDrag = { _, dragAmount ->
                            scope.launch {
                                offsetY.snapTo((offsetY.value + dragAmount).coerceAtMost(20f))
                            }
                        }
                    )
                }
        ) {
            ProxyOverlayPill(
                host = config.socksHost,
                socksPort = config.socksPort,
                httpPort = config.httpPort,
                onHide = { showProxyOverlay = false },
                onCopy = { 
                    onShowToast("Address copied: $it", false)
                },
                scaleFactor = scaleFactor
            )
        }
    }
}

@Composable
fun ProxyOverlayPill(
    host: String,
    socksPort: String,
    httpPort: String,
    onHide: () -> Unit,
    onCopy: (String) -> Unit,
    scaleFactor: Float
) {
    val clipboardManager = LocalClipboardManager.current
    val socksAddress = "$host:$socksPort"
    val httpAddress = "$host:$httpPort"

    Surface(
        modifier = Modifier
            .widthIn(max = 400.dp)
            .padding(horizontal = 8.dp)
            .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Dns, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ProxyCopyRow(
                    label = "SOCKS5",
                    address = socksAddress,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(socksAddress))
                        onCopy(socksAddress)
                    },
                    scaleFactor = scaleFactor
                )
                ProxyCopyRow(
                    label = "HTTP",
                    address = httpAddress,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(httpAddress))
                        onCopy(httpAddress)
                    },
                    scaleFactor = scaleFactor
                )
            }

            VerticalDivider(modifier = Modifier.height(36.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

            IconButton(
                onClick = onHide,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun ProxyCopyRow(
    label: String,
    address: String,
    onCopy: () -> Unit,
    scaleFactor: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCopy() }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = (9 * scaleFactor).sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = address,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = (12 * scaleFactor).sp,
                maxLines = 1
            )
        }
        Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = "Copy",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size((14 * scaleFactor).dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WardenStatusHeroCard(
    connectionStatus: ConnectionStatus,
    elapsedSeconds: Long,
    sessionTraffic: SessionTraffic,
    config: AetherConfig,
    ipInfo: IpInfo = IpInfo(),
    pingState: PingState = PingState(),
    onRefreshIpInfo: () -> Unit = {},
    onRefreshPing: () -> Unit = {},
    onShowToast: (String, Boolean) -> Unit = { _, _ -> },
    hideConfigChips: Boolean = false,
    scaleFactor: Float = 1f
) {
    val appTheme = LocalAppTheme.current
    val defaultMutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val statusColor by animateColorAsState(
        targetValue = when (connectionStatus) {
            ConnectionStatus.RUNNING -> appTheme.connected
            ConnectionStatus.STARTING, ConnectionStatus.VALIDATING, ConnectionStatus.RECONNECTING, ConnectionStatus.STOPPING -> appTheme.scanning
            ConnectionStatus.ERROR -> appTheme.error
            ConnectionStatus.STOPPED -> defaultMutedColor
        },
        label = "statusColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("status_hero_card")
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            statusColor.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding((14 * scaleFactor).dp)
        ) {
            Column {
                // --- بخش وضعیت بالا (Finding Servers / Protocol) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size((7 * scaleFactor).dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width((5 * scaleFactor).dp))
                        Text(
                            text = when (connectionStatus) {
                                ConnectionStatus.RUNNING -> if (config.connectionMode == ConnectionMode.TUNNEL) "PROTECTED & CONNECTED" else "PROXY ACTIVE"
                                ConnectionStatus.STARTING -> "FINDING SERVERS..."
                                ConnectionStatus.VALIDATING -> "ESTABLISHING LINK..."
                                ConnectionStatus.RECONNECTING -> "RECONNECTING..."
                                ConnectionStatus.STOPPING -> "STOPPING..."
                                ConnectionStatus.ERROR -> "CONNECTION ERROR"
                                ConnectionStatus.STOPPED -> "READY TO CONNECT"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = statusColor,
                            fontSize = (8.5 * scaleFactor).sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        val protocolText = if (config.protocol == AetherProtocol.MASQUE) {
                            if (config.h2Mode) "MASQUE (H2)" else "MASQUE (H3)"
                        } else {
                            config.protocol.displayName
                        }
                        Text(
                            text = protocolText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = (8.5 * scaleFactor).sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height((16 * scaleFactor).dp))

                // --- تایمر و پینگ ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = formatTime(elapsedSeconds),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = (32 * scaleFactor).sp
                    )

                    if (connectionStatus == ConnectionStatus.RUNNING) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onRefreshPing() }
                                .padding(4.dp)
                        ) {
                            if (pingState.isPinging) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size((14 * scaleFactor).dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "Ping",
                                    tint = if (pingState.error != null) appTheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size((16 * scaleFactor).dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when {
                                    pingState.isPinging -> "..."
                                    pingState.error != null -> "ERR"
                                    pingState.ms >= 0 -> "${pingState.ms}ms"
                                    else -> "PING"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (pingState.error != null) appTheme.error else MaterialTheme.colorScheme.primary,
                                fontSize = (14 * scaleFactor).sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height((14 * scaleFactor).dp))

                // --- سرعت آپلود و دانلود ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "UPLOAD", 
                            fontSize = (9 * scaleFactor).sp, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant, 
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatTrafficBytes(sessionTraffic.uploadedBytes), 
                            fontSize = (14 * scaleFactor).sp, 
                            color = MaterialTheme.colorScheme.primary, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text(
                            text = "DOWNLOAD", 
                            fontSize = (9 * scaleFactor).sp, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant, 
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatTrafficBytes(sessionTraffic.downloadedBytes), 
                            fontSize = (14 * scaleFactor).sp, 
                            color = appTheme.connected, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height((12 * scaleFactor).dp))

                // --- کارت آی پی و کشور زنده ---
                val clipboardManager = LocalClipboardManager.current
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (ipInfo.ip.isNotEmpty()) {
                                clipboardManager.setText(AnnotatedString(ipInfo.ip))
                                onShowToast("IP copied to clipboard", false)
                            } else {
                                onRefreshIpInfo()
                            }
                        },
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = ipInfo.flagEmoji.ifEmpty { "🌐" }, 
                                fontSize = (22 * scaleFactor).sp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = when {
                                        ipInfo.country.isNotEmpty() -> if (ipInfo.countryCode.isNotEmpty()) "${ipInfo.country} (${ipInfo.countryCode})" else ipInfo.country
                                        ipInfo.isLoading -> "Locating..."
                                        ipInfo.error != null -> "Error"
                                        else -> "Unknown Location"
                                    },
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (13 * scaleFactor).sp
                                )
                                Text(
                                    text = when {
                                        ipInfo.ip.isNotEmpty() -> ipInfo.ip
                                        ipInfo.isLoading -> "Fetching IP..."
                                        ipInfo.error != null -> "Failed to get IP"
                                        else -> "Tap to fetch IP"
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = (11 * scaleFactor).sp
                                )
                            }
                        }
                        if (ipInfo.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size((16 * scaleFactor).dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size((18 * scaleFactor).dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WardenPowerButton(
    connectionStatus: ConnectionStatus,
    onToggle: () -> Unit,
    size: Dp
) {
    val isRunning = connectionStatus == ConnectionStatus.RUNNING
    val isConnecting = connectionStatus == ConnectionStatus.STARTING || 
                       connectionStatus == ConnectionStatus.VALIDATING || 
                       connectionStatus == ConnectionStatus.RECONNECTING
    val isError = connectionStatus == ConnectionStatus.ERROR

    val appTheme = LocalAppTheme.current
    val inactiveColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)

    // انیمیشن نرم برای رنگ اصلی براساس تم و وضعیت
    val mainColor by animateColorAsState(
        targetValue = when {
            isRunning -> appTheme.connected
            isConnecting -> appTheme.scanning
            isError -> appTheme.error
            else -> inactiveColor
        },
        animationSpec = tween(durationMillis = 400),
        label = "MainColorAnimation"
    )

    // پس‌زمینه داخلی دکمه (نیمه‌شفاف)
    val animatedContainerColor by animateColorAsState(
        targetValue = when {
            isRunning -> appTheme.connected.copy(alpha = 0.12f)
            isConnecting -> appTheme.scanning.copy(alpha = 0.12f)
            isError -> appTheme.error.copy(alpha = 0.12f)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 400),
        label = "ContainerColorAnimation"
    )

    // انیمیشن ضربان (Pulse Glow) فقط در حالت متصل
    val infiniteTransition = rememberInfiniteTransition(label = "GlowTransition")
    
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isRunning) 1.28f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HaloScale"
    )

    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = if (isRunning) 0.35f else 0.0f,
        targetValue = if (isRunning) 0.02f else 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HaloAlpha"
    )

    // انیمیشن فنری برای لمس دکمه
    var isPressed by remember { mutableStateOf(false) }
    val buttonClickScale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "ClickScale"
    )

    Box(
        modifier = Modifier.size(size * 1.4f), // ایجاد فضای بیشتر برای هاله
        contentAlignment = Alignment.Center
    ) {
        // هاله‌های نورانی (فقط وقتی متصل است پخش می‌شود)
        if (isRunning) {
            Box(
                modifier = Modifier
                    .size(size)
                    .scale(haloScale * 1.12f)
                    .clip(CircleShape)
                    .background(mainColor.copy(alpha = haloAlpha * 0.4f))
            )
            Box(
                modifier = Modifier
                    .size(size)
                    .scale(haloScale)
                    .clip(CircleShape)
                    .background(mainColor.copy(alpha = haloAlpha))
            )
        }

        // هسته دکمه تعاملی
        Box(
            modifier = Modifier
                .size(size)
                .scale(buttonClickScale)
                .clip(CircleShape)
                .background(animatedContainerColor)
                .border(
                    width = 3.dp,
                    color = mainColor,
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        isPressed = true
                        onToggle()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            LaunchedEffect(isPressed) {
                if (isPressed) {
                    kotlinx.coroutines.delay(100)
                    isPressed = false
                }
            }

            // نمایش Loading موقع اتصال، یا آیکون+متن موقع توقف/اتصال موفق
            if (isConnecting) {
                CircularProgressIndicator(
                    color = mainColor,
                    modifier = Modifier.size(size * 0.35f),
                    strokeWidth = 3.dp
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = if (isRunning) "Disconnect" else "Connect",
                        tint = mainColor,
                        modifier = Modifier.size(size * 0.28f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isRunning) "قطع کن" else "اتصال",
                        color = mainColor,
                        fontSize = (size.value * 0.12f).sp, // فونت داینامیک براساس سایز
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun WardenProtocolSegmentedControl(
    selectedProtocol: AetherProtocol,
    onProtocolSelected: (AetherProtocol) -> Unit,
    enabled: Boolean,
    scaleFactor: Float
) {
    val protocols = AetherProtocol.values().toList()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        protocols.forEach { protocol ->
            val isSelected = protocol == selectedProtocol
            val bgColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, label = "")
            val contentColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, label = "")
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .clickable(enabled = enabled) { onProtocolSelected(protocol) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = protocol.displayName,
                    color = contentColor,
                    fontSize = (12 * scaleFactor).sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// --- توابع فرمت‌کننده برای تایمر و حجم اینترنت ---
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
