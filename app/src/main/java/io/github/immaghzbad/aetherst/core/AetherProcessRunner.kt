package io.github.immaghzbad.aetherst.core

import android.content.Context
import io.github.immaghzbad.aetherst.data.LogRepository
import io.github.immaghzbad.aetherst.model.AetherConfig
import io.github.immaghzbad.aetherst.model.AetherProtocol
import io.github.immaghzbad.aetherst.model.ConnectionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.milliseconds

class AetherProcessRunner(private val context: Context) {

    private val lock = Any()
    private var process: Process? = null
    private var runnerJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val currentAttemptId = AtomicLong(0)
    private var goolOuterValidated = false

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    fun start(config: AetherConfig) {
        synchronized(lock) {
            if (runnerJob?.isActive == true) return

            val attemptId = currentAttemptId.incrementAndGet()
            runnerJob = scope.launch {
                var retryCount = 0
                val maxRetries = 5

                while (isActive && (currentAttemptId.get() == attemptId) && (retryCount <= maxRetries)) {
                    if (retryCount > 0) {
                        LogRepository.i("Restarting tunnel (Attempt ${retryCount + 1}/$maxRetries)...")
                        updateState(ConnectionState.RECONNECTING, attemptId)
                        delay(2000.milliseconds)
                    } else {
                        LogRepository.i("Initializing Aether Core...")
                        updateState(ConnectionState.SCANNING, attemptId)
                    }

                    if (currentAttemptId.get() != attemptId) break

                    try {
                        val result = runBinary(config, attemptId)
                        if (result || (currentAttemptId.get() != attemptId)) break
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        LogRepository.e("Execution cycle error: ${e.localizedMessage}")
                    }

                    retryCount++
                }

                if (isActive && (currentAttemptId.get() == attemptId) && (retryCount > maxRetries)) {
                    LogRepository.e("Tunnel stabilization failed.")
                    updateState(ConnectionState.ERROR, attemptId)
                }
            }
        }
    }

    private suspend fun runBinary(config: AetherConfig, attemptId: Long): Boolean {
        var proc: Process? = null
        return try {
            val binaryFile = BinaryManager.prepareBinary(context)
            if (currentAttemptId.get() != attemptId) return true

            val commandList = mutableListOf<String>()
            commandList.add(binaryFile.absolutePath)
            commandList.add("--bind")
            commandList.add(config.socksAddress)

            commandList.add(
                when (config.ipMode) {
                    io.github.immaghzbad.aetherst.model.AetherIpMode.IPV4 -> "-4"
                    io.github.immaghzbad.aetherst.model.AetherIpMode.IPV6 -> "-6"
                    io.github.immaghzbad.aetherst.model.AetherIpMode.DUAL -> "--dual"
                }
            )

            if (config.h2Mode) commandList.add("--h2")
            if (config.h2Fragment) {
                commandList.add("--fragment")
                commandList.add("--fragment-size")
                commandList.add(config.fragmentSize)
                commandList.add("--fragment-delay")
                commandList.add(config.fragmentDelay)
            }
            if (config.noDataCheck) commandList.add("--no-data-check")
            if (config.quickReconnect) commandList.add("--quick-reconnect") else commandList.add("--no-quick-reconnect")

            if (config.peer.isNotEmpty()) {
                commandList.add("--peer")
                commandList.add(config.peer)
            }

            if ((config.protocol == AetherProtocol.WG) || (config.protocol == AetherProtocol.GOOL)) {
                commandList.add("--keepalive")
                commandList.add(config.keepalive.toString())
            }

            if (config.tlsGroups.isNotEmpty()) {
                commandList.add("--tls-groups")
                commandList.add(config.tlsGroups)
            }

            if (config.noProfileRetry) commandList.add("--no-profile-retry")

            val pb = ProcessBuilder(commandList)
            pb.directory(context.filesDir)

            val env = pb.environment()
            env["AETHER_PROTOCOL"] = config.protocol.rawValue
            env["AETHER_NOIZE"] = config.noise.rawValue
            env["AETHER_SCAN"] = config.scanMode.rawValue
            env["AETHER_IP"] = config.ipMode.rawValue
            env["AETHER_SOCKS"] = config.socksAddress
            env["AETHER_LOG"] = config.coreLogLevel.rawValue

            if (config.h2Mode) env["AETHER_MASQUE_HTTP2"] = "1"
            if (config.h2Fragment) {
                env["AETHER_MASQUE_H2_FRAGMENT"] = "1"
                env["AETHER_MASQUE_H2_FRAGMENT_SIZE"] = config.fragmentSize
                env["AETHER_MASQUE_H2_FRAGMENT_DELAY"] = config.fragmentDelay
            }

            if (config.noDataCheck) {
                env["AETHER_MASQUE_NO_DATA_CHECK"] = "1"
                env["AETHER_WG_NO_DATA_CHECK"] = "1"
            }

            if (config.quickReconnect) env["AETHER_QUICK_RECONNECT"] = "1" else env["AETHER_QUICK_RECONNECT"] = "0"

            if (config.peer.isNotEmpty()) {
                env["AETHER_PEER"] = config.peer
                env["AETHER_WG_PEER"] = config.peer
            }

            env["AETHER_WG_KEEPALIVE"] = config.keepalive.toString()
            env["AETHER_MASQUE_VALIDATE_SECS"] = config.validateSecs.toString()
            env["AETHER_MASQUE_RECONNECT_SECS"] = config.reconnectSecs.toString()
            env["AETHER_WG_RECONNECT_SECS"] = config.reconnectSecs.toString()

            if (config.noProfileRetry) env["AETHER_WG_NO_PROFILE_RETRY"] = "1"
            if (config.tlsGroups.isNotEmpty()) env["AETHER_TLS_GROUPS"] = config.tlsGroups

            pb.redirectErrorStream(true)

            proc = withContext(Dispatchers.IO) { pb.start() }

            synchronized(lock) {
                if (currentAttemptId.get() != attemptId) {
                    proc?.destroyForcibly()
                    return true
                }
                process = proc
            }

            BufferedReader(InputStreamReader(proc!!.inputStream)).use { reader ->
                var line: String?
                while (currentCoroutineContext().isActive && (currentAttemptId.get() == attemptId)) {
                    line = try {
                        reader.readLine()
                    } catch (e: java.io.IOException) {
                        if (currentAttemptId.get() != attemptId) null else throw e
                    } ?: break

                    parseOutputLine(line, attemptId, config.protocol)
                }
            }

            val exitCode = try { withContext(Dispatchers.IO) { proc.waitFor() } } catch (_: Exception) { -1 }
            if (currentAttemptId.get() == attemptId) {
                LogRepository.i("Core process exited with code $exitCode")
            }
            exitCode == 0
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (currentAttemptId.get() == attemptId) {
                LogRepository.e("Binary runtime error: ${e.localizedMessage}")
                return false
            }
            true
        } finally {
            synchronized(lock) {
                if (process === proc) process = null
            }
            try { proc?.destroyForcibly() } catch (_: Exception) {}
        }
    }

    private fun parseOutputLine(line: String, attemptId: Long, protocol: AetherProtocol) {
        if (currentAttemptId.get() != attemptId) return

        LogRepository.i(line, "AetherCore")

        val lower = line.lowercase()
        val isCriticalError = (lower.contains("fatal") || lower.contains("panic")) &&
                !lower.contains("socksbridge") &&
                !lower.contains("connection failed")

        when {
            lower.contains("scanning") -> {
                goolOuterValidated = false
                updateState(ConnectionState.SCANNING, attemptId)
            }
            lower.contains("validating") -> updateState(ConnectionState.VALIDATING, attemptId)

            protocol == AetherProtocol.MASQUE && lower.contains("tunnel validated (end-to-end data confirmed)") -> {
                updateState(ConnectionState.CONNECTED, attemptId)
            }

            protocol == AetherProtocol.WG && lower.contains("wireguard tunnel validated") -> {
                updateState(ConnectionState.CONNECTED, attemptId)
            }

            protocol == AetherProtocol.GOOL -> {
                if (lower.contains("outer") && lower.contains("tunnel validated")) {
                    goolOuterValidated = true
                }
                if (lower.contains("inner") && lower.contains("tunnel validated") && goolOuterValidated) {
                    updateState(ConnectionState.CONNECTED, attemptId)
                }
            }

            protocol != AetherProtocol.MASQUE && protocol != AetherProtocol.WG && protocol != AetherProtocol.GOOL &&
            (lower.contains("tunnel validated") || lower.contains("connect-ip status: 200")) -> {
                updateState(ConnectionState.CONNECTED, attemptId)
            }

            lower.contains("reconnecting") -> {
                goolOuterValidated = false
                updateState(ConnectionState.RECONNECTING, attemptId)
            }
            isCriticalError -> {
                val current = _connectionState.value
                if (current != ConnectionState.CONNECTED && current != ConnectionState.RECONNECTING) {
                    updateState(ConnectionState.ERROR, attemptId)
                }
            }
        }
    }

    private fun updateState(state: ConnectionState, attemptId: Long = currentAttemptId.get()) {
        if (currentAttemptId.get() == attemptId) {
            _connectionState.value = state
        }
    }

    fun stop() {
        currentAttemptId.incrementAndGet()
        _connectionState.value = ConnectionState.DISCONNECTED

        var jobToCancel: Job? = null
        var procToDestroy: Process? = null

        synchronized(lock) {
            jobToCancel = runnerJob
            procToDestroy = process
            runnerJob = null
            process = null
        }

        jobToCancel?.cancel()
        try {
            procToDestroy?.destroyForcibly()
        } catch (_: Exception) {}

        LogRepository.i("Tunnel shutdown complete.")
    }

    fun release() {
        stop()
        scope.cancel()
    }
}
