package de.mickkc.vibin.utils.http

/**
 * @param windowSize Maximum number of requests allowed per window.
 * @param windowDelayMs Length of the window in milliseconds.
 * @param requestDelayMs Minimum delay in milliseconds between two requests.
 */
data class RatelimitConfig(
    val windowSize: Int,
    val windowDelayMs: Long,
    val requestDelayMs: Long = 0
)
