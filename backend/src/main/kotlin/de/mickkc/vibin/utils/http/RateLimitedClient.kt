package de.mickkc.vibin.utils.http

import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.time.Duration.Companion.milliseconds

/**
 * Client-side rate limiter using a fixed window. At most [RatelimitConfig.windowSize] requests are allowed per window
 * of [RatelimitConfig.windowDelayMs] milliseconds. Requests are additionally spaced by at least
 * [RatelimitConfig.requestDelayMs] milliseconds.
 */
open class RateLimitedClient(val config: RatelimitConfig) {

    private val requests = mutableListOf<RateLimitRequest>()
    private val mutex = Mutex()

    private val logger = LoggerFactory.getLogger(RateLimitedClient::class.java)

    init {
        require(config.windowSize > 0) { "windowSize must be positive" }
        require(config.windowDelayMs > 0) { "windowDelayMs must be positive" }
        require(config.requestDelayMs >= 0) { "requestDelayMs must not be negative" }
    }

    open fun now() = System.currentTimeMillis()

    private val maxRetries = 5

    /**
     * Drops all requests older that the start of the current window.
     * Must only be called while holding [mutex].
     */
    private fun pruneExpiredRequestsLocked(now: Long) {
        requests.removeAll { it.timestamp + config.windowDelayMs <= now }
    }

    /**
     * Calculates the delay until the next request may be sent, based on the requests inside the current window.
     * Must only be called while holding [mutex].
     */
    private fun computeNextRequestDelay(): Long {
        val now = now()
        pruneExpiredRequestsLocked(now)
        if (requests.isEmpty()) return 0L
        return if (requests.size >= config.windowSize) {
            (requests.first().timestamp + config.windowDelayMs - now).coerceAtLeast(0)
        } else {
            (requests.last().timestamp + config.requestDelayMs - now).coerceAtLeast(0)
        }
    }

    /**
     * Checks whether a request can be sent immediately.
     *
     * @return true if [computeNextRequestDelay] is 0, false otherwise.
     */
    suspend fun canSendNow(): Boolean = mutex.withLock { computeNextRequestDelay() <= 0L }

    /**
     * Records a request sent at the current time, dropping requests that are no longer inside the window.
     * Must only be called while holding [mutex].
     */
    private fun recordRequestLocked() {
        val now = now()
        pruneExpiredRequestsLocked(now)
        requests.add(RateLimitRequest(now()))
    }

    /**
     * Waits until a request may be sent, records it, and runs [block].
     *
     * If the response is 429 (Too Many Requests) or 503 (Service Unavailable) and has a Retry-After header, waits the
     * requested time and retries the request.
     *
     * @param block The request to run once the rate limit allows it.
     * @return The response of the first attempt that is not retried.
     */
    suspend fun waitAndRun(block: suspend () -> HttpResponse): HttpResponse {
        var attempts = 0
        while (true) {
            val waitMs = mutex.withLock {
                val delay = computeNextRequestDelay()
                if (delay <= 0L) {
                    recordRequestLocked()
                }
                delay
            }
            if (waitMs <= 0L) {
                val response = block()
                if (response.status == HttpStatusCode.TooManyRequests || response.status == HttpStatusCode.ServiceUnavailable) {
                    val retryAfterMs = parseRetryAfterMillis(response.headers[HttpHeaders.RetryAfter], now())
                    if (retryAfterMs != null && attempts < maxRetries) {
                        attempts++
                        logger.info("Request returned ${response.status.value}, retrying in ${retryAfterMs}ms (Retry-After)...")
                        delay(retryAfterMs.milliseconds)
                        continue
                    }
                }
                return response
            }
            logger.info("Waiting ${waitMs}ms to avoid rate-limiting...")
            delay(waitMs.milliseconds)
        }
    }

    /**
     * Parses a `Retry-After` header value into a delay in milliseconds relative to [nowMs].
     * Supports both the delay-seconds (`Retry-After: 120`) and the HTTP-date format (`Retry-After: Wed, 21 Oct 2015 07:28:00 GMT`).
     *
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Retry-After">Mozilla docs</a>
     *
     * @return The delay in milliseconds (at least 0), or null if the header is missing or invalid.
     */
    private fun parseRetryAfterMillis(header: String?, nowMs: Long): Long? {
        if (header == null) return null
        val value = header.trim()
        value.toLongOrNull()?.let { return (it * 1000).coerceAtLeast(0) }
        return try {
            val date = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)
            (date.toInstant().toEpochMilli() - nowMs).coerceAtLeast(0)
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
