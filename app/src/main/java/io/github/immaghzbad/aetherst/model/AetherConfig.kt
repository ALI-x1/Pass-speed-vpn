package io.github.immaghzbad.aetherst.model

enum class AetherProtocol(val rawValue: String, val displayName: String, val description: String) {
    MASQUE("masque", "MASQUE", "HTTP/2/3 Tunneling (MASQUE)"),
    WG("wg", "WireGuard", "Lean speed WireGuard tunnel"),
    GOOL("gool", "Gool (WG-in-WG)", "Double encryption WireGuard-in-WireGuard")
}

enum class AetherNoise(val rawValue: String, val displayName: String, val category: String) {
    FIREWALL("firewall", "Firewall (Strict Censorship)", "masque"),
    GFW("gfw", "GFW (Heavy DPI Obfuscation)", "masque"),
    OFF("off", "Off (No Noise)", "all"),
    BALANCED("balanced", "Balanced Stealth & Speed", "wg"),
    AGGRESSIVE("aggressive", "Aggressive Decoy Packets", "wg"),
    LIGHT("light", "Light Overhead", "wg")
}

enum class AetherScanMode(val rawValue: String, val displayName: String, val description: String) {
    TURBO("turbo", "Turbo Scan", "Fastest endpoint match"),
    BALANCED("balanced", "Balanced Scan", "Optimal speed & reliability"),
    THOROUGH("thorough", "Thorough Optimization", "Deep ping & latency optimization"),
    STEALTH("stealth", "Stealth Quiet Scan", "Quiet slow scanning"),
    IRONCLAD("ironclad", "Ironclad Probe Verification", "Full end-to-end HTTP data probe verification")
}

enum class AetherIpMode(val rawValue: String, val displayName: String) {
    IPV4("IPv4", "IPv4 Only"),
    IPV6("IPv6", "IPv6 Only"),
    DUAL("Dual", "Dual Stack (4+6)")
}

enum class AetherLogLevel(val rawValue: String, val displayName: String) {
    OFF("off", "Off (Disabled - Default, Zero RAM Overhead)"),
    ERROR("error", "Error Only"),
    WARN("warn", "Warning & Error"),
    INFO("info", "Info, Warn & Error"),
    DEBUG("debug", "Debug (All Verbose Output)")
}

enum class ConnectionState {
    DISCONNECTED,
    SCANNING,
    VALIDATING,
    CONNECTED,
    RECONNECTING,
    DISCONNECTING,
    ERROR
}

data class AetherConfig(
    val presetId: String = "custom",
    val protocol: AetherProtocol = AetherProtocol.MASQUE,
    val noise: AetherNoise = AetherNoise.FIREWALL,
    val scanMode: AetherScanMode = AetherScanMode.BALANCED,
    val ipMode: AetherIpMode = AetherIpMode.IPV4,
    val h2Mode: Boolean = true,
    val h2Fragment: Boolean = false,
    val fragmentSize: String = "16-32",
    val fragmentDelay: String = "2-10",
    val noDataCheck: Boolean = false,
    val quickReconnect: Boolean = true,
    val socksHost: String = "127.0.0.1",
    val socksPort: String = "1819",
    val appLogLevel: AetherLogLevel = AetherLogLevel.INFO,
    val coreLogLevel: AetherLogLevel = AetherLogLevel.OFF,
    val peer: String = "",
    val keepalive: Int = 5,
    val validateSecs: Int = 10,
    val reconnectSecs: Int = 2,
    val noProfileRetry: Boolean = false,
    val tlsGroups: String = "",
    val mtu: Int = 1100,
    val proxyOnly: Boolean = false,
    val excludedPackages: Set<String> = emptySet()
)
