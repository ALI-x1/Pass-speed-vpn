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
    private val port: Int = 1819
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
                ss.bind(InetSocketAddress(InetAddress.getByName("127.0.0.1"), port))
                serverSocket = ss
                LogRepository.i("LocalSocksProxyServer listening on 127.0.0.1:$port", "SocksProxy")

                while (isRunning.get()) {
                    try {
                        val clientSocket = ss.accept()
                        executor.execute {
                            handleClient(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (isRunning.get()) {
                            LogRepository.w("SocksProxy accept error: ${e.localizedMessage}", "SocksProxy")
                        }
                    }
                }
            } catch (e: Exception) {
                LogRepository.w("LocalSocksProxyServer failed to bind on port $port: ${e.localizedMessage}", "SocksProxy")
            }
        }, "Aether-LocalSocksProxyServer").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        if (!isRunning.getAndSet(false)) return
        LogRepository.i("Stopping LocalSocksProxyServer...", "SocksProxy")
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        executor.shutdownNow()
        mainThread?.interrupt()
        mainThread = null
    }

    private fun handleClient(clientSocket: Socket) {
        try {
            clientSocket.soTimeout = 10000
            val clientIn = clientSocket.getInputStream()
            val clientOut = clientSocket.getOutputStream()

            val greeting = ByteArray(3)
            if (!readExact(clientIn, greeting, 0, 2)) {
                clientSocket.close()
                return
            }

            val ver = greeting[0].toInt() and 0xFF
            val nmethods = greeting[1].toInt() and 0xFF
            if (ver != 5) {
                clientSocket.close()
                return
            }

            val methods = ByteArray(nmethods)
            readExact(clientIn, methods, 0, nmethods)

            clientOut.write(byteArrayOf(0x05, 0x00))
            clientOut.flush()

            val reqHeader = ByteArray(4)
            if (!readExact(clientIn, reqHeader, 0, 4)) {
                clientSocket.close()
                return
            }

            val cmd = reqHeader[1].toInt() and 0xFF
            val atyp = reqHeader[3].toInt() and 0xFF

            if (cmd != 1) {
                sendSocksResponse(clientOut, 0x07)
                clientSocket.close()
                return
            }

            val targetHost: String
            val targetPort: Int

            when (atyp) {
                1 -> {
                    val ipBytes = ByteArray(4)
                    if (!readExact(clientIn, ipBytes, 0, 4)) {
                        clientSocket.close()
                        return
                    }
                    targetHost = InetAddress.getByAddress(ipBytes).hostAddress ?: "127.0.0.1"
                }
                3 -> {
                    val lenByte = ByteArray(1)
                    if (!readExact(clientIn, lenByte, 0, 1)) {
                        clientSocket.close()
                        return
                    }
                    val domainLen = lenByte[0].toInt() and 0xFF
                    val domainBytes = ByteArray(domainLen)
                    if (!readExact(clientIn, domainBytes, 0, domainLen)) {
                        clientSocket.close()
                        return
                    }
                    targetHost = String(domainBytes, Charsets.UTF_8)
                }
                4 -> {
                    val ip6Bytes = ByteArray(16)
                    if (!readExact(clientIn, ip6Bytes, 0, 16)) {
                        clientSocket.close()
                        return
                    }
                    targetHost = InetAddress.getByAddress(ip6Bytes).hostAddress ?: "::1"
                }
                else -> {
                    sendSocksResponse(clientOut, 0x08)
                    clientSocket.close()
                    return
                }
            }

            val portBytes = ByteArray(2)
            if (!readExact(clientIn, portBytes, 0, 2)) {
                clientSocket.close()
                return
            }
            targetPort = ((portBytes[0].toInt() and 0xFF) shl 8) or (portBytes[1].toInt() and 0xFF)

            val targetSocket = Socket()

            if (targetHost != "127.0.0.1" && targetHost != "localhost" && !targetHost.startsWith("127.")) {
                try {
                    vpnService.protect(targetSocket)
                } catch (e: Exception) {
                    LogRepository.w("Failed to protect targetSocket: ${e.localizedMessage}", "SocksProxy")
                }
            }

            try {
                targetSocket.connect(InetSocketAddress(targetHost, targetPort), 10000)
            } catch (e: Exception) {
                LogRepository.w("SocksProxy target connection failed to $targetHost:$targetPort - ${e.localizedMessage}", "SocksProxy")
                sendSocksResponse(clientOut, 0x04)
                clientSocket.close()
                return
            }

            sendSocksResponse(clientOut, 0x00)

            clientSocket.soTimeout = 0
            targetSocket.soTimeout = 0

            val targetIn = targetSocket.getInputStream()
            val targetOut = targetSocket.getOutputStream()

            val t1 = Thread {
                pipeStream(clientIn, targetOut, targetSocket)
            }
            val t2 = Thread {
                pipeStream(targetIn, clientOut, clientSocket)
            }
            t1.start()
            t2.start()

            t1.join(15000)
            t2.join(15000)

            try { targetSocket.close() } catch (_: Exception) {}
            try { clientSocket.close() } catch (_: Exception) {}

        } catch (_: Exception) {
            try { clientSocket.close() } catch (_: Exception) {}
        }
    }

    private fun sendSocksResponse(out: OutputStream, repCode: Byte) {
        try {
            out.write(byteArrayOf(
                0x05, repCode, 0x00, 0x01,
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00
            ))
            out.flush()
        } catch (_: Exception) {}
    }

    private fun readExact(input: InputStream, buffer: ByteArray, offset: Int, length: Int): Boolean {
        var readTotal = 0
        while (readTotal < length) {
            val count = input.read(buffer, offset + readTotal, length - readTotal)
            if (count <= 0) return false
            readTotal += count
        }
        return true
    }

    private fun pipeStream(input: InputStream, output: OutputStream, peerSocket: Socket) {
        val buffer = ByteArray(16384)
        try {
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } > 0) {
                output.write(buffer, 0, bytesRead)
                output.flush()
            }
        } catch (_: Exception) {
        } finally {
            try { peerSocket.close() } catch (_: Exception) {}
        }
    }
}
