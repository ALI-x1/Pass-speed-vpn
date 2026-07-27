package io.github.immaghzbad.aetherst.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
private const val DeveloperTelegramUrl = "https://t.me/PowerSigma"

@Composable
fun AboutUsScreen(
    bottomContentPadding: Dp = 0.dp
) {
    val uriHandler = LocalUriHandler.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 20.dp,
            end = 16.dp,
            bottom = bottomContentPadding + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { AboutHero() }
        item {
            AboutInfoCard(
                icon = Icons.Default.Info,
                iconColor = IosActiveBlue,
                title = "A simpler way to use Aether"
            ) {
                Text(
                    text = "AetherST is an Android client experience powered by the Aether core. It brings connection controls, protocol selection, status monitoring, diagnostics, and practical presets into one clear interface so users can establish and manage a tunnel with less manual setup.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = IosSecondaryLabel,
                    lineHeight = 22.sp
                )
            }
        }
        item {
            AboutInfoCard(
                icon = Icons.Default.Shield,
                iconColor = IosActiveGreen,
                title = "What is Aether?"
            ) {
                Text(
                    text = "Aether is an open-source censorship-circumvention and user-space proxy core designed for heavily restricted networks. It discovers reachable gateways, validates that traffic can really pass through them, creates an encrypted tunnel, and provides connectivity through a local SOCKS5 proxy.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = IosSecondaryLabel,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                FeatureRow(icon = Icons.Default.Language, title = "Restriction-aware", description = "Built for DPI, protocol fingerprinting, endpoint blocking, and UDP throttling.")
                FeatureRow(icon = Icons.Default.Security, title = "Multiple transports", description = "Supports MASQUE over HTTP/3 or HTTP/2, WireGuard, and nested WireGuard.")
                FeatureRow(icon = Icons.Default.NetworkCheck, title = "Real connection checks", description = "Data-plane validation tests actual traffic before a gateway is trusted.")
                FeatureRow(icon = Icons.Default.Bolt, title = "Faster recovery", description = "Automatic reconnection and quick reconnect reuse a recently working gateway when possible.")
            }
        }
        item {
            AboutInfoCard(
                icon = Icons.Default.Memory,
                iconColor = Color(0xFFAF52DE),
                title = "What is HEV SOCKS5 Tunnel?"
            ) {
                Text(
                    text = "HEV SOCKS5 Tunnel is the native packet engine between Android's TUN interface and Aether's local SOCKS5 server. It uses a mature user-space network stack to translate full-device IP traffic into efficient SOCKS5 TCP and UDP sessions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = IosSecondaryLabel,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                FeatureRow(icon = Icons.Default.SwapVert, title = "Full-device forwarding", description = "Carries concurrent TCP, UDP, and DNS traffic from Android's VPN interface to Aether.")
                FeatureRow(icon = Icons.Default.Bolt, title = "Native performance", description = "Reduces packet copies, thread pressure, and garbage collection compared with a custom Kotlin bridge.")
                FeatureRow(icon = Icons.Default.Security, title = "Open-source engine", description = "Integrated from heiher/hev-socks5-tunnel and distributed under the MIT license.")
            }
        }
        item {
            AboutInfoCard(
                icon = Icons.Default.Code,
                iconColor = IosActiveBlue,
                title = "Projects & Community"
            ) {
                AboutLinkCard(
                    icon = Icons.Default.Person,
                    iconColor = Color.White,
                    title = "Developer on GitHub",
                    subtitle = "Follow immaghzbad for more projects",
                    url = UserGithubUrl,
                    onClick = { uriHandler.openUri(UserGithubUrl) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                AboutLinkCard(
                    icon = Icons.AutoMirrored.Filled.Send,
                    iconColor = Color(0xFF2AABEE),
                    title = "PowerSigma Team",
                    subtitle = "Join the developer's Telegram channel",
                    url = DeveloperTelegramUrl,
                    onClick = { uriHandler.openUri(DeveloperTelegramUrl) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                AboutLinkCard(
                    icon = Icons.Default.Code,
                    iconColor = IosActiveBlue,
                    title = "Aether Core on GitHub",
                    subtitle = "Source code, releases, and documentation",
                    url = AetherRepositoryUrl,
                    onClick = { uriHandler.openUri(AetherRepositoryUrl) }
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
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Built with ", color = IosSecondaryLabel, fontSize = 13.sp)
                        Icon(imageVector = Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFFF375F), modifier = Modifier.size(16.dp))
                        Text(text = " by PowerSigma Team", color = IosSecondaryLabel, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "AetherST is an independent client project. Aether core is developed by CluvexStudio and distributed under its own open-source license.",
                        style = MaterialTheme.typography.bodySmall,
                        color = IosSecondaryLabel,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutHero() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = IosCardBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            IosActiveBlue.copy(alpha = 0.35f),
                            IosActiveBlue.copy(alpha = 0.05f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(IosActiveBlue, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(50.dp), tint = Color.White)
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(text = "AetherST", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(text = "Advanced Tunnel Client", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VersionBadge(label = "App", version = "1.0.0", color = IosActiveBlue)
                        VersionBadge(label = "Core", version = "1.4.0", color = IosActiveGreen)
                    }
                    VersionBadge(label = "Engine", version = "2.15.0", color = Color(0xFFAF52DE))
                }
            }
        }
    }
}

@Composable
private fun VersionBadge(label: String, version: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = label, color = color.copy(alpha = 0.9f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(text = version, color = color, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun AboutInfoCard(icon: ImageVector, iconColor: Color, title: String, content: @Composable () -> Unit) {
    var expanded by rememberSaveable(title) { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = IosCardBg)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(12.dp), color = iconColor.copy(alpha = 0.18f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Icon(imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            AnimatedVisibility(visible = expanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 18.dp)) { content() }
            }
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, description: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Icon(imageVector = icon, contentDescription = null, tint = IosActiveBlue, modifier = Modifier.padding(top = 2.dp).size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = IosSecondaryLabel, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun AboutLinkCard(icon: ImageVector, iconColor: Color, title: String, subtitle: String, url: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = IosCardBg)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(42.dp), shape = RoundedCornerShape(12.dp), color = iconColor.copy(alpha = 0.16f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = IosSecondaryLabel)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = url, style = MaterialTheme.typography.labelSmall, color = iconColor, maxLines = 1)
            }
        }
    }
}
