package io.github.immaghzbad.aetherst.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
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

    val isDark = isSystemInDarkTheme()
    
    // اعمال دقیق رنگ‌های پس‌زمینه بر اساس فایل HTML شما
    val htmlDashboardBg = if (isDark) Color(0xFF0B1120) else Color(0xFFF4F7FB)
    val htmlOnDashboardBg = if (isDark) Color(0xFFF8FAFC) else Color(0xFF1E293B)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(htmlDashboardBg)
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
                            color = htmlOnDashboardBg,
                            fontSize = (26 * scaleFactor).sp,
                            lineHeight = (30 * scaleFactor).sp
                        )
                        Text(
                            text = if (config.connectionMode == ConnectionMode.TUNNEL) "Secure & Private" else "High-Performance Local Proxy",
                            style = MaterialTheme.typography.bodySmall,
                            color = htmlOnDashboardBg.copy(alpha = 0.7f),
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
                                tint = htmlOnDashboardBg.copy(alpha = 0.7f),
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
                // محاسبه سایز دقیق دکمه با حفظ تناسبات HTML
                val minDim = if (screenWidth < screenHeight) screenWidth else screenHeight
                val buttonWrapSize = (minDim * 0.75f).coerceIn(250.dp, 350.dp)
                
                WardenPowerButton(
                    connectionStatus = connectionStatus,
                    onToggle = onToggleVpn,
                    wrapSize = buttonWrapSize
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
                ProxyCopyRow("SOCKS5", socksAddress, { clipboardManager.setText(AnnotatedString(socksAddress)); onCopy(socksAddress) }, scaleFactor)
                ProxyCopyRow("HTTP", httpAddress, { clipboardManager.setText(AnnotatedString(httpAddress)); onCopy(httpAddress) }, scaleFactor)
            }
            VerticalDivider(modifier = Modifier.height(36.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
            IconButton(onClick = onHide, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun ProxyCopyRow(
    label: String, address: String, onCopy: () -> Unit, scaleFactor: Float
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
        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size((14 * scaleFactor).dp))
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
            ConnectionStatus.RUNNING -> Color(0xFF10B981) // HTML Connected Green
            ConnectionStatus.STARTING, ConnectionStatus.VALIDATING, ConnectionStatus.RECONNECTING, ConnectionStatus.STOPPING -> Color(0xFFF59E0B) // HTML Amber
            ConnectionStatus.ERROR -> appTheme.error
            ConnectionStatus.STOPPED -> defaultMutedColor
        },
        label = "statusColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("status_hero_card")
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
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
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        val protocolText = when (config.protocol) {
                            AetherProtocol.MASQUE -> if (config.h2Mode) "MASQUE (H2)" else "MASQUE (H3)"
                            AetherProtocol.GOOL -> "Gool"
                            else -> config.protocol.displayName
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
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
                            color = Color(0xFF10B981), 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height((12 * scaleFactor).dp))

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
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
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

/**
 * WardenPowerButton - 1000% Exact Match HTML Edition
 * Orbital Tracer, Liquid Glass, Touch Pad Ripple, Vibrant Glow
 */
@Composable
fun WardenPowerButton(
    connectionStatus: ConnectionStatus,
    onToggle: () -> Unit,
    wrapSize: Dp
) {
    val isRunning = connectionStatus == ConnectionStatus.RUNNING
    val isConnecting = connectionStatus == ConnectionStatus.STARTING || 
                       connectionStatus == ConnectionStatus.VALIDATING || 
                       connectionStatus == ConnectionStatus.RECONNECTING
    val isError = connectionStatus == ConnectionStatus.ERROR

    val isDark = isSystemInDarkTheme()
    val appTheme = LocalAppTheme.current
    val baseThemeColor = MaterialTheme.colorScheme.primary

    // 1. استخراج دقیق رنگ‌های استیت از HTML
    val cRing by animateColorAsState(
        targetValue = when {
            isRunning -> Color(0xFF10B981) // HTML Emerald
            isConnecting -> Color(0xFFF59E0B) // HTML Amber
            isError -> appTheme.error
            else -> baseThemeColor
        },
        animationSpec = tween(400),
        label = "cRing"
    )

    val cGlow by animateColorAsState(
        targetValue = when {
            isRunning -> Color(0xFF10B981).copy(alpha = 0.65f)
            isConnecting -> Color(0xFFF59E0B).copy(alpha = 0.65f)
            isError -> appTheme.error.copy(alpha = 0.65f)
            else -> baseThemeColor.copy(alpha = 0.65f)
        },
        animationSpec = tween(600, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)),
        label = "cGlow"
    )

    val cBg by animateColorAsState(
        targetValue = when {
            isRunning -> if (isDark) Color(0xFF10B981).copy(alpha = 0.1f) else Color(0xFFECFDF5)
            isConnecting -> if (isDark) Color(0xFFF59E0B).copy(alpha = 0.05f) else Color(0xFFFFFBEB)
            isError -> appTheme.error.copy(alpha = if (isDark) 0.1f else 0.05f)
            else -> if (isDark) baseThemeColor.copy(alpha = 0.05f) else Color(0xFFFFFFFF)
        },
        animationSpec = tween(400),
        label = "cBg"
    )

    // رنگ لیکویید گلس برای لایت و دارک مود
    val glassBgColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.6f)
    val glassBorderColor = Color.White.copy(alpha = 0.2f)

    // 2. مقیاس‌بندی دقیق و ریاضی ابعاد بر اساس فایل HTML شما (گرفته شده از نسبت‌های 320px)
    val glowSize = wrapSize * 1.125f     // 360px / 320px
    val glassSize = wrapSize * 0.875f    // 280px / 320px
    val spinRingSize = wrapSize * 0.8375f // 268px / 320px
    val coreRingSize = wrapSize * 0.75f  // 240px / 320px

    // 3. انیمیشن‌های Pulse و Orbital Tracer
    val infiniteTransition = rememberInfiniteTransition(label = "power_button")
    
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isConnecting) 0.9f else 1f,
        animationSpec = infiniteRepeatable(tween(750, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "iconScale"
    )
    val iconAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isConnecting) 0.6f else 1f,
        animationSpec = infiniteRepeatable(tween(750, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "iconAlpha"
    )

    // Orbital Rotate: 0 -> 360 (2s linear)
    val orbitRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "orbitRotation"
    )

    // Orbital Dash (کش آمدن و جمع شدن مدار چرخنده)
    val orbitPhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "orbitPhase"
    )

    // تبدیل Phase به زوایای کش آمدن مدار (Sweep Angle)
    val sweepAngle = if (orbitPhase < 0.5f) {
        2f + (118f * (orbitPhase * 2f)) // 2 to 120
    } else {
        120f - (118f * ((orbitPhase - 0.5f) * 2f)) // 120 to 2
    }
    // حرکت رو به عقب خط (Offset)
    val offsetAngle = -(320f * orbitPhase)

    // 4. ساخت مسیرهای برداری (SVG) برای لوگوی W داخل Shield 
    val shieldPath = remember {
        Path().apply {
            moveTo(12f, 22f)
            cubicTo(12f, 22f, 20f, 18f, 20f, 10f)
            lineTo(20f, 5f)
            lineTo(12f, 2f)
            lineTo(4f, 5f)
            lineTo(4f, 10f)
            cubicTo(4f, 18f, 12f, 22f, 12f, 22f)
            close()
        }
    }
    val wPath = remember {
        Path().apply {
            moveTo(7.5f, 9.5f)
            lineTo(10f, 15f)
            lineTo(12f, 11f)
            lineTo(14f, 15f)
            lineTo(16.5f, 9.5f)
        }
    }

    Box(
        modifier = Modifier.size(glowSize),
        contentAlignment = Alignment.Center
    ) {
        // [ لایه اول: Vibrant Glow / هاله نورانی ] (v1-glow)
        Box(
            modifier = Modifier
                .size(glowSize)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(cGlow, Color.Transparent),
                        radius = with(LocalDensity.current) { (glowSize.toPx() / 2f) * 0.75f }
                    )
                )
        )

        // Wrapper اصلی (320px معادل در HTML)
        Box(
            modifier = Modifier.size(wrapSize),
            contentAlignment = Alignment.Center
        ) {
            // [ لایه دوم: Liquid Glass ] (v1-glass)
            Box(
                modifier = Modifier
                    .size(glassSize)
                    .clip(CircleShape)
                    .background(glassBgColor)
                    .border(1.dp, glassBorderColor, CircleShape)
            )

            // [ لایه سوم: Orbital Tracer / انیمیشن مدار چرخان ] (v1-spin-ring)
            if (isConnecting) {
                Canvas(modifier = Modifier.size(spinRingSize)) {
                    val strokeWidthPx = 4.dp.toPx() // Stroke width = 4
                    rotate(orbitRotation) {
                        // یک سایه ملایم همرنگِ خودِ خط برای افکت drop-shadow در HTML
                        drawArc(
                            color = cRing.copy(alpha = 0.35f),
                            startAngle = offsetAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = strokeWidthPx * 2.5f, cap = StrokeCap.Round)
                        )
                        // خط اصلی چرخنده
                        drawArc(
                            color = cRing,
                            startAngle = offsetAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                        )
                    }
                }
            }

            // [ تاچ‌پد: سیستم ریپل کاملاً منطبق بر e.clientX (دقیقاً در نقطه لمس) ]
            val ripples = remember { mutableStateListOf<Pair<Animatable<Float, AnimationVector1D>, Offset>>() }
            val rippleScope = rememberCoroutineScope()

            Box(
                modifier = Modifier
                    .size(coreRingSize)
                    .shadow(12.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.25f))
                    .clip(CircleShape)
                    .background(cBg)
                    .border(3.dp, cRing, CircleShape)
                    .pointerInput(Unit) {
                        // گرفتن مختصات دقیق لمس کاربر!
                        detectTapGestures(
                            onTap = { touchOffset ->
                                val rippleAnim = Animatable(1f)
                                ripples.add(rippleAnim to touchOffset)
                                rippleScope.launch {
                                    // Scale 1 to 15, duration 700ms
                                    rippleAnim.animateTo(
                                        targetValue = 15f,
                                        animationSpec = tween(700, easing = CubicBezierEasing(0f, 0f, 0.2f, 1f))
                                    )
                                    ripples.remove(rippleAnim to touchOffset)
                                }
                                onToggle()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // رسم انیمیشن ریپل سفارشی که از زیر انگشت باز می‌شود
                Canvas(modifier = Modifier.fillMaxSize()) {
                    ripples.forEach { (anim, touchOffset) ->
                        // محاسبه Fade out: از 0.5 به 0 وقتی بزرگ می‌شود
                        val alpha = (0.5f * (1f - (anim.value - 1f) / 14f)).coerceIn(0f, 0.5f)
                        val radius = (size.width / 2f) * anim.value
                        drawCircle(
                            color = cRing,
                            radius = radius,
                            center = touchOffset,
                            alpha = alpha
                        )
                    }
                }

                // محتوای مرکزی (Icon و Text) با افکت Pulse
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                        alpha = iconAlpha
                    }
                ) {
                    // W Shield Icon (v1-icon)
                    Canvas(modifier = Modifier.size(coreRingSize * 0.27f)) {
                        val s = size.width / 24f
                        withTransform({ scale(s, s, Offset.Zero) }) {
                            drawPath(
                                path = shieldPath,
                                color = cRing,
                                style = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                                alpha = 0.85f
                            )
                            drawPath(
                                path = wPath,
                                color = cRing,
                                style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(coreRingSize * 0.06f))
                    
                    // (v1-label)
                    Text(
                        text = when {
                            isConnecting -> "CONNECTING"
                            isRunning -> "CONNECTED" // دقیقا کلمه‌ی Connected در HTML 
                            else -> "CONNECT"
                        },
                        color = cRing,
                        fontSize = (coreRingSize.value * 0.075f).sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        maxLines = 1,
                        textAlign = TextAlign.Center
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
    val protocols = AetherProtocol.values().filter { it != AetherProtocol.ZERO_TRUST }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        protocols.forEach { protocol ->
            val isSelected = protocol == selectedProtocol
            val bgColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, label = "")
            val contentColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, label = "")
            
            val displayText = if (protocol == AetherProtocol.GOOL) "Gool" else protocol.displayName
            
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
                    text = displayText,
                    color = contentColor,
                    fontSize = (12 * scaleFactor).sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
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
