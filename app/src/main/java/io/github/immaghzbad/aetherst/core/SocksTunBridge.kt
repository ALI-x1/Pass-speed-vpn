@file:Suppress("unused")

package io.github.immaghzbad.aetherst.core

import android.net.VpnService
import android.os.ParcelFileDescriptor
import io.github.immaghzbad.aetherst.data.LogRepository
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class SocksTunBridge(
    private val vpnService: VpnService,
    private val tunDescriptor: ParcelFileDescriptor,
    private val socksHost: String = "127.0.0.1",
    private val socksPort: Int = 1819,
    private val mtu: Int = 1100
) {

    private val isRunning = AtomicBoolean(false)
    private var readThread: Thread? = null
    private var writeThread: Thread? = null
    private val executor = Executors.newCachedThreadPool()

    private val tunOutputQueue = LinkedBlockingQueue<ByteArray>(2048)
    private val tcpSessions = ConcurrentHashMap<Long, TcpSession>()
    private val udpSessions = ConcurrentHashMap<Long, UdpSession>()

    private val localIpInt = 0x0A000002

    fun start() {
        if (isRunning.getAndSet(true)) return
        LogRepository.i("Starting V4.2 Bridge (MTU=$mtu)...")

        writeThread = Thread({
            val fos = FileOutputStream(tunDescriptor.fileDescriptor)
            while (isRunning.get()) {
                try {
                    val packet = tunOutputQueue.poll(500, TimeUnit.MILLISECONDS) ?: continue
                    fos.write(packet)
                } catch (_: InterruptedException) { break }
                catch (e: Exception) { if (isRunning.get()) LogRepository.w("TUN Write Err: ${e.message}") }
            }
        }, "Aether-Writer").apply { priority = Thread.MAX_PRIORITY; isDaemon = true; start() }

        readThread = Thread({
            val fis = FileInputStream(tunDescriptor.fileDescriptor)
            val buffer = ByteArray(mtu + 200)
            waitForCore()
            while (isRunning.get()) {
                try {
                    val n = fis.read(buffer)
                    if (n <= 0) continue
                    processPacket(buffer, n)
                } catch (e: Exception) {
                    if (isRunning.get()) LogRepository.w("TUN Read Err: ${e.message}")
                }
            }
        }, "Aether-Reader").apply { priority = Thread.MAX_PRIORITY; isDaemon = true; start() }
    }

    private fun waitForCore() {
        var ready = false
        for (i in 1..25) {
            try {
                Socket().use { it.connect(InetSocketAddress(socksHost, socksPort), 200) }
                ready = true; break
            } catch (_: Exception) { Thread.sleep(200) }
        }
        if (ready) LogRepository.i("Core link established.")
    }

    fun stop() {
        if (!isRunning.getAndSet(false)) return
        tcpSessions.values.forEach { it.close() }
        tcpSessions.clear()
        udpSessions.values.forEach { it.close() }
        udpSessions.clear()
        executor.shutdownNow()
        readThread?.interrupt()
        writeThread?.interrupt()
    }

    private fun enqueueTun(data: ByteArray) {
        if (!tunOutputQueue.offer(data)) {
            tunOutputQueue.poll()
            tunOutputQueue.offer(data)
        }
    }

    private fun processPacket(packet: ByteArray, len: Int) {
        if (len < 20 || (packet[0].toInt() shr 4) != 4) return

        val proto = packet[9].toInt() and 0xFF
        val hLen = (packet[0].toInt() and 0x0F) * 4
        val dIp = getInt(packet, 16)
        val sP = getShort(packet, hLen)
        val dP = getShort(packet, hLen + 2)

        val key = ((dIp.toLong() and 0xFFFFFFFFL) shl 32) or ((sP.toLong() and 0xFFFFL) shl 16) or (dP.toLong() and 0xFFFFL)

        if (proto == 17) {
            val payload = packet.copyOfRange(hLen + 8, len)
            val session = udpSessions.getOrPut(key) {
                UdpSession(key, dIp, sP, dP).also { executor.execute { it.run() } }
            }
            session.queue(payload)
        } else if (proto == 6) {
            val flags = packet[hLen + 13].toInt() and 0xFF
            val isSyn = (flags and 0x02) != 0
            val isAck = (flags and 0x10) != 0
            val isFin = (flags and 0x01) != 0
            val isRst = (flags and 0x04) != 0

            val tcpDataOff = hLen + ((packet[hLen + 12].toInt() and 0xF0) shr 2)
            val payloadLen = len - tcpDataOff

            if (isSyn && !isAck) {
                val seq = getLong(packet, hLen + 4)
                val session = TcpSession(key, dIp, sP, dP, seq)
                tcpSessions[key] = session
                executor.execute { session.run() }
            } else {
                val session = tcpSessions[key]
                if (session != null && session.active) {
                    session.update(payloadLen, getLong(packet, hLen + 4), isSyn, isFin)
                    if (payloadLen > 0) session.queue(packet.copyOfRange(tcpDataOff, len))
                    if (isFin || isRst) { tcpSessions.remove(key); session.close() }
                }
            }
        }
    }

    private inner class UdpSession(val key: Long, val dIpInt: Int, val sP: Int, val dP: Int) {
        private val queue = LinkedBlockingQueue<ByteArray>(256)
        @Volatile var active = true
        private var ctrlSock: Socket? = null
        private var udpSock: DatagramSocket? = null

        fun queue(data: ByteArray) { if (active) queue.offer(data) }

        fun run() {
            try {
                val s = Socket()
                if (socksHost != "127.0.0.1") vpnService.protect(s)
                s.tcpNoDelay = true
                s.connect(InetSocketAddress(socksHost, socksPort), 5000)
                ctrlSock = s
                val out = s.getOutputStream(); val ins = s.getInputStream()
                out.write(byteArrayOf(5, 1, 0)); out.flush()
                val auth = ByteArray(2); readExact(ins, auth)
                if (auth[1] != 0.toByte()) return
                out.write(byteArrayOf(5, 3, 0, 1, 0, 0, 0, 0, 0, 0)); out.flush()
                val resp = ByteArray(10); readExact(ins, resp)
                if (resp[1] != 0.toByte()) return

                val relayAddr = InetSocketAddress(getIpAddressFromPacket(resp, 4), getShort(resp, 8))
                udpSock = DatagramSocket().apply { vpnService.protect(this); soTimeout = 30000 }

                executor.execute {

                    val buf = ByteArray(mtu - 28)
                    while (active && isRunning.get()) {
                        try {
                            val p = DatagramPacket(buf, buf.size)
                            udpSock?.receive(p)
                            if (p.length < 10) continue
                            enqueueTun(buildUdp(dIpInt, dP, sP, p.data.copyOfRange(10, p.length)))
                        } catch (_: Exception) { break }
                    }
                    close()
                }

                val socksHeader = ByteArray(10).apply {
                    this[3] = 1; setInt(this, 4, dIpInt)
                    this[8] = (dP shr 8).toByte(); this[9] = (dP and 0xFF).toByte()
                }

                while (active && isRunning.get()) {
                    val payload = queue.poll(2, TimeUnit.SECONDS) ?: continue
                    val full = ByteArray(10 + payload.size)
                    System.arraycopy(socksHeader, 0, full, 0, 10)
                    System.arraycopy(payload, 0, full, 10, payload.size)
                    udpSock?.send(DatagramPacket(full, full.size, relayAddr))
                }
            } catch (_: Exception) { } finally { close() }
        }

        fun close() { active = false; try { ctrlSock?.close() } catch (_: Exception) {}; try { udpSock?.close() } catch (_: Exception) {}; udpSessions.remove(key) }
    }

    private inner class TcpSession(val key: Long, val dIpInt: Int, val sP: Int, val dP: Int, initialSeq: Long) {
        private val queue = LinkedBlockingQueue<ByteArray>(512)
        @Volatile var active = false
        private var sock: Socket? = null
        private var mySeq = (100000..900000).random().toLong()
        private var myAck = (initialSeq + 1) and 0xFFFFFFFFL

        fun update(len: Int, seq: Long, syn: Boolean, fin: Boolean) { myAck = (seq + (if (syn || fin) 1 else len)) and 0xFFFFFFFFL }
        fun queue(data: ByteArray) { if (active) queue.offer(data) }

        fun run() {
            try {
                val s = Socket()
                if (socksHost != "127.0.0.1") vpnService.protect(s)
                s.tcpNoDelay = true
                s.setReceiveBufferSize(256 * 1024)
                s.connect(InetSocketAddress(socksHost, socksPort), 5000)
                sock = s
                val ins = s.getInputStream(); val outS = s.getOutputStream()
                outS.write(byteArrayOf(5, 1, 0)); outS.flush()
                val auth = ByteArray(2); readExact(ins, auth)
                if (auth[1] != 0.toByte()) return
                val req = byteArrayOf(5, 1, 0, 1, 0, 0, 0, 0, (dP shr 8).toByte(), (dP and 0xFF).toByte())
                setInt(req, 4, dIpInt); outS.write(req); outS.flush()
                val resp = ByteArray(10); readExact(ins, resp)
                if (resp[1] != 0.toByte()) return

                active = true
                enqueueTun(buildTcp(dIpInt, dP, sP, null, mySeq++, myAck, 0x12))

                executor.execute {

                    val buf = ByteArray(mtu - 40)
                    while (active && isRunning.get()) {
                        try {
                            val n = ins.read(buf)
                            if (n <= 0) break
                            enqueueTun(buildTcp(dIpInt, dP, sP, buf.copyOf(n), mySeq, myAck, 0x18))
                            mySeq = (mySeq + n) and 0xFFFFFFFFL
                        } catch (_: Exception) { break }
                    }
                    close()
                }

                while (active && isRunning.get()) {
                    val data = queue.poll(2, TimeUnit.SECONDS) ?: continue
                    outS.write(data); outS.flush()
                }
            } catch (_: Exception) { } finally { close() }
        }

        fun close() { active = false; try { sock?.close() } catch (_: Exception) {}; tcpSessions.remove(key) }
    }

    private fun readExact(ins: InputStream, b: ByteArray) {
        var o = 0
        while (o < b.size) {
            val c = ins.read(b, o, b.size - o)
            if (c < 0) break; o += c
        }
    }

    private fun getInt(b: ByteArray, o: Int) = ((b[o].toInt() and 0xFF) shl 24) or ((b[o+1].toInt() and 0xFF) shl 16) or ((b[o+2].toInt() and 0xFF) shl 8) or (b[o+3].toInt() and 0xFF)
    private fun setInt(b: ByteArray, o: Int, v: Int) { b[o] = (v shr 24).toByte(); b[o+1] = (v shr 16).toByte(); b[o+2] = (v shr 8).toByte(); b[o+3] = (v and 0xFF).toByte() }
    private fun getIpAddressFromPacket(p: ByteArray, o: Int) = "${p[o].toInt() and 0xFF}.${p[o+1].toInt() and 0xFF}.${p[o+2].toInt() and 0xFF}.${p[o+3].toInt() and 0xFF}"
    private fun getShort(p: ByteArray, o: Int) = ((p[o].toInt() and 0xFF) shl 8) or (p[o+1].toInt() and 0xFF)
    private fun getLong(p: ByteArray, o: Int) = ((p[o].toLong() and 0xFF) shl 24) or ((p[o+1].toLong() and 0xFF) shl 16) or ((p[o+2].toLong() and 0xFF) shl 8) or (p[o+3].toLong() and 0xFF)

    private fun buildUdp(sIp: Int, sP: Int, dP: Int, data: ByteArray): ByteArray {
        val total = 28 + data.size
        val p = ByteArray(total)
        p[0] = 0x45; p[2] = (total shr 8).toByte(); p[3] = (total and 0xFF).toByte()
        p[8] = 64; p[9] = 17
        setInt(p, 12, sIp); setInt(p, 16, localIpInt)
        val ck = calculateChecksum(p, 20); p[10] = (ck shr 8).toByte(); p[11] = (ck and 0xFF).toByte()
        p[20] = (sP shr 8).toByte(); p[21] = (sP and 0xFF).toByte()
        p[22] = (dP shr 8).toByte(); p[23] = (dP and 0xFF).toByte()
        val uLen = 8 + data.size; p[24] = (uLen shr 8).toByte(); p[25] = (uLen and 0xFF).toByte()
        System.arraycopy(data, 0, p, 28, data.size)
        return p
    }

    private fun buildTcp(sIp: Int, sP: Int, dP: Int, data: ByteArray?, seq: Long, ack: Long, flags: Int): ByteArray {
        val isSynAck = flags == 0x12; val optLen = if (isSynAck) 4 else 0
        val dSize = data?.size ?: 0; val total = 40 + optLen + dSize
        val p = ByteArray(total)
        p[0] = 0x45; p[2] = (total shr 8).toByte(); p[3] = (total and 0xFF).toByte()
        p[8] = 64; p[9] = 6
        setInt(p, 12, sIp); setInt(p, 16, localIpInt)
        val ck = calculateChecksum(p, 20); p[10] = (ck shr 8).toByte(); p[11] = (ck and 0xFF).toByte()
        p[20] = (sP shr 8).toByte(); p[21] = (sP and 0xFF).toByte()
        p[22] = (dP shr 8).toByte(); p[23] = (dP and 0xFF).toByte()
        p[24] = (seq shr 24).toByte(); p[25] = (seq shr 16).toByte(); p[26] = (seq shr 8).toByte(); p[27] = (seq and 0xFF).toByte()
        p[28] = (ack shr 24).toByte(); p[29] = (ack shr 16).toByte(); p[30] = (ack shr 8).toByte(); p[31] = (ack and 0xFF).toByte()
        p[32] = ((5 + optLen/4) shl 4).toByte(); p[33] = flags.toByte()
        p[34] = 0x72.toByte(); p[35] = 0x10.toByte()
        if (isSynAck) { val mss = mtu - 60; p[40] = 2; p[41] = 4; p[42] = (mss shr 8).toByte(); p[43] = (mss and 0xFF).toByte() }
        data?.let { System.arraycopy(it, 0, p, 40 + optLen, it.size) }
        val tCk = calculateTcpChecksum(sIp, localIpInt, p.copyOfRange(20, total))
        p[36] = (tCk shr 8).toByte(); p[37] = (tCk and 0xFF).toByte()
        return p
    }

    private fun calculateTcpChecksum(sIp: Int, dIp: Int, seg: ByteArray): Int {
        var sum = 0L
        sum += ((sIp ushr 16) and 0xFFFF) + (sIp and 0xFFFF)
        sum += ((dIp ushr 16) and 0xFFFF) + (dIp and 0xFFFF)
        sum += 6L + seg.size
        for (i in 0 until seg.size - 1 step 2) sum += ((seg[i].toInt() and 0xFF) shl 8) or (seg[i+1].toInt() and 0xFF)
        if (seg.size % 2 != 0) sum += (seg.last().toInt() and 0xFF) shl 8
        while (sum shr 16 > 0) sum = (sum and 0xFFFF) + (sum shr 16)
        return (sum.inv() and 0xFFFF).toInt()
    }

    private fun calculateChecksum(b: ByteArray, len: Int): Int {
        var sum = 0L
        for (i in 0 until len - 1 step 2) sum += ((b[i].toInt() and 0xFF) shl 8) or (b[i+1].toInt() and 0xFF)
        if (len % 2 != 0) sum += (b[len - 1].toInt() and 0xFF) shl 8
        while (sum shr 16 > 0) sum = (sum and 0xFFFF) + (sum shr 16)
        return (sum.inv() and 0xFFFF).toInt()
    }
}
