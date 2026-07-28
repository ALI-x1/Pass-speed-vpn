package io.github.immaghzbad.aetherst.data

import android.content.Context
import android.content.SharedPreferences
import io.github.immaghzbad.aetherst.model.AetherConfig
import io.github.immaghzbad.aetherst.model.AetherIpMode
import io.github.immaghzbad.aetherst.model.AetherLogLevel
import io.github.immaghzbad.aetherst.model.AetherNoise
import io.github.immaghzbad.aetherst.model.AetherProtocol
import io.github.immaghzbad.aetherst.model.AetherScanMode
import io.github.immaghzbad.aetherst.model.OnboardingStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AetherConfigRepository private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("aether_prefs", Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<AetherConfig> = _config.asStateFlow()

    private val _isOnboardingComplete = MutableStateFlow(prefs.getBoolean("onboarding_complete", false))
    val isOnboardingComplete: StateFlow<Boolean> = _isOnboardingComplete.asStateFlow()

    companion object {
        @Volatile
        private var INSTANCE: AetherConfigRepository? = null

        fun getInstance(context: Context): AetherConfigRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AetherConfigRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        LogRepository.currentAppLogLevel = _config.value.appLogLevel
        LogRepository.currentCoreLogLevel = _config.value.coreLogLevel
    }

    private fun loadConfig(): AetherConfig {
        return readFromPrefs("")
    }

    private fun loadManualConfig(): AetherConfig {
        return readFromPrefs("manual_")
    }

    private fun readFromPrefs(prefix: String): AetherConfig {
        val protocolStr = prefs.getString("${prefix}protocol", AetherProtocol.MASQUE.name) ?: AetherProtocol.MASQUE.name
        val noiseStr = prefs.getString("${prefix}noise", AetherNoise.FIREWALL.name) ?: AetherNoise.FIREWALL.name
        val scanModeStr = prefs.getString("${prefix}scan_mode", AetherScanMode.BALANCED.name) ?: AetherScanMode.BALANCED.name
        val ipModeStr = prefs.getString("${prefix}ip_mode", AetherIpMode.IPV4.name) ?: AetherIpMode.IPV4.name
        val appLogLevelStr = prefs.getString("${prefix}app_log_level", AetherLogLevel.INFO.name) ?: AetherLogLevel.INFO.name
        val coreLogLevelStr = prefs.getString("${prefix}core_log_level", AetherLogLevel.OFF.name) ?: AetherLogLevel.OFF.name
        val presetId = prefs.getString("${prefix}preset_id", "custom") ?: "custom"

        val socksHost = prefs.getString("${prefix}socks_host", "127.0.0.1") ?: "127.0.0.1"
        val cleanHost = if (socksHost == "198.18.0.1") "127.0.0.1" else socksHost
        
        return AetherConfig(
            presetId = presetId,
            protocol = runCatching { AetherProtocol.valueOf(protocolStr) }.getOrDefault(AetherProtocol.MASQUE),
            noise = runCatching { AetherNoise.valueOf(noiseStr) }.getOrDefault(AetherNoise.FIREWALL),
            scanMode = runCatching { AetherScanMode.valueOf(scanModeStr) }.getOrDefault(AetherScanMode.BALANCED),
            ipMode = runCatching { AetherIpMode.valueOf(ipModeStr) }.getOrDefault(AetherIpMode.IPV4),
            h2Mode = prefs.getBoolean("${prefix}h2_mode", true),
            h2Fragment = prefs.getBoolean("${prefix}h2_fragment", false),
            fragmentSize = prefs.getString("${prefix}fragment_size", "16-32") ?: "16-32",
            fragmentDelay = prefs.getString("${prefix}fragment_delay", "2-10") ?: "2-10",
            noDataCheck = prefs.getBoolean("${prefix}no_data_check", false),
            quickReconnect = prefs.getBoolean("${prefix}quick_reconnect", true),
            socksHost = cleanHost,
            socksPort = prefs.getString("${prefix}socks_port", "1819") ?: "1819",
            appLogLevel = runCatching { AetherLogLevel.valueOf(appLogLevelStr) }.getOrDefault(AetherLogLevel.INFO),
            coreLogLevel = runCatching { AetherLogLevel.valueOf(coreLogLevelStr) }.getOrDefault(AetherLogLevel.OFF),
            peer = prefs.getString("${prefix}peer", "") ?: "",
            keepalive = prefs.getInt("${prefix}keepalive", 5),
            validateSecs = prefs.getInt("${prefix}validate_secs", 10),
            reconnectSecs = prefs.getInt("${prefix}reconnect_secs", 2),
            noProfileRetry = prefs.getBoolean("${prefix}no_profile_retry", false),
            tlsGroups = prefs.getString("${prefix}tls_groups", "") ?: "",
            mtu = prefs.getInt("${prefix}mtu", 1100),
            proxyOnly = prefs.getBoolean("${prefix}proxy_only", false),
            excludedPackages = prefs.getStringSet("${prefix}excluded_packages", emptySet()) ?: emptySet()
        )
    }

    fun updateConfig(newConfig: AetherConfig) {
        val manualConfig = newConfig.copy(presetId = "custom")
        saveToPrefs("", manualConfig)
        saveToPrefs("manual_", manualConfig)
        LogRepository.currentAppLogLevel = manualConfig.appLogLevel
        LogRepository.currentCoreLogLevel = manualConfig.coreLogLevel
        _config.value = manualConfig
    }

    fun setOnboardingComplete(complete: Boolean) {
        prefs.edit().putBoolean("onboarding_complete", complete).apply()
        _isOnboardingComplete.value = complete
    }

    fun getOnboardingStep(): OnboardingStep {
        val name = prefs.getString("onboarding_step_name", OnboardingStep.WELCOME.name) ?: OnboardingStep.WELCOME.name
        return try { OnboardingStep.valueOf(name) } catch (_: Exception) { OnboardingStep.WELCOME }
    }

    fun setOnboardingStep(step: OnboardingStep) {
        prefs.edit().putString("onboarding_step_name", step.name).apply()
    }

    fun getLastDismissedUpdate(): String {
        return prefs.getString("last_dismissed_update", "") ?: ""
    }

    fun setLastDismissedUpdate(version: String) {
        prefs.edit().putString("last_dismissed_update", version).apply()
    }

    private fun saveToPrefs(prefix: String, cfg: AetherConfig) {
        prefs.edit().apply {
            putString("${prefix}preset_id", cfg.presetId)
            putString("${prefix}protocol", cfg.protocol.name)
            putString("${prefix}noise", cfg.noise.name)
            putString("${prefix}scan_mode", cfg.scanMode.name)
            putString("${prefix}ip_mode", cfg.ipMode.name)
            putBoolean("${prefix}h2_mode", cfg.h2Mode)
            putBoolean("${prefix}h2_fragment", cfg.h2Fragment)
            putString("${prefix}fragment_size", cfg.fragmentSize)
            putString("${prefix}fragment_delay", cfg.fragmentDelay)
            putBoolean("${prefix}no_data_check", cfg.noDataCheck)
            putBoolean("${prefix}quick_reconnect", cfg.quickReconnect)
            putString("${prefix}socks_host", cfg.socksHost)
            putString("${prefix}socks_port", cfg.socksPort)
            putString("${prefix}app_log_level", cfg.appLogLevel.name)
            putString("${prefix}core_log_level", cfg.coreLogLevel.name)
            putString("${prefix}peer", cfg.peer)
            putInt("${prefix}keepalive", cfg.keepalive)
            putInt("${prefix}validate_secs", cfg.validateSecs)
            putInt("${prefix}reconnect_secs", cfg.reconnectSecs)
            putBoolean("${prefix}no_profile_retry", cfg.noProfileRetry)
            putString("${prefix}tls_groups", cfg.tlsGroups)
            putInt("${prefix}mtu", cfg.mtu)
            putBoolean("${prefix}proxy_only", cfg.proxyOnly)
            putStringSet("${prefix}excluded_packages", cfg.excludedPackages)
            apply()
        }
    }

    fun applyPreset(presetId: String) {
        val current = _config.value
        val updated = when (presetId) {
            "custom" -> loadManualConfig()
            "bypass_udp" -> current.copy(
                presetId = "bypass_udp",
                protocol = AetherProtocol.MASQUE,
                noise = AetherNoise.FIREWALL,
                scanMode = AetherScanMode.BALANCED,
                h2Mode = true,
                h2Fragment = true,
                fragmentSize = "16-32",
                fragmentDelay = "2-10",
                noDataCheck = false,
                tlsGroups = "",
                mtu = 1100,
                proxyOnly = false
            )
            "ironclad_stealth" -> current.copy(
                presetId = "ironclad_stealth",
                protocol = AetherProtocol.MASQUE,
                noise = AetherNoise.GFW,
                scanMode = AetherScanMode.IRONCLAD,
                h2Mode = false,
                h2Fragment = false,
                noDataCheck = false,
                tlsGroups = "",
                mtu = 1100,
                proxyOnly = false
            )
            "turbo_wg" -> current.copy(
                presetId = "turbo_wg",
                protocol = AetherProtocol.WG,
                noise = AetherNoise.BALANCED,
                scanMode = AetherScanMode.TURBO,
                noDataCheck = true,
                h2Mode = false,
                h2Fragment = false,
                mtu = 1100,
                proxyOnly = false
            )
            else -> current
        }
        saveToPrefs("", updated)
        LogRepository.currentAppLogLevel = updated.appLogLevel
        LogRepository.currentCoreLogLevel = updated.coreLogLevel
        _config.value = updated
    }
}
