package io.github.immaghzbad.aetherst.core

import java.util.Collections

object DnsMap {
    private val maxEntries = 2000
    
    private val ipToDomain = Collections.synchronizedMap(object : LinkedHashMap<String, String>(128, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean = size > maxEntries
    })

    fun put(ip: String, domain: String) {
        ipToDomain[ip] = domain
    }

    fun get(ip: String): String? {
        return ipToDomain[ip]
    }

    fun clear() {
        ipToDomain.clear()
    }
}
