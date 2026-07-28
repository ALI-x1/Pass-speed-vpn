@file:Suppress("unused")

package io.github.immaghzbad.aetherst.core

import android.net.VpnService
import io.github.immaghzbad.aetherst.data.LogRepository
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class LocalSocksProxyServer(
    private val vpnService: VpnService,
    private val listenHost: String = "198.18.0.1",
    private val listenPort: Int = 1819,
    private val targetHost: String = "127.0.0.1",
    private val targetPort: Int = 1819
) {
    private val isRunning = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null
    private val executor = Executors.newCachedThreadPool()
    private var mainThread: Thread? = null

    fun start() {
        if (isRunning.getAndSet(true)) return

        mainThread = Thread({
            try {
                val ss = ServerSocket()
                ss.reuseAddress = true
                ss.bind(InetSocketAddress(InetAddress.getByName(listenHost), listenPort))
                serverSocket = ss
                LogRepository.i("Transparent Bridge: $listenHost:$listenPort -> $targetHost:$targetPort", "Bridge")

                while (isRunning.get()) {
                    try {
                        val clientSocket = ss.accept()
                        executor.execute { handleRelay(clientSocket) }
                    } catch (e: Exception) {
                        if (isRunning.get()) LogRepository.w("Bridge accept error: ${e.localizedMessage}")
                    }
                }
            } catch (e: Exception) {
                LogRepository.w("Bridge failed to bind on $listenHost:$listenPort: ${e.localizedMessage}")
            }
        }, "Aether-Bridge").apply {
            isDaemon = true
            start()
        }
    }

    private fun handleRelay(clientSocket: Socket) {
        try {
            clientSocket.tcpNoDelay = true
            val targetSocket = Socket()
            vpnService.protect(targetSocket)
            targetSocket.tcpNoDelay = true
            targetSocket.connect(InetSocketAddress(targetHost, targetPort), 5000)

            val clientIn = clientSocket.getInputStream()
            val clientOut = clientSocket.getOutputStream()
            val targetIn = targetSocket.getInputStream()
            val targetOut = targetSocket.getOutputStream()

            val t1 = Thread { pipe(clientIn, targetOut) }
            val t2 = Thread { pipe(targetIn, clientOut) }
            
            t1.start()
            t2.start()
            
            t1.join(30000)
            t2.join(30000)
            
            runCatching { targetSocket.close() }
            runCatching { clientSocket.close() }
        } catch (e: Exception) {
            runCatching { clientSocket.close() }
        }
    }

    private fun pipe(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(32768)
        try {
            var n: Int
            while (input.read(buffer).also { n = it } != -1) {
                output.write(buffer, 0, n)
                output.flush()
            }
        } catch (_: Exception) {}
    }

    fun stop() {
        if (!isRunning.getAndSet(false)) return
        LogRepository.i("Stopping Bridge...", "Bridge")
        runCatching { serverSocket?.close() }
        serverSocket = null
        executor.shutdownNow()
        mainThread?.interrupt()
        mainThread = null
    }
}
