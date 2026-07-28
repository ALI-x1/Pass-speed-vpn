package io.github.immaghzbad.aetherst.ui

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.immaghzbad.aetherst.BuildConfig
import io.github.immaghzbad.aetherst.data.AetherConfigRepository
import io.github.immaghzbad.aetherst.data.IpInfo
import io.github.immaghzbad.aetherst.data.IpInfoRepository
import io.github.immaghzbad.aetherst.data.LogRepository
import io.github.immaghzbad.aetherst.data.PingRepository
import io.github.immaghzbad.aetherst.data.PingState
import io.github.immaghzbad.aetherst.model.*
import io.github.immaghzbad.aetherst.service.AetherVpnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicLong

class AetherViewModel(context: Context) : ViewModel() {

    private val repository = AetherConfigRepository.getInstance(context)
    private val lastToggleAt = AtomicLong(0)

    val config: StateFlow<AetherConfig> = repository.config
    val isOnboardingComplete: StateFlow<Boolean> = repository.isOnboardingComplete
    val connectionState: StateFlow<ConnectionState> = AetherVpnService.serviceState
    val elapsedSeconds: StateFlow<Long> = AetherVpnService.elapsedSeconds
    val sessionTraffic = AetherVpnService.sessionTraffic
    val logs: StateFlow<List<LogEntry>> = LogRepository.logs
    val ipInfo: StateFlow<IpInfo> = IpInfoRepository.ipInfo
    val pingState: StateFlow<PingState> = PingRepository.pingState

    private val _needVpnPermission = MutableStateFlow(false)

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    init {
        LogRepository.initialize(context)
        loadInstalledApps(context)
        checkForUpdates()
        viewModelScope.launch {

            IpInfoRepository.fetchIpInfo(useProxy = false)
            PingRepository.runPing(useProxy = false)

            connectionState.collect { state ->

                when (state) {
                    ConnectionState.CONNECTED -> {

                        val cfg = config.value
                        val host = cfg.socksHost
                        val port = cfg.socksPort.toIntOrNull() ?: 1819
                        LogRepository.i("[Health] Fetching public IP via SOCKS5 ($host:$port)", "UI")
                        IpInfoRepository.fetchIpInfo(host, port, useProxy = true)
                        PingRepository.runPing(host, port, useProxy = true)
                    }
                    ConnectionState.DISCONNECTED -> {

                        IpInfoRepository.fetchIpInfo(useProxy = false)
                        PingRepository.runPing(useProxy = false)
                    }
                    else -> {}
                }
            }
        }
    }

    fun toggleVpn(context: Context, onPermissionRequired: () -> Unit) {
        val now = SystemClock.elapsedRealtime()
        while (true) {
            val previous = lastToggleAt.get()
            if (now - previous < 450L) return
            if (lastToggleAt.compareAndSet(previous, now)) break
        }
        val currentState = connectionState.value
        if (currentState == ConnectionState.DISCONNECTING) return
        try {
            if (currentState == ConnectionState.DISCONNECTED || currentState == ConnectionState.ERROR) {
                val prepareIntent = VpnService.prepare(context)
                if (prepareIntent != null) {
                    _needVpnPermission.value = true
                    onPermissionRequired()
                    return
                }
                _needVpnPermission.value = false
                AetherVpnService.startVpn(context)
            } else {
                AetherVpnService.stopVpn(context)
            }
        } catch (exception: Exception) {
            LogRepository.e("[UI] Connection toggle failed: ${exception.localizedMessage}")
        }
    }

    fun updateConfig(newConfig: AetherConfig) {
        repository.updateConfig(newConfig)
    }

    fun toggleExcludedPackage(packageName: String) {
        val current = config.value.excludedPackages
        val newSet = if (current.contains(packageName)) {
            current - packageName
        } else {
            current + packageName
        }
        updateConfig(config.value.copy(excludedPackages = newSet))
    }

    private fun loadInstalledApps(context: Context) {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                val myPkg = context.packageName
                val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                packages.filter { it.packageName != myPkg }
                    .map { app ->
                        AppInfo(
                            name = pm.getApplicationLabel(app).toString(),
                            packageName = app.packageName,
                            icon = pm.getApplicationIcon(app),
                            isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        )
                    }.sortedBy { it.name.lowercase() }
            }
            _installedApps.value = apps
        }
    }

    fun applyPreset(presetId: String) {
        repository.applyPreset(presetId)
    }

    fun refreshIpInfo() {
        viewModelScope.launch {
            val state = connectionState.value
            if (state == ConnectionState.CONNECTED) {
                val cfg = config.value
                IpInfoRepository.fetchIpInfo(cfg.socksHost, cfg.socksPort.toIntOrNull() ?: 1819, useProxy = true)
            } else {
                IpInfoRepository.fetchIpInfo(useProxy = false)
            }
        }
    }

    fun refreshPing() {
        viewModelScope.launch {
            val state = connectionState.value
            if (state == ConnectionState.CONNECTED) {
                val cfg = config.value
                PingRepository.runPing(cfg.socksHost, cfg.socksPort.toIntOrNull() ?: 1819, useProxy = true)
            } else {
                PingRepository.runPing(useProxy = false)
            }
        }
    }

    fun clearLogs() {
        LogRepository.clear()
    }

    fun copyLogs(context: Context) {
        LogRepository.copyToClipboard(context)
    }

    fun dismissUpdate() {
        _updateInfo.value = null
    }

    private fun checkForUpdates() {
        viewModelScope.launch {
            try {
                val info = withContext(Dispatchers.IO) {
                    val url = URL("https://raw.githubusercontent.com/immaghzbad/AetherST/refs/heads/main/update.json")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    if (conn.responseCode == 200) {
                        val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(jsonStr)
                        UpdateInfo(
                            version = json.getString("version"),
                            versionCode = json.getInt("version_code"),
                            isBeta = json.getBoolean("is_beta"),
                            changelog = json.getString("changelog"),
                            releaseUrl = json.getString("release_url")
                        )
                    } else null
                }
                
                if (info != null) {
                    val currentVersion = BuildConfig.VERSION_NAME
                    if (info.version != currentVersion) {
                        _updateInfo.value = info
                    }
                }
            } catch (e: Exception) {
                LogRepository.w("Update check failed: ${e.localizedMessage}")
            }
        }
    }
}
