package de.mickkc.vibin.widgets

import org.openqa.selenium.chrome.ChromeDriver
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Semaphore

/**
 * A bounded pool of reusable [ChromeDriver] instances.
 *
 * @param maxSize The maximum number of drivers (and therefore concurrent renders) the pool allows
 * @param factory Creates a new, ready-to-use [ChromeDriver]
 */
class ChromeDriverPool(
    private val maxSize: Int,
    private val factory: () -> ChromeDriver
) {
    private val logger: Logger = LoggerFactory.getLogger(ChromeDriverPool::class.java)

    private val permits = Semaphore(maxSize)

    /**
     * Drivers that are alive and available.
     */
    private val idle = ConcurrentLinkedQueue<ChromeDriver>()

    @Volatile
    private var closed = false

    /**
     * Gets a driver from the pool (creating one if none are available), runs the [action] with exclusive access to it,
     * and returns it to the pool afterward. Blocks until a driver is available.
     *
     * If [action] throws an exception, the driver is quit rather than reused.
     *
     * @param action The action to run on the driver
     * @return The result of [action]
     */
    fun <T> withDriver(action: (ChromeDriver) -> T): T {
        check(!closed) { "ChromeDriverPool is closed" }

        permits.acquire()
        val driver = try {
            idle.poll() ?: createDriver()
        } catch (e: Throwable) {
            permits.release()
            throw e
        }

        var healthy = true
        try {
            return action(driver)
        } catch (e: Throwable) {
            healthy = false
            throw e
        } finally {
            if (healthy && !closed) {
                idle.offer(driver)
            } else {
                quitDriver(driver)
            }
            permits.release()
        }
    }

    private fun createDriver(): ChromeDriver {
        logger.debug("Creating new ChromeDriver for pool")
        return factory()
    }

    private fun quitDriver(driver: ChromeDriver) {
        try {
            driver.quit()
        } catch (e: Exception) {
            logger.error("Error quitting ChromeDriver", e)
        }
    }

    /**
     * Quits all idle drivers and prevents the pool from handing out new ones.
     */
    fun close() {
        closed = true
        while (true) {
            val driver = idle.poll() ?: break
            quitDriver(driver)
        }
    }
}
