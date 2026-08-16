package de.mickkc.vibin.scraping

import de.mickkc.vibin.parsing.ArtistMetadata
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

            logger.info("Opening track search page '$url'...")
            driver.get(url)

            val wait = WebDriverWait(driver, Duration.ofSeconds(5))
            val elements = wait.until { driver ->
                logger.info("Waiting for track search results to appear...")
                val foundElements = driver.findElements(By.cssSelector("div[data-testid=\"tracklist-row\"]"))
                foundElements.ifEmpty { null }
            }
            logger.info("Found ${elements?.size} track results.")

            //File.createTempFile("screen", ".png").apply { writeBytes(driver.getScreenshotAs(OutputType.BYTES)) }

            return@withDriver elements.mapNotNull { e ->
                val coverUrl = e.findElements(By.tagName("img"))?.singleOrNull()?.getAttribute("src")
                val fullSizeCoverUrl = coverUrl?.replace("00004851", "0000b273")

                val titleElement = e.findElements(By.cssSelector("a[href^='/track/']"))?.singleOrNull()
                val title = titleElement?.text ?: return@mapNotNull null

                val artistElement = e.findElements(By.cssSelector("a[href^='/artist/']"))
                val artistNames = artistElement?.map { it.text }

                val albumElement = e.findElements(By.cssSelector("a[href^='/album/']"))?.singleOrNull()
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

    fun searchArtists(query: String, limit: Int = 0, fetchDescriptions: Boolean = false): List<ArtistMetadata>? = ScrapingChromeDrivers.withDriver { driver ->
        try {
            val urlSafeQuery = URLEncoder.encode(query, Charsets.UTF_8)
            val url = "https://open.spotify.com/search/$urlSafeQuery/artists"

            logger.info("Opening artist search page '$url'...")
            driver.get(url)

            val wait = WebDriverWait(driver, Duration.ofSeconds(5))
            val elements = wait.until { driver ->
                logger.info("Waiting for search results to appear...")
                val foundElements = driver.findElements(By.cssSelector("div[data-testid^='search-category-card']"))
                foundElements.ifEmpty { null }
            }
            logger.info("Found ${elements?.size} artist results.")

            val limited = if (limit > 0) elements.take(limit) else elements

            return@withDriver limited.mapNotNull { e ->
                val imageUrl = e.findElements(By.tagName("img"))?.firstOrNull()?.getAttribute("src")

                val nameElement = e.findElements(By.cssSelector("a[href^='/artist/']"))?.firstOrNull()
                val name = nameElement?.text ?: return@mapNotNull null

                var description: String? = null

                if (fetchDescriptions) {
                    try {
                        logger.info("Requesting info for artist '$name'...")
                        driver.get(nameElement.getAttribute("href"))
                        val element = wait.until { driver ->
                            logger.info("Waiting for artist metadata to appear...")
                            val foundElements = driver.findElements(By.cssSelector("button[aria-label=\"$name\"]"))
                            foundElements.ifEmpty { null }
                        }.singleOrNull()

                        if (element != null) {
                            // First line is the "monthly listeners" text
                            description = element.text.replaceBefore('\n', "").trim()
                        }
                    }
                    catch (e: Exception) {
                        logger.error("Failed to scrape info for artist '$name', skipping...", e)
                    }
                }

                ArtistMetadata(
                    name = name,
                    pictureUrl = imageUrl,
                    biography = description
                )
            }.also {
                logger.info("Successfully scraped ${it.size} artists from Spotify.")
            }
        }
        catch (e: Exception) {
            logger.error("Failed to scrape tracks from Spotify.", e)
            return@withDriver null
        }
    }
}