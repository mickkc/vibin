package de.mickkc.vibin.widgets

import de.mickkc.vibin.config.EnvUtil
import de.mickkc.vibin.db.widgets.SharedWidgetEntity
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
    private val widgetImageCachePath = File(
        EnvUtil.getOrDefault(EnvUtil.WIDGET_IMAGE_CACHE_DIR, EnvUtil.DEFAULT_WIDGET_IMAGE_CACHE_DIR)
    )

    private val cacheExpirationMinutes = EnvUtil.getOrDefault(EnvUtil.WIDGET_CACHE_EXPIRATION_MINUTES, EnvUtil.DEFAULT_WIDGET_CACHE_EXPIRATION_MINUTES).toLong()

    init {
        if (!widgetImageCachePath.exists()) {
            widgetImageCachePath.mkdirs()
            logger.info("Created widget image cache directory: ${widgetImageCachePath.absolutePath}")
        }
        logger.info("Widget image cache expiration set to $cacheExpirationMinutes minutes")
    }

    /**
     * Gets a cached widget image or generates a new one if not cached.
     *
     * @param widget The widget entity
     * @param widgetTypes The list of widget types to render
     * @param ctx The widget context containing colors, language, etc.
     * @param width The desired image width
     * @param height The desired image height
     * @return The widget image as a byte array (PNG format)
     */
    fun getOrCreateImage(
        widget: SharedWidgetEntity,
        widgetTypes: List<WidgetType>,
        ctx: WidgetContext,
        width: Int,
        height: Int
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

        logger.info("Generating widget image: $cacheKey (${width}x${height})")
        val html = WidgetBuilder.build(widgetTypes, ctx)
        val imageBytes = generateImage(html, width, height)

        try {
            cacheFile.writeBytes(imageBytes)
            logger.info("Cached widget image: $cacheKey")
        } catch (e: Exception) {
            logger.error("Failed to cache widget image: $cacheKey", e)
        }

        return imageBytes
    }

    /**
     * Generates a widget image from HTML using Selenium Chrome driver.
     *
     * @param html The HTML content to render
     * @param width The desired image width
     * @param height The desired image height
     * @return The rendered image as a byte array (PNG format)
     */
    private fun generateImage(html: String, width: Int, height: Int): ByteArray {
        // Replace relative URLs with absolute URLs for Selenium
        val processedHtml = prepareHtml(html)

        val options = createChromeOptions(width, height)
        val driver = ChromeDriver(options)

        try {
            val tempHtmlFile = createTempFile(suffix = ".html").toFile()
            try {
                tempHtmlFile.writeText(processedHtml)
                driver.get(tempHtmlFile.toURI().toString())

                val screenshot = (driver as TakesScreenshot).getScreenshotAs(OutputType.BYTES)
                return screenshot
            } finally {
                try {
                    tempHtmlFile.delete()
                } catch (e: Exception) {
                    logger.warn("Failed to delete temp HTML file: ${tempHtmlFile.absolutePath}", e)
                }
            }
        } finally {
            try {
                driver.quit()
            } catch (e: Exception) {
                logger.error("Error quitting ChromeDriver", e)
            }
        }
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
        options.addArguments(
            "--headless=new",
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
     * @param height The image height
     * @return A unique cache key string
     */
    private fun generateCacheKey(
        widgetId: String,
        ctx: WidgetContext,
        width: Int,
        height: Int
    ): String {
        return "${widgetId}_${ctx.userId}_${ctx.backgroundColor}_" +
                "${ctx.foregroundColor}_${ctx.accentColor}_${ctx.language}_${width}_${height}"
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
     * @return The number of expired images that were removed
     */
    fun cleanupExpiredCache(): Int {
        val cachedFiles = widgetImageCachePath.listFiles { file ->
            file.name.endsWith(".png")
        } ?: return 0

        var cleanedCount = 0

        for (file in cachedFiles) {
            try {
                if (isCacheFileExpired(file)) {
                    if (file.delete()) {
                        cleanedCount++
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

        return cleanedCount
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
            .replace(Regex("<a [^>]*class=\"btn\"[^>]*>.*?</a>"), "") // Remove buttons
    }
}