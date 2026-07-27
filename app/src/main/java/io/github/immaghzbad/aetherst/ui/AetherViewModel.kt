package io.github.immaghzbad.aetherst.ui

import android.content.Context
import android.net.VpnService
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.immaghzbad.aetherst.data.AetherConfigRepository
import io.github.immaghzbad.aetherst.data.IpInfo
import io.github.immaghzbad.aetherst.data.IpInfoRepository
import io.github.immaghzbad.aetherst.data.LogRepository
import io.github.immaghzbad.aetherst.data.PingRepository
import io.github.immaghzbad.aetherst.data.PingState
import io.github.immaghzbad.aetherst.model.AetherConfig
import io.github.immaghzbad.aetherst.model.ConnectionState
import io.github.immaghzbad.aetherst.model.LogEntry
import io.github.immaghzbad.aetherst.service.AetherVpnService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    val needVpnPermission: StateFlow<Boolean> = _needVpnPermission.asStateFlow()

    init {
        LogRepository.initialize(context)
        viewModelScope.launch {

            IpInfoRepository.fetchIpInfo(useProxy = false)
            PingRepository.runPing(useProxy = false)

            connectionState.collect { state ->

                when (state) {
                    ConnectionState.CONNECTED -> {

                        val socksAddr = config.value.socksAddress
                        LogRepository.i("[Health] Fetching public IP via SOCKS5 ($socksAddr)", "UI")
                        IpInfoRepository.fetchIpInfo(socksAddr, useProxy = true)
                        PingRepository.runPing(socksAddr, useProxy = true)
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

    fun checkVpnPermission(context: Context): Boolean {
        val intent = VpnService.prepare(context)
        val needed = intent != null
        _needVpnPermission.value = needed
        return !needed
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

    fun applyPreset(presetId: String) {
        repository.applyPreset(presetId)
    }

    fun refreshIpInfo() {
        viewModelScope.launch {
            val state = connectionState.value
            if (state == ConnectionState.CONNECTED) {
                IpInfoRepository.fetchIpInfo(config.value.socksAddress, useProxy = true)
            } else {
                IpInfoRepository.fetchIpInfo(useProxy = false)
            }
        }
    }

    fun refreshPing() {
        viewModelScope.launch {
            val state = connectionState.value
            if (state == ConnectionState.CONNECTED) {
                PingRepository.runPing(config.value.socksAddress, useProxy = true)
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
}
