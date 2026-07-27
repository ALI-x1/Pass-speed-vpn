package io.github.immaghzbad.aetherst.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import io.github.immaghzbad.aetherst.MainActivity
import io.github.immaghzbad.aetherst.core.AetherProcessRunner
import io.github.immaghzbad.aetherst.core.HevTun2SocksEngine
import io.github.immaghzbad.aetherst.core.HevTun2SocksNative
import io.github.immaghzbad.aetherst.data.AetherConfigRepository
import io.github.immaghzbad.aetherst.data.LogRepository
import io.github.immaghzbad.aetherst.model.AetherConfig
import io.github.immaghzbad.aetherst.model.ConnectionState
import io.github.immaghzbad.aetherst.model.SessionTraffic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class AetherVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var runner: AetherProcessRunner? = null
    private var hevEngine: HevTun2SocksEngine? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val stateMutex = Mutex()
    private val attemptCounter = AtomicLong(0)
    private val activeAttemptId = AtomicLong(0)
    private val commandCounter = AtomicLong(0)
    private var startupJob: Job? = null
    private var timerJob: Job? = null
    private var durationSeconds = 0L
    private val trafficLock = Any()
    private var lastTxBytes = 0L
    private var lastRxBytes = 0L
    private var accumulatedTxBytes = 0L
    private var accumulatedRxBytes = 0L

    companion object {
        const val ACTION_START = "io.github.immaghzbad.aetherst.ACTION_START"
        const val ACTION_STOP = "io.github.immaghzbad.aetherst.ACTION_STOP"
        const val CHANNEL_ID = "aether_vpn_status_v2"
        const val NOTIFICATION_ID = 1001

        private val _serviceState = MutableStateFlow(ConnectionState.DISCONNECTED)
        val serviceState: StateFlow<ConnectionState> = _serviceState.asStateFlow()

        private val _elapsedSeconds = MutableStateFlow(0L)
        val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

        private val _sessionTraffic = MutableStateFlow(SessionTraffic())
        val sessionTraffic: StateFlow<SessionTraffic> = _sessionTraffic.asStateFlow()

        fun startVpn(context: Context): Boolean = runCatching {
            val intent = Intent(context, AetherVpnService::class.java).apply { action = ACTION_START }
            context.startForegroundService(intent)
            true
        }.getOrElse {
            LogRepository.e("[VpnService] Start failed: ${it.localizedMessage}")
            false
        }

        fun stopVpn(context: Context): Boolean = runCatching {
            val intent = Intent(context, AetherVpnService::class.java).apply { action = ACTION_STOP }
            context.startService(intent)
            true
        }.getOrElse {
            LogRepository.e("[VpnService] Stop failed: ${it.localizedMessage}")
            false
        }
    }

    override fun onCreate() {
        super.onCreate()
        LogRepository.initialize(this)
        createNotificationChannel()
        runner = AetherProcessRunner(this)
        hevEngine = HevTun2SocksEngine()

        scope.launch {
            runner?.connectionState?.collect(::handleCoreState)
        }

        scope.launch {
            hevEngine?.state?.collect { state ->
                if (state == HevTun2SocksEngine.State.FAILED) {
                    handleCriticalFailure("Native engine exited unexpectedly")
                }
            }
        }

        scope.launch {
            hevEngine?.stats?.collect(::updateSessionTraffic)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startAttempt(commandCounter.incrementAndGet())
            ACTION_STOP -> stopVpnService(commandCounter.incrementAndGet())
        }
        return START_NOT_STICKY
    }

    private suspend fun handleCoreState(coreState: ConnectionState) {
        val attemptId = activeAttemptId.get()
        if (attemptId == 0L) return

        var failure: String? = null
        stateMutex.withLock {
            if (activeAttemptId.get() != attemptId) return@withLock
            val current = _serviceState.value
            when (coreState) {
                ConnectionState.SCANNING -> Unit
                ConnectionState.VALIDATING -> {
                    if (current == ConnectionState.SCANNING) {
                        setStateLocked(attemptId, ConnectionState.VALIDATING)
                    }
                }
                ConnectionState.CONNECTED -> {
                    if (current == ConnectionState.RECONNECTING && hevEngine?.state?.value == HevTun2SocksEngine.State.RUNNING) {
                        setStateLocked(attemptId, ConnectionState.CONNECTED)
                        startTimer()
                    }
                }
                ConnectionState.RECONNECTING -> {
                    if (current == ConnectionState.CONNECTED) {
                        setStateLocked(attemptId, ConnectionState.RECONNECTING)
                        stopTimer()
                        resetSessionTraffic()
                    }
                }
                ConnectionState.ERROR -> {
                    if (current != ConnectionState.DISCONNECTING && current != ConnectionState.DISCONNECTED) {
                        failure = "Aether Core reported an error"
                    }
                }
                ConnectionState.DISCONNECTED -> {
                    if (current == ConnectionState.CONNECTED || current == ConnectionState.RECONNECTING) {
                        failure = "Aether Core stopped unexpectedly"
                    }
                }
                ConnectionState.DISCONNECTING -> Unit
            }
        }
        failure?.let { handleCriticalFailure(it) }
    }

    private fun startAttempt(commandId: Long) {
        startupJob = scope.launch {
            if (commandCounter.get() != commandId) return@launch
            val attemptId = stateMutex.withLock {
                if (commandCounter.get() != commandId) return@launch
                val current = _serviceState.value
                if (current != ConnectionState.DISCONNECTED && current != ConnectionState.ERROR) return@launch
                val id = attemptCounter.incrementAndGet()
                activeAttemptId.set(id)
                resetSessionTraffic()
                setStateLocked(id, ConnectionState.SCANNING)
                id
            }
            showInitialNotification()

            if (!HevTun2SocksNative.isAvailable) {
                LogRepository.e("[Hev] [attempt=$attemptId] Native library unavailable: ${HevTun2SocksNative.loadFailure?.localizedMessage}")
                rollback(attemptId, "Native library unavailable")
                return@launch
            }

            val config = AetherConfigRepository.getInstance(this@AetherVpnService).config.value
            try {
                LogRepository.i("[Core] [attempt=$attemptId] Starting")
                runner?.start(config)

                val coreReady = withTimeoutOrNull(30.seconds) {
                    runner?.connectionState?.first { it == ConnectionState.CONNECTED }
                    true
                } == true

                ensureCurrentAttempt(attemptId)
                if (!coreReady) throw IllegalStateException("Core validation timed out")
                LogRepository.i("[Core] [attempt=$attemptId] Data plane validated")

                if (!config.proxyOnly) {
                    publishState(attemptId, ConnectionState.VALIDATING)
                    if (!probeSocks5WithRetry(config, attemptId)) throw IllegalStateException("SOCKS5 readiness timed out")
                    ensureCurrentAttempt(attemptId)
                    LogRepository.i("[SocksProbe] [attempt=$attemptId] Handshake succeeded")

                    if (!establishVpnTun(attemptId)) throw IllegalStateException("TUN establishment failed")
                    ensureCurrentAttempt(attemptId)

                    val descriptor = vpnInterface ?: throw IllegalStateException("TUN descriptor unavailable")
                    val started = hevEngine?.start(
                        descriptor,
                        getSocksHost(config),
                        getSocksPort(config),
                        1280,
                        attemptId
                    ) == true
                    ensureCurrentAttempt(attemptId)
                    if (!started) throw IllegalStateException("Native engine stabilization failed")
                }

                publishState(attemptId, ConnectionState.CONNECTED)
                LogRepository.i("[State] [attempt=$attemptId] Full-device VPN connected")
                startTimer()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                if (activeAttemptId.get() == attemptId && commandCounter.get() == commandId) {
                    rollback(attemptId, throwable.localizedMessage ?: "Startup failed")
                }
            }
        }
    }

    private suspend fun publishState(attemptId: Long, state: ConnectionState) {
        stateMutex.withLock {
            ensureCurrentAttempt(attemptId)
            setStateLocked(attemptId, state)
        }
    }

    private fun setStateLocked(attemptId: Long, state: ConnectionState) {
        val previous = _serviceState.value
        if (previous == state) return
        _serviceState.value = state
        LogRepository.i("[State] [attempt=$attemptId] $previous -> $state")
        updateNotification()
    }

    private fun ensureCurrentAttempt(attemptId: Long) {
        if (activeAttemptId.get() != attemptId) {
            throw IllegalStateException("Connection attempt invalidated")
        }
    }

    private suspend fun probeSocks5WithRetry(config: AetherConfig, attemptId: Long): Boolean {
        val deadline = System.nanoTime() + 5.seconds.inWholeNanoseconds
        var attempts = 0
        while (System.nanoTime() < deadline && activeAttemptId.get() == attemptId) {
            attempts += 1
            if (probeSocks5Once(config)) {
                LogRepository.i("[SocksProbe] [attempt=$attemptId] Ready after $attempts attempt(s)")
                return true
            }
            delay(150.milliseconds)
        }
        LogRepository.w("[SocksProbe] [attempt=$attemptId] Readiness timed out after $attempts attempt(s)")
        return false
    }

    private fun probeSocks5Once(config: AetherConfig): Boolean = runCatching {
        Socket().use { socket ->
            socket.soTimeout = 750
            socket.connect(InetSocketAddress(getSocksHost(config), getSocksPort(config)), 750)
            socket.getOutputStream().apply {
                write(byteArrayOf(5, 1, 0))
                flush()
            }
            val response = ByteArray(2)
            var offset = 0
            while (offset < response.size) {
                val count = socket.getInputStream().read(response, offset, response.size - offset)
                if (count < 0) return@use false
                offset += count
            }
            response[0] == 5.toByte() && response[1] == 0.toByte()
        }
    }.getOrDefault(false)

    private fun getSocksHost(config: AetherConfig): String = config.socksAddress.substringBefore(":").ifBlank { "127.0.0.1" }

    private fun getSocksPort(config: AetherConfig): Int = config.socksAddress.substringAfter(":", "1819").toIntOrNull() ?: 1819

    private fun establishVpnTun(attemptId: Long): Boolean = runCatching {
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val builder = Builder()
            .addAddress("198.18.0.1", 30)
            .addAddress("fd00::1", 128)
            .addRoute("0.0.0.0", 0)
            .addRoute("::", 0)
            .addDnsServer("1.1.1.1")
            .setMtu(1280)
            .setSession("AetherST Tunnel")
            .setConfigureIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), pendingFlags))
        builder.addDisallowedApplication(packageName)
        vpnInterface = builder.establish() ?: return false
        LogRepository.i("[Tun] [attempt=$attemptId] Established mtu=1280")
        true
    }.getOrElse {
        LogRepository.e("[Tun] [attempt=$attemptId] Establishment failed: ${it.localizedMessage}")
        false
    }

    private fun handleCriticalFailure(reason: String) {
        val attemptId = activeAttemptId.get()
        if (attemptId == 0L) return
        scope.launch { rollback(attemptId, reason) }
    }

    private suspend fun rollback(attemptId: Long, reason: String) {
        val shouldRollback = stateMutex.withLock {
            if (activeAttemptId.get() != attemptId) return@withLock false
            activeAttemptId.set(0)
            setStateLocked(attemptId, ConnectionState.DISCONNECTING)
            true
        }
        if (!shouldRollback) return
        LogRepository.e("[State] [attempt=$attemptId] Startup/runtime failure: $reason")
        stopTimer()
        resetSessionTraffic()
        hevEngine?.stopAndAwait()
        closeVpnInterface(attemptId)
        runner?.stop()
        stateMutex.withLock {
            if (activeAttemptId.get() == 0L && _serviceState.value == ConnectionState.DISCONNECTING) {
                setStateLocked(attemptId, ConnectionState.ERROR)
            }
        }
    }

    private fun stopVpnService(commandId: Long) {
        scope.launch {
            val attemptId = stateMutex.withLock {
                val id = activeAttemptId.getAndSet(0).takeIf { it != 0L } ?: attemptCounter.get()
                if (_serviceState.value == ConnectionState.DISCONNECTED) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@launch
                }
                if (_serviceState.value != ConnectionState.DISCONNECTING) setStateLocked(id, ConnectionState.DISCONNECTING)
                id
            }
            startupJob?.cancelAndJoin()
            startupJob = null
            stopTimer()
            resetSessionTraffic()
            LogRepository.i("[Hev] [attempt=$attemptId] Stop requested")
            hevEngine?.stopAndAwait()
            closeVpnInterface(attemptId)
            runner?.stop()
            stateMutex.withLock {
                if (commandCounter.get() == commandId) setStateLocked(attemptId, ConnectionState.DISCONNECTED)
            }
            if (commandCounter.get() == commandId) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun closeVpnInterface(attemptId: Long) {
        val descriptor = vpnInterface
        vpnInterface = null
        if (descriptor != null) {
            runCatching { descriptor.close() }
                .onSuccess { LogRepository.i("[Tun] [attempt=$attemptId] Descriptor closed") }
                .onFailure { LogRepository.w("[Tun] [attempt=$attemptId] Close failed: ${it.localizedMessage}") }
        }
    }

    private fun resetSessionTraffic() {
        synchronized(trafficLock) {
            val stats = hevEngine?.stats?.value
            lastTxBytes = stats?.txBytes ?: 0L
            lastRxBytes = stats?.rxBytes ?: 0L
            accumulatedTxBytes = 0
            accumulatedRxBytes = 0
            _sessionTraffic.value = SessionTraffic()
        }
    }

    private fun updateSessionTraffic(stats: HevTun2SocksEngine.Stats) {
        synchronized(trafficLock) {
            val txDelta = if (stats.txBytes >= lastTxBytes) stats.txBytes - lastTxBytes else stats.txBytes
            val rxDelta = if (stats.rxBytes >= lastRxBytes) stats.rxBytes - lastRxBytes else stats.rxBytes

            if (_serviceState.value == ConnectionState.CONNECTED) {
                accumulatedTxBytes = saturatingAdd(accumulatedTxBytes, txDelta)
                accumulatedRxBytes = saturatingAdd(accumulatedRxBytes, rxDelta)
                _sessionTraffic.value = SessionTraffic(accumulatedTxBytes, accumulatedRxBytes)
            }

            lastTxBytes = stats.txBytes
            lastRxBytes = stats.rxBytes
        }
    }

    private fun saturatingAdd(current: Long, delta: Long): Long {
        if (delta <= 0) return current
        return if (Long.MAX_VALUE - current < delta) Long.MAX_VALUE else current + delta
    }

    private fun showInitialNotification() {
        val notification = buildNotification("Connecting...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startTimer() {
        if (timerJob?.isActive == true) return
        durationSeconds = 0
        _elapsedSeconds.value = 0
        timerJob = scope.launch {
            while (isActive) {
                delay(1.seconds)
                durationSeconds += 1
                _elapsedSeconds.value = durationSeconds
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        durationSeconds = 0
        _elapsedSeconds.value = 0
    }

    private fun updateNotification() {
        val text = when (_serviceState.value) {
            ConnectionState.CONNECTED -> "Full-device VPN connected"
            ConnectionState.SCANNING -> "Scanning..."
            ConnectionState.VALIDATING -> "Validating..."
            ConnectionState.RECONNECTING -> "Reconnecting..."
            ConnectionState.DISCONNECTING -> "Disconnecting..."
            ConnectionState.ERROR -> "Connection error"
            ConnectionState.DISCONNECTED -> "Standby"
        }
        runCatching {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, buildNotification(text))
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val contentIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), flags)
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, AetherVpnService::class.java).apply { action = ACTION_STOP },
            flags
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AetherST Tunnel")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disconnect", stopIntent)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "AetherST Tunnel", NotificationManager.IMPORTANCE_LOW).apply {
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
            setShowBadge(false)
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        activeAttemptId.set(0)
        hevEngine?.requestStop()
        closeVpnInterface(attemptCounter.get())
        runner?.release()
        scope.cancel()
        super.onDestroy()
    }
}
