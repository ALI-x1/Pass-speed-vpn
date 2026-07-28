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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val IosCardBg = Color(0xFF1C1C1E)
private val IosActiveBlue = Color(0xFF007AFF)
private val IosActiveGreen = Color(0xFF34C759)
private val IosSecondaryLabel = Color(0xFF8E8E93)
private const val UserGithubUrl = "https://github.com/immaghzbad"
private const val AetherRepositoryUrl = "https://github.com/CluvexStudio/Aether"
private const val HevRepositoryUrl = "https://github.com/heiher/hev-socks5-tunnel"
private const val DeveloperTelegramUrl = "https://t.me/PowerSigma"

@Composable
fun AboutUsScreen(
    bottomContentPadding: Dp = 0.dp
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val screenWidth = this.maxWidth
        val scaleFactor = (screenWidth.value / 411f).coerceIn(0.7f, 1.1f)
        val horizontalPadding = if (screenWidth < 360.dp) 12.dp else 16.dp

        val uriHandler = LocalUriHandler.current
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = horizontalPadding,
                top = 20.dp,
                end = horizontalPadding,
                bottom = bottomContentPadding + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy((16 * scaleFactor).dp)
        ) {
            item { AboutHero(scaleFactor = scaleFactor) }
            item {
                AboutInfoCard(
                    icon = Icons.Default.Info,
                    iconColor = IosActiveBlue,
                    title = "The AetherST Experience",
                    scaleFactor = scaleFactor
                ) {
                    Text(
                        text = "AetherST serves as a high-performance Android gateway to the Aether ecosystem. It abstracts the underlying complexity of advanced tunneling protocols into a refined, user-centric interface. By offering granular connection controls, real-time telemetry, and intelligent configuration presets, it ensures that establishing a secure and stable digital tunnel is seamless, even for non-technical users.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IosSecondaryLabel,
                        lineHeight = (22 * scaleFactor).sp,
                        fontSize = (14 * scaleFactor).sp
                    )
                }
            }
            item {
                AboutInfoCard(
                    icon = Icons.Default.Shield,
                    iconColor = IosActiveGreen,
                    title = "Core Architecture: Aether",
                    scaleFactor = scaleFactor
                ) {
                    Text(
                        text = "Aether is a cutting-edge, open-source proxy core engineered to thrive in the world's most restrictive network environments. Unlike traditional VPNs, Aether focuses on advanced censorship circumvention through dynamic gateway discovery and rigorous data-plane validation. It utilizes sophisticated noise obfuscation and multiple transport layers to mask traffic signatures, ensuring your connection remains both invisible and resilient.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IosSecondaryLabel,
                        lineHeight = (22 * scaleFactor).sp,
                        fontSize = (14 * scaleFactor).sp
                    )
                    Spacer(modifier = Modifier.height((14 * scaleFactor).dp))
                    FeatureRow(icon = Icons.Default.Language, title = "Censorship-Resistant", description = "Specifically designed to bypass Deep Packet Inspection (DPI) and protocol-based blocking.", scaleFactor = scaleFactor)
                    FeatureRow(icon = Icons.Default.Security, title = "Hybrid Transports", description = "Support for MASQUE (HTTP/3 & HTTP/2), WireGuard, and cascaded tunnel architectures.", scaleFactor = scaleFactor)
                    FeatureRow(icon = Icons.Default.NetworkCheck, title = "Adaptive Validation", description = "Mandatory traffic probes verify gateway integrity before any data is routed through the tunnel.", scaleFactor = scaleFactor)
                    FeatureRow(icon = Icons.Default.Bolt, title = "Dynamic Recovery", description = "Heuristic-based reconnection logic automatically switches to optimal gateways during network shifts.", scaleFactor = scaleFactor)
                }
            }
            item {
                AboutInfoCard(
                    icon = Icons.Default.Memory,
                    iconColor = Color(0xFFAF52DE),
                    title = "Native Engine: HEV Tunnel",
                    scaleFactor = scaleFactor
                ) {
                    Text(
                        text = "The HEV SOCKS5 Tunnel is the critical high-speed link between the Android system's virtual network interface (TUN) and the Aether proxy core. Built using a specialized native user-space network stack, it provides near-zero overhead translation of device-wide IP packets into SOCKS5 sessions. This native implementation drastically reduces CPU cycles and memory usage compared to high-level bridges.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IosSecondaryLabel,
                        lineHeight = (22 * scaleFactor).sp,
                        fontSize = (14 * scaleFactor).sp
                    )
                    Spacer(modifier = Modifier.height((14 * scaleFactor).dp))
                    FeatureRow(icon = Icons.Default.SwapVert, title = "Full-Stack Interception", description = "Seamlessly handles TCP, UDP, and DNS traffic across the entire Android operating system.", scaleFactor = scaleFactor)
                    FeatureRow(icon = Icons.Default.Bolt, title = "C-Native Performance", description = "Leverages low-level optimizations to ensure maximum throughput with minimal battery drain.", scaleFactor = scaleFactor)
                    FeatureRow(icon = Icons.Default.Security, title = "Verified Reliability", description = "Integrated from the industry-standard hev-socks5-tunnel for mission-critical stability.", scaleFactor = scaleFactor)
                }
            }
            item {
                AboutInfoCard(
                    icon = Icons.Default.Code,
                    iconColor = IosActiveBlue,
                    title = "Open Source & Community",
                    scaleFactor = scaleFactor
                ) {
                    AboutLinkCard(
                        title = "Lead Developer",
                        subtitle = "Follow immaghzbad for updates and new releases",
                        url = UserGithubUrl,
                        urlColor = Color.White,
                        onClick = { uriHandler.openUri(UserGithubUrl) },
                        scaleFactor = scaleFactor
                    )
                    Spacer(modifier = Modifier.height((8 * scaleFactor).dp))
                    AboutLinkCard(
                        title = "Official Community",
                        subtitle = "Join the PowerSigma Telegram channel for support",
                        url = DeveloperTelegramUrl,
                        urlColor = Color(0xFF2AABEE),
                        onClick = { uriHandler.openUri(DeveloperTelegramUrl) },
                        scaleFactor = scaleFactor
                    )
                    Spacer(modifier = Modifier.height((8 * scaleFactor).dp))
                    AboutLinkCard(
                        title = "Aether Core Source",
                        subtitle = "Browse the main tunnel logic on GitHub",
                        url = AetherRepositoryUrl,
                        urlColor = IosActiveBlue,
                        onClick = { uriHandler.openUri(AetherRepositoryUrl) },
                        scaleFactor = scaleFactor
                    )
                    Spacer(modifier = Modifier.height((8 * scaleFactor).dp))
                    AboutLinkCard(
                        title = "HEV Engine Source",
                        subtitle = "Explore the native network stack implementation",
                        url = HevRepositoryUrl,
                        urlColor = Color(0xFFAF52DE),
                        onClick = { uriHandler.openUri(HevRepositoryUrl) },
                        scaleFactor = scaleFactor
                    )
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = IosCardBg)
                ) {
                    Column(
                        modifier = Modifier.padding((16 * scaleFactor).dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Built with ", color = IosSecondaryLabel, fontSize = (13 * scaleFactor).sp)
                            Icon(imageVector = Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFFF375F), modifier = Modifier.size((16 * scaleFactor).dp))
                            Text(text = " by PowerSigma Team", color = IosSecondaryLabel, fontSize = (13 * scaleFactor).sp)
                        }
                        Spacer(modifier = Modifier.height((8 * scaleFactor).dp))
                        Text(
                            text = "AetherST is an independent client project. Aether core is developed by CluvexStudio and distributed under its own open-source license.",
                            style = MaterialTheme.typography.bodySmall,
                            color = IosSecondaryLabel,
                            textAlign = TextAlign.Center,
                            lineHeight = (18 * scaleFactor).sp,
                            fontSize = (11 * scaleFactor).sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutHero(scaleFactor: Float = 1f) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape((28 * scaleFactor).dp),
        colors = CardDefaults.cardColors(containerColor = IosCardBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            IosActiveBlue.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = (24 * scaleFactor).dp, horizontal = (20 * scaleFactor).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "AetherST",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontSize = (32 * scaleFactor).sp
                )
                Text(
                    text = "Advanced Secure Tunneling Client",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    fontSize = (13 * scaleFactor).sp
                )

                Spacer(modifier = Modifier.height((20 * scaleFactor).dp))

                Surface(
                    shape = RoundedCornerShape((12 * scaleFactor).dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = (12 * scaleFactor).dp, vertical = (8 * scaleFactor).dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp)
                    ) {
                        VersionText(label = "App", value = "1.1.0", color = IosActiveBlue, scaleFactor = scaleFactor)
                        Text("•", color = Color.White.copy(alpha = 0.2f), fontSize = (12 * scaleFactor).sp)
                        VersionText(label = "Aether", value = "1.4.0", color = IosActiveGreen, scaleFactor = scaleFactor)
                        Text("•", color = Color.White.copy(alpha = 0.2f), fontSize = (12 * scaleFactor).sp)
                        VersionText(label = "Hev", value = "2.15.0", color = Color(0xFFAF52DE), scaleFactor = scaleFactor)
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionText(label: String, value: String, color: Color, scaleFactor: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "$label ", color = IosSecondaryLabel, fontSize = (10 * scaleFactor).sp, fontWeight = FontWeight.Medium)
        Text(text = value, color = color, fontSize = (11 * scaleFactor).sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AboutInfoCard(icon: ImageVector, iconColor: Color, title: String, scaleFactor: Float = 1f, content: @Composable () -> Unit) {
    var expanded by rememberSaveable(title) { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape((20 * scaleFactor).dp),
        colors = CardDefaults.cardColors(containerColor = IosCardBg)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding((18 * scaleFactor).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(modifier = Modifier.size((40 * scaleFactor).dp), shape = RoundedCornerShape((12 * scaleFactor).dp), color = iconColor.copy(alpha = 0.18f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size((22 * scaleFactor).dp))
                    }
                }
                Spacer(modifier = Modifier.width((12 * scaleFactor).dp))
                Text(text = title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White, fontSize = (16 * scaleFactor).sp)
                Icon(imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = iconColor, modifier = Modifier.size((24 * scaleFactor).dp))
            }
            AnimatedVisibility(visible = expanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Column(modifier = Modifier.padding(start = (18 * scaleFactor).dp, end = (18 * scaleFactor).dp, bottom = (18 * scaleFactor).dp)) { content() }
            }
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, description: String, scaleFactor: Float = 1f) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = (8 * scaleFactor).dp), verticalAlignment = Alignment.Top) {
        Icon(imageVector = icon, contentDescription = null, tint = IosActiveBlue, modifier = Modifier.padding(top = (2 * scaleFactor).dp).size((18 * scaleFactor).dp))
        Spacer(modifier = Modifier.width((12 * scaleFactor).dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = (14 * scaleFactor).sp)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = IosSecondaryLabel, lineHeight = (18 * scaleFactor).sp, fontSize = (12 * scaleFactor).sp)
        }
    }
}

@Composable
private fun AboutLinkCard(title: String, subtitle: String, url: String, urlColor: Color, onClick: () -> Unit, scaleFactor: Float = 1f) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape((16 * scaleFactor).dp),
        colors = CardDefaults.cardColors(containerColor = IosCardBg)
    ) {
        Column(modifier = Modifier.padding(horizontal = (18 * scaleFactor).dp, vertical = (14 * scaleFactor).dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = (15 * scaleFactor).sp)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = IosSecondaryLabel, fontSize = (12 * scaleFactor).sp)
            Spacer(modifier = Modifier.height((4 * scaleFactor).dp))
            Text(text = url, style = MaterialTheme.typography.labelSmall, color = urlColor, maxLines = 1, fontSize = (10 * scaleFactor).sp)
        }
    }
}
