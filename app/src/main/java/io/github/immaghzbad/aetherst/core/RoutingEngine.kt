package io.github.immaghzbad.aetherst.core

import io.github.immaghzbad.aetherst.model.RoutingMode
import io.github.immaghzbad.aetherst.model.RoutingRule
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

enum class MatchType {
    DOMAIN,
    DOMAIN_SUFFIX,
    KEYWORD,
    REGEX,
    IPV4,
    IPV6,
    CIDR,
    DNS_CACHE,
    SNI,
    HTTP_HOST,
    DEFAULT
}

data class RoutingDecision(
    val mode: RoutingMode,
    val matchedRule: RoutingRule?,
    val matchedBy: MatchType,
    val resolvedDomain: String?
)

class RoutingEngine(private val rules: List<RoutingRule>) {

    private val decisionCache = ConcurrentHashMap<String, RoutingDecision>()

    fun resolve(
        destinationIp: String,
        destinationPort: Int,
        resolvedDomain: String?,
        tlsSni: String?,
        httpHost: String?
    ): RoutingDecision {
        val cacheKey = "$destinationIp:$destinationPort:$resolvedDomain:$tlsSni:$httpHost"
        val cached = decisionCache[cacheKey]
        if (cached != null) return cached

        val decision = resolveInternal(destinationIp, destinationPort, resolvedDomain, tlsSni, httpHost)
        decisionCache[cacheKey] = decision
        return decision
    }

    private fun resolveInternal(
        destinationIp: String,
        destinationPort: Int,
        resolvedDomain: String?,
        tlsSni: String?,
        httpHost: String?
    ): RoutingDecision {
        val ipDecision = matchIpRules(destinationIp)
        if (ipDecision != null) return ipDecision

        if (resolvedDomain != null) {
            matchDomainRules(resolvedDomain, MatchType.DNS_CACHE)?.let { return it }
        }

        if (tlsSni != null) {
            matchDomainRules(tlsSni, MatchType.SNI)?.let { return it }
        }

        if (httpHost != null) {
            matchDomainRules(httpHost, MatchType.HTTP_HOST)?.let { return it }
        }
        
        matchDomainRules(destinationIp, if (destinationIp.contains(":")) MatchType.IPV6 else MatchType.IPV4)?.let { return it }

        return RoutingDecision(RoutingMode.TUNNEL, null, MatchType.DEFAULT, resolvedDomain ?: tlsSni ?: httpHost)
    }

    private fun matchIpRules(ip: String): RoutingDecision? {
        if (ip.isEmpty()) return null
        for (rule in rules) {
            val pattern = rule.pattern.lowercase()
            if (!pattern.startsWith("ip:")) continue
            val ipPattern = pattern.removePrefix("ip:")
            
            if (matchIpOrCidr(ip, ipPattern)) {
                val matchType = if (ipPattern.contains("/")) MatchType.CIDR else if (ip.contains(":")) MatchType.IPV6 else MatchType.IPV4
                return RoutingDecision(rule.mode, rule, matchType, null)
            }
        }
        return null
    }

    private fun matchDomainRules(domain: String, matchType: MatchType): RoutingDecision? {
        if (domain.isEmpty()) return null
        val lowDomain = domain.lowercase()
        for (rule in rules) {
            val pattern = rule.pattern.lowercase().removePrefix("domain:")
            if (pattern.startsWith("ip:")) continue
            
            val matched = when {
                pattern.startsWith("keyword:") -> lowDomain.contains(pattern.removePrefix("keyword:"))
                pattern.startsWith("regexp:") -> try { Regex(pattern.removePrefix("regexp:")).containsMatchIn(lowDomain) } catch (_: Exception) { false }
                else -> lowDomain == pattern || lowDomain.endsWith(".$pattern")
            }

            if (matched) {
                val matchTypeResult = when {
                    pattern.startsWith("keyword:") -> MatchType.KEYWORD
                    pattern.startsWith("regexp:") -> MatchType.REGEX
                    lowDomain == pattern -> MatchType.DOMAIN
                    lowDomain.endsWith(".$pattern") -> MatchType.DOMAIN_SUFFIX
                    else -> matchType
                }
                return RoutingDecision(rule.mode, rule, matchTypeResult, domain)
            }
        }
        return null
    }

    private fun matchIpOrCidr(ip: String, pattern: String): Boolean {
        if (ip == pattern) return true
        if (!pattern.contains("/")) return false
        
        return try {
            val parts = pattern.split("/")
            val network = InetAddress.getByName(parts[0]).address
            val addr = InetAddress.getByName(ip).address
            if (network.size != addr.size) return false
            
            val prefixLen = parts[1].toInt()
            val fullBytes = prefixLen / 8
            val remainingBits = prefixLen % 8
            
            for (i in 0 until fullBytes) {
                if (network[i] != addr[i]) return false
            }
            
            if (remainingBits > 0) {
                val mask = (0xFF shl (8 - remainingBits)).toByte()
                if ((network[fullBytes].toInt() and mask.toInt()) != (addr[fullBytes].toInt() and mask.toInt())) return false
            }
            
            true
        } catch (_: Exception) {
            false
        }
    }

    fun clearCache() {
        decisionCache.clear()
    }
}
