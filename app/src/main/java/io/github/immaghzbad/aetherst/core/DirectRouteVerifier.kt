package io.github.immaghzbad.aetherst.core

import android.net.Network
import io.github.immaghzbad.aetherst.data.IpInfoRepository
import io.github.immaghzbad.aetherst.data.LogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object DirectRouteVerifier {
    private data class ExitInfo(
        val ip: String,
        val country: String,
        val countryCode: String,
        val city: String,
        val isp: String
    )

    private const val DOMAIN_COOLDOWN_MS = 300_000L
    private const val GLOBAL_COOLDOWN_MS = 3_000L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lastVerifiedAt = ConcurrentHashMap<String, Long>()
    private val inFlight = ConcurrentHashMap.newKeySet<String>()
    private val lastApiRequestAt = AtomicLong(0)
    private val apiMutex = Mutex()

    fun verify(domain: String, network: Network, networkType: String) {
        val normalizedDomain = domain.trim().trimEnd('.').lowercase(Locale.ROOT)
        if (normalizedDomain.isEmpty()) return
        val key = "$normalizedDomain:${network}"
        val now = System.currentTimeMillis()
        if (now - (lastVerifiedAt[key] ?: 0L) < DOMAIN_COOLDOWN_MS) return
        if (!inFlight.add(key)) return

        scope.launch {
            try {
                val direct = apiMutex.withLock {
                    val waitMs = GLOBAL_COOLDOWN_MS - (System.currentTimeMillis() - lastApiRequestAt.get())
                    if (waitMs > 0) delay(waitMs)
                    lastApiRequestAt.set(System.currentTimeMillis())
                    fetchIpWhoIs(network) ?: fetchIpApi(network)
                }
                if (direct == null) {
                    LogRepository.w("[DirectVerify] FAILED domain=$normalizedDomain network=$networkType reason=api_unavailable", "DirectVerify")
                    return@launch
                }

                var tunnel = IpInfoRepository.ipInfo.value
                repeat(10) {
                    if (tunnel.ip.isNotEmpty()) return@repeat
                    delay(500L)
                    tunnel = IpInfoRepository.ipInfo.value
                }
                val tunnelIp = tunnel.ip.ifEmpty { "unknown" }
                val result = when {
                    tunnel.ip.isEmpty() -> "DIRECT_API_CONFIRMED"
                    direct.ip != tunnel.ip -> "DIRECT_CONFIRMED"
                    else -> "ROUTE_MISMATCH"
                }
                val location = listOf(direct.city, direct.country).filter { it.isNotEmpty() }.joinToString(", ").ifEmpty { "Unknown" }
                val isp = direct.isp.ifEmpty { "Unknown" }
                LogRepository.i(
                    "[DirectVerify] $result domain=$normalizedDomain network=$networkType directIp=${direct.ip} location=$location countryCode=${direct.countryCode.ifEmpty { "--" }} isp=$isp tunnelIp=$tunnelIp tunnelCountry=${tunnel.country.ifEmpty { "Unknown" }}",
                    "DirectVerify"
                )
            } catch (exception: Exception) {
                LogRepository.w("[DirectVerify] FAILED domain=$normalizedDomain network=$networkType reason=${exception.localizedMessage}", "DirectVerify")
            } finally {
                lastVerifiedAt[key] = System.currentTimeMillis()
                inFlight.remove(key)
            }
        }
    }

    private fun fetchIpWhoIs(network: Network): ExitInfo? {
        return fetch(network, "https://ipwho.is/") { json ->
            if (!json.optBoolean("success", true)) return@fetch null
            val ip = json.optString("ip", "")
            if (ip.isEmpty()) return@fetch null
            ExitInfo(
                ip = ip,
                country = json.optString("country", ""),
                countryCode = json.optString("country_code", ""),
                city = json.optString("city", ""),
                isp = json.optJSONObject("connection")?.optString("isp", "") ?: ""
            )
        }
    }

    private fun fetchIpApi(network: Network): ExitInfo? {
        return fetch(network, "https://ipapi.co/json/") { json ->
            val ip = json.optString("ip", "")
            if (ip.isEmpty()) return@fetch null
            ExitInfo(
                ip = ip,
                country = json.optString("country_name", ""),
                countryCode = json.optString("country_code", ""),
                city = json.optString("city", ""),
                isp = json.optString("org", "")
            )
        }
    }

    private fun fetch(network: Network, endpoint: String, parser: (JSONObject) -> ExitInfo?): ExitInfo? {
        var connection: HttpURLConnection? = null
        return try {
            val conn = network.openConnection(URL(endpoint)) as HttpURLConnection
            connection = conn
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.instanceFollowRedirects = true
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "AetherST/DirectRouteVerifier")
            conn.setRequestProperty("Accept", "application/json")
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            parser(JSONObject(body))
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}
