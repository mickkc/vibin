package de.mickkc.vibin.scraping

import de.mickkc.vibin.parsing.TrackInfoMetadata
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.WebDriverWait
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URLEncoder
import java.time.Duration

class SpotifyScraper {
    private val logger: Logger = LoggerFactory.getLogger(SpotifyScraper::class.java)

    fun searchTracks(query: String): List<TrackInfoMetadata>? = ScrapingChromeDrivers.withDriver { driver ->
        try {
            val urlSafeQuery = URLEncoder.encode(query, Charsets.UTF_8)
            val url = "https://open.spotify.com/search/$urlSafeQuery/tracks"

            logger.info("Opening page '$url'...")
            driver.get(url)

            val wait = WebDriverWait(driver, Duration.ofSeconds(5))
            val elements = wait.until { driver ->
                logger.info("Waiting for search results to appear...")
                val foundElements = driver.findElements(By.cssSelector("div[data-testid=\"tracklist-row\"]"))
                foundElements.ifEmpty { null }
            }
            logger.info("Found ${elements?.size} results.")

            //File.createTempFile("screen", ".png").apply { writeBytes(driver.getScreenshotAs(OutputType.BYTES)) }

            return@withDriver elements.mapNotNull { e ->
                val coverUrl = e.findElement(By.tagName("img"))?.getAttribute("src")
                val fullSizeCoverUrl = coverUrl?.replace("00004851", "0000b273")

                val titleElement = e.findElement(By.cssSelector("a[href^='/track/']"))
                val title = titleElement?.text ?: return@mapNotNull null

                val artistElement = e.findElements(By.cssSelector("a[href^='/artist/']"))
                val artistNames = artistElement?.map { it.text }

                val albumElement = e.findElement(By.cssSelector("a[href^='/album/']"))
                val albumName = albumElement?.text

                val explicit = e.findElements(By.cssSelector("span[title=\"Explicit\"]"))

                TrackInfoMetadata(
                    title = title,
                    coverImageUrl = fullSizeCoverUrl,
                    artists = artistNames,
                    album = albumName,
                    explicit = explicit?.isNotEmpty()
                )
            }.also {
                logger.info("Successfully scraped ${it.size} tracks from Spotify.")
            }
        }
        catch (e: Exception) {
            logger.error("Failed to scrape tracks from Spotify.", e)
            return@withDriver null
        }
    }
}