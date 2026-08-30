package de.mickkc.vibin.security

import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.append
import io.ktor.serialization.gson.*
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

object IpLookupManager {

    data class IpLocationInfo(
        val country: String?,
        val region_name: String?,
        val city: String?
    )

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            gson {  }
        }
    }

    private val logger = LoggerFactory.getLogger(IpLookupManager::class.java)

    private val cache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterAccess(1, TimeUnit.HOURS)
        .build<String, IpLocationInfo>()

    private val ipRegex = Regex("^(((?!25?[6-9])[12]\\d|[1-9])?\\d\\.?\\b){4}$")

    fun findIp(ip: String): String? {
        return ipRegex.findAll(ip).singleOrNull()?.value
    }

    suspend fun lookupIpAddress(ip: String): IpLocationInfo? {
        val cached = cache.getIfPresent(ip)
        if (cached != null) return cached

        try {
            val response = client.get("https://ifconfig.co/json?ip=$ip") {
                headers.append(HttpHeaders.Accept, ContentType.Application.Json)
            }

            val result = response.body<IpLocationInfo>()
            cache.put(ip, result)
            return result
        }
        catch (e: Exception) {
            logger.error("Failed to lookup IP address.", e)
            return null
        }
    }
}