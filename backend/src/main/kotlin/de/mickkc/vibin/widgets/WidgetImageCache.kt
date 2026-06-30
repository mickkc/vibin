package de.mickkc.vibin.widgets

import de.mickkc.vibin.config.EnvUtil
import de.mickkc.vibin.db.widgets.SharedWidgetEntity
import de.mickkc.vibin.utils.RateLimiter
import org.openqa.selenium.Dimension
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.io.path.createTempFile

/**
 * Manages the caching and generation of widget images using Selenium.
 * This cache stores pre-rendered PNG images of widgets to avoid
 * regenerating them on every request.
 */
object WidgetImageCache {

    private val logger: Logger = LoggerFactory.getLogger(WidgetImageCache::class.java)
    private val widgetImageCachePath = EnvUtil.getOrError(EnvUtil.WIDGET_IMAGE_CACHE_DIR)

    val rateLimiter = RateLimiter(
        maxRequests = EnvUtil.getOrError(EnvUtil.WIDGET_IMAGE_CACHE_RATE_LIMIT_REQUESTS),
        windowMinutes = EnvUtil.getOrError(EnvUtil.WIDGET_CACHE_EXPIRATION_MINUTES)
    )

    private val cacheExpirationMinutes = EnvUtil.getOrError(EnvUtil.WIDGET_CACHE_EXPIRATION_MINUTES)

    private const val DEFAULT_HEIGHT = 720

    private const val DEFAULT_WIDTH = 1080

    /**
     * The size of the Chrome window decorations.
     * This needs to be added to the auto-detected widget height to get the actual Chrome window size.
     */
    @Volatile
    private var viewportHeightOffset: Int? = null

    /**
     * Pool of long-lived Chrome drivers used to render widgets.
     */
    private val driverPool = ChromeDriverPool(
        maxSize = EnvUtil.getOrDefault(
            EnvUtil.WIDGET_IMAGE_RENDER_POOL_SIZE,
            EnvUtil.DEFAULT_WIDGET_IMAGE_RENDER_POOL_SIZE
        ).toInt()
    ) {
        ChromeDriver(createChromeOptions(DEFAULT_WIDTH, DEFAULT_HEIGHT))
    }

    init {
        if (!widgetImageCachePath.exists()) {
            widgetImageCachePath.mkdirs()
            logger.info("Created widget image cache directory: ${widgetImageCachePath.absolutePath}")
        }
        logger.info("Widget image cache expiration set to $cacheExpirationMinutes minutes")

        Runtime.getRuntime().addShutdownHook(Thread {
            logger.info("Shutting down widget image render pool")
            driverPool.close()
        })
    }

    /**
     * Gets a cached widget image or generates a new one if not cached.
     *
     * If [height] is null, it is calculated based on the width and widget size.
     *
     * @param widget The widget entity
     * @param widgetTypes The list of widget types to render
     * @param ctx The widget context containing colors, language, etc.
     * @param width The desired image width
     * @param height The desired image height, or null to derive it from the natural size
     * @return The widget image as a byte array (PNG format)
     */
    fun getOrCreateImage(
        widget: SharedWidgetEntity,
        widgetTypes: List<WidgetType>,
        ctx: WidgetContext,
        width: Int,
        height: Int? = null
    ): ByteArray {
        val cacheKey = generateCacheKey(widget.id.value, ctx, width, height)
        val cacheFile = File(widgetImageCachePath, "$cacheKey.png")

        // Check if cached image exists and is still valid
        if (cacheFile.exists()) {
            if (!isCacheFileExpired(cacheFile)) {
                return cacheFile.readBytes()
            } else {
                try {
                    cacheFile.delete()
                } catch (e: Exception) {
                    logger.warn("Failed to delete expired cache file: ${cacheFile.absolutePath}", e)
                }
            }
        }

        logger.info("Generating widget image: $cacheKey (${width}x${height ?: "auto"})")
        val timeStart = System.currentTimeMillis()

        val html = WidgetBuilder.build(widgetTypes, ctx, interactive = false)
        val imageBytes = generateImage(html, width, height)

        try {
            cacheFile.writeBytes(imageBytes)
            logger.info("Cached widget image: $cacheKey")
        } catch (e: Exception) {
            logger.error("Failed to cache widget image: $cacheKey", e)
        }

        val timeEnd = System.currentTimeMillis()
        logger.info("Widget image generation took ${timeEnd - timeStart} ms.")

        return imageBytes
    }

    /**
     * Generates a widget image from HTML using Selenium Chrome driver.
     *
     * @param html The HTML content to render
     * @param width The desired image width
     * @param height The desired image height, or null to derive it from the natural content height
     * @return The rendered image as a byte array (PNG format)
     */
    private fun generateImage(html: String, width: Int, height: Int?): ByteArray {
        // Replace relative URLs with absolute URLs for Selenium
        val processedHtml = prepareHtml(html)

        val tempHtmlFile = createTempFile(suffix = ".html").toFile()
        try {
            tempHtmlFile.writeText(processedHtml)
            val uri = tempHtmlFile.toURI().toString()

            return driverPool.withDriver { driver ->
                driver.manage().window().size = Dimension(width, height ?: DEFAULT_HEIGHT)
                driver.get(uri)

                if (height == null) {
                    val launchHeight = measureLaunchHeightForWidget(driver, width)
                    driver.manage().window().size = Dimension(width, launchHeight)
                }

                (driver as TakesScreenshot).getScreenshotAs(OutputType.BYTES)
            }
        } finally {
            try {
                tempHtmlFile.delete()
            } catch (e: Exception) {
                logger.warn("Failed to delete temp HTML file: ${tempHtmlFile.absolutePath}", e)
            }
        }
    }

    /**
     * Determines the window height that renders the widget currently loaded in [driver] with a viewport exactly as
     * tall as its natural content.
     *
     * @param driver A driver that has already loaded the widget at viewport width [width]
     * @param width The viewport width the widget was rendered at
     * @return The window height to resize [driver] to before taking the final screenshot
     */
    private fun measureLaunchHeightForWidget(driver: ChromeDriver, width: Int): Int {
        val naturalHeight = measureWidgetHeight(driver)
        val offset = viewportHeightOffset ?: (DEFAULT_HEIGHT - measureWindowHeight(driver)).also {
            viewportHeightOffset = it
        }
        logger.debug("Detected natural content height $naturalHeight for width $width (viewport offset: $offset)")
        return naturalHeight + offset
    }

    private fun measureWidgetHeight(driver: ChromeDriver): Int {
        // document.body.scrollHeight didn't work.
        // document.body.clientHeight didn't work.
        // document.documentElement.scrollHeight didn't work (even though it works on firefox).
        // document.documentElement.clientHeight didn't work (even though it works on firefox).
        // >:(
        val result = (driver as JavascriptExecutor).executeScript("return +getComputedStyle(document.body).height.replace(\"px\", \"\");")
        return (result as? Number)?.toInt() ?: DEFAULT_HEIGHT
    }

    private fun measureWindowHeight(driver: ChromeDriver): Int {
        val result = (driver as JavascriptExecutor).executeScript("return window.innerHeight;")
        return (result as? Number)?.toInt() ?: DEFAULT_HEIGHT
    }

    /**
     * Creates consistent ChromeOptions for widget rendering.
     *
     * @param width The viewport width
     * @param height The viewport height
     * @return Configured ChromeOptions
     */
    private fun createChromeOptions(width: Int, height: Int): ChromeOptions {
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
            "--window-size=$width,$height"
        )
        return options
    }

    /**
     * Generates a cache key based on widget parameters.
     * The cache key is a hash of the widget ID and rendering parameters.
     *
     * @param widgetId The widget ID
     * @param ctx The widget context
     * @param width The image width
     * @param height The image height, or null for auto
     * @return A unique cache key string
     */
    private fun generateCacheKey(
        widgetId: String,
        ctx: WidgetContext,
        width: Int,
        height: Int?
    ): String {
        return "${widgetId}_${ctx.userId}_${ctx.backgroundColor}_" +
                "${ctx.foregroundColor}_${ctx.accentColor}_${ctx.language}_${width}_${height ?: "auto"}"
    }

    /**
     * Evicts all cached images for a specific widget.
     * This should be called when a widget is updated or deleted.
     *
     * @param widgetId The widget ID
     */
    fun evictCacheForWidget(widgetId: String) {
        val cachedFiles = widgetImageCachePath.listFiles { file ->
            file.name.endsWith(".png") && file.name.startsWith("${widgetId}_")
        } ?: return

        var evictedCount = 0
        for (file in cachedFiles) {
            try {
                if (file.delete()) {
                    evictedCount++
                }
                else {
                    logger.error("Failed to evict cached widget image: ${file.absolutePath}")
                }
            } catch (e: Exception) {
                logger.error("Error evicting cached widget image: ${file.absolutePath}", e)
            }
        }

        if (evictedCount > 0) {
            logger.info("Evicted $evictedCount cached widget images for widget: $widgetId")
        }
    }

    /**
     * Cleans up expired cached images.
     *
     * @return The number of expired images that were removed and total bytes freed
     */
    fun cleanupExpiredCache(): Pair<Int, Long> {
        val cachedFiles = widgetImageCachePath.listFiles { file ->
            file.name.endsWith(".png")
        } ?: return 0 to 0

        var cleanedCount = 0
        var totalFreedBytes = 0L

        for (file in cachedFiles) {
            try {
                if (isCacheFileExpired(file)) {
                    val length = file.length()
                    if (file.delete()) {
                        cleanedCount++
                        totalFreedBytes += length
                    }
                    else {
                        logger.error("Failed to clean up cached widget image: ${file.absolutePath}")
                    }
                }
            } catch (e: Exception) {
                logger.error("Error cleaning up cached widget image: ${file.absolutePath}", e)
            }
        }

        if (cleanedCount > 0) {
            logger.info("Cleaned up $cleanedCount expired widget images")
        }

        return cleanedCount to totalFreedBytes
    }

    fun isCacheFileExpired(file: File): Boolean {
        val age = System.currentTimeMillis() - file.lastModified()
        val outOfDate = age >= cacheExpirationMinutes * 60 * 1000
        if (outOfDate) {
            logger.debug("Cache file ${file.name} is out of date (age: ${age / (60 * 1000)} minutes)")
        }
        return outOfDate
    }

    fun prepareHtml(html: String): String {
        return html
            .replace("/api/", "http://localhost:8080/api/") // Replace with absolute URLs
    }
}