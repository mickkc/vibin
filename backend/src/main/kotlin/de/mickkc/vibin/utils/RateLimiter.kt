package de.mickkc.vibin.utils

import java.util.concurrent.ConcurrentHashMap

class RateLimiter(
    private val maxRequests: Int,
    private val windowMinutes: Int
) {
    init {
        require(maxRequests > 0) { "maxRequests must be positive" }
        require(windowMinutes > 0) { "windowMinutes must be positive" }
    }

    private data class RateLimitEntry(
        val count: Int,
        val windowStart: Long
    )

    private val rateLimits = ConcurrentHashMap<String, RateLimitEntry>()

    /**
     * Checks if a client has exceeded the rate limit and updates the count.
     *
     * @param clientId The unique identifier for the client
     * @return true if the request is allowed, false if rate limit is exceeded
     */
    fun checkAndIncrement(clientId: String): Boolean {
        val windowStart = DateTimeUtils.now() - (windowMinutes * 60)

        val entry = rateLimits.compute(clientId) { _, existing ->
            if (existing == null || existing.windowStart < windowStart) {
                // New window or expired window - reset count
                RateLimitEntry(1, DateTimeUtils.now())
            } else {
                // Within current window - increment count
                RateLimitEntry(existing.count + 1, existing.windowStart)
            }
        }

        return entry!!.count <= maxRequests
    }

    /**
     * Checks if a client would exceed the rate limit without incrementing the count.
     *
     * @param clientId The unique identifier for the client
     * @return true if the client is within rate limit, false otherwise
     */
    fun check(clientId: String): Boolean {
        val windowStart = DateTimeUtils.now() - (windowMinutes * 60)

        val entry = rateLimits[clientId] ?: return true

        // Check if entry is still valid
        if (entry.windowStart < windowStart) {
            return true // Window expired
        }

        return entry.count < maxRequests
    }

    /**
     * Gets the current request count for a client.
     *
     * @param clientId The unique identifier for the client
     * @return The current count, or 0 if no requests have been made or window expired
     */
    fun getCurrentCount(clientId: String): Int {
        val windowStart = DateTimeUtils.now() - (windowMinutes * 60)

        val entry = rateLimits[clientId] ?: return 0

        if (entry.windowStart < windowStart) {
            return 0
        }

        return entry.count
    }

    /**
     * Gets the remaining requests allowed for a client in the current window.
     *
     * @param clientId The unique identifier for the client
     * @return The number of remaining requests
     */
    fun getRemainingRequests(clientId: String): Int {
        return (maxRequests - getCurrentCount(clientId)).coerceAtLeast(0)
    }

    /**
     * Resets the rate limit counter for a specific client.
     *
     * @param clientId The unique identifier for the client
     */
    fun reset(clientId: String) {
        rateLimits.remove(clientId)
    }

    /**
     * Cleans up expired entries.
     */
    private fun cleanupOldEntries() {
        val windowStart = DateTimeUtils.now() - (windowMinutes * 60)

        rateLimits.entries.removeIf { (_, entry) ->
            entry.windowStart < windowStart
        }
    }
}