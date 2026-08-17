package de.mickkc.vibin.scraping

import de.mickkc.vibin.config.EnvUtil
import de.mickkc.vibin.config.EnvUtil.SCRAPING_POOL_SIZE
import de.mickkc.vibin.widgets.ChromeDriverPool
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.slf4j.LoggerFactory

object ScrapingChromeDrivers {

    private val logger = LoggerFactory.getLogger(ScrapingChromeDrivers::class.java)

    private val pool by lazy {

        val poolSize = EnvUtil.getOrError(SCRAPING_POOL_SIZE)

        return@lazy ChromeDriverPool(
            maxSize = poolSize,
            factory = { ChromeDriver(createChromeOptions()) }
        )
    }

    fun <T>withDriver(action: (ChromeDriver) -> T) = pool.withDriver(action)

    private fun createChromeOptions(): ChromeOptions {
        val options = ChromeOptions()

        val binary = EnvUtil.getOrError(EnvUtil.CHROME_BINARY_PATH)

        if (binary.isNotBlank()) {
            logger.info("Using custom Chrome binary at: $binary")
            options.setBinary(binary)
        }

        options.addArguments(
            "--headless=new",
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--window-size=1920,1080"
        )
        return options
    }
}