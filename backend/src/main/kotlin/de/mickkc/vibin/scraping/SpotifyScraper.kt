package de.mickkc.vibin.scraping

import de.mickkc.vibin.parsing.AlbumMetadata
import de.mickkc.vibin.parsing.ArtistMetadata
import de.mickkc.vibin.parsing.TrackInfoMetadata
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.WebDriverWait
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Duration

class SpotifyScraper {
    private val logger: Logger = LoggerFactory.getLogger(SpotifyScraper::class.java)

    private fun encodePathSegment(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8)
            .replace("+", "%20")

    /**
     * Searches for tracks.
     *
     * @param query The search query
     * @param limit The maximum amount of results (0 means all that were found, which is usually 30)
     * @return A list of found albums, or null if an error occurred
     */
    fun searchTracks(query: String, limit: Int = 0): List<TrackInfoMetadata>? = ScrapingChromeDrivers.withDriver { driver ->
        try {
            val urlSafeQuery = encodePathSegment(query)
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

            val limited = if (limit > 0) elements.take(limit) else elements

            return@withDriver limited.mapNotNull { e ->
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

    /**
     * Executes a search and collects the results.
     * Used for searches that return "category cards": artists and albums.
     *
     * @throws org.openqa.selenium.TimeoutException If the timeout expires.
     * @param driver The chrome driver to use
     * @param url The search url
     * @param limit The maximum amount of results (0 means all that were found, which is usually 30)
     * @return A list of WebElements containing the results
     */
    private fun searchCategoryCards(driver: ChromeDriver, url: String, limit: Int): List<WebElement> {
        logger.info("Opening search page '$url'...")
        driver.get(url)

        val wait = WebDriverWait(driver, Duration.ofSeconds(5))
        val elements = wait.until { driver ->
            logger.info("Waiting for search results to appear...")
            val foundElements = driver.findElements(By.cssSelector("div[data-testid^='search-category-card']"))
            foundElements.ifEmpty { null }
        }
        logger.info("Found ${elements?.size} results.")

        return if (limit > 0) elements.take(limit) else elements
    }

    /**
     * Fetches the description for an artist.
     * This opens a new page inside the specified ChromeDriver.
     *
     * @param driver The chrome driver to use
     * @param nameLink The <a> element linking to the artist
     * @return The description or null, if an error occurred
     */
    private fun fetchArtistDescription(driver: ChromeDriver, nameLink: WebElement): String? {
        val artistName = nameLink.text
        try {
            val artistUrl = nameLink.getAttribute("href") ?: return null

            val wait = WebDriverWait(driver, Duration.ofSeconds(5))

            logger.info("Requesting info for artist '$artistName'...")
            driver.get(artistUrl)

            val element = wait.until { driver ->
                logger.info("Waiting for artist metadata to appear...")
                val foundElements = driver.findElements(By.cssSelector("button[aria-label=\"$artistName\"]"))
                foundElements.ifEmpty { null }
            }.singleOrNull()

            // First line is the "monthly listeners" text
            return element?.text?.replaceBefore('\n', "")?.trim()
        }
        catch (e: Exception) {
            logger.error("Failed to scrape info for artist '$artistName', skipping...", e)
            return null
        }
    }

    /**
     * Searches for artists, optionally fetching the description for each artist (Warning: VERY SLOW!).
     *
     * @param query The search query
     * @param limit The maximum amount of results (0 means all that were found, which is usually 30)
     * @param fetchDescriptions If true, each artist's page gets scraped too, to get their description
     * @return A list of found albums, or null if an error occurred
     */
    fun searchArtists(query: String, limit: Int = 0, fetchDescriptions: Boolean = false): List<ArtistMetadata>? = ScrapingChromeDrivers.withDriver { driver ->
        try {
            val urlSafeQuery = encodePathSegment(query)
            val url = "https://open.spotify.com/search/$urlSafeQuery/artists"

            val elements = searchCategoryCards(driver, url, limit)

            return@withDriver elements.mapNotNull { e ->
                val imageUrl = e.findElements(By.tagName("img"))?.firstOrNull()?.getAttribute("src")

                val nameElement = e.findElements(By.cssSelector("a[href^='/artist/']"))?.firstOrNull()
                val name = nameElement?.text ?: return@mapNotNull null

                val description = if (fetchDescriptions) fetchArtistDescription(driver, nameElement) else null

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

    /**
     * Searches for albums.
     *
     * @param query The search query
     * @param limit The maximum amount of results (0 means all that were found, which is usually 30)
     * @return A list of found albums, or null if an error occurred
     */
    fun searchAlbums(query: String, limit: Int = 0): List<AlbumMetadata>? = ScrapingChromeDrivers.withDriver { driver ->
        try {
            val urlSafeQuery = encodePathSegment(query)
            val url = "https://open.spotify.com/search/$urlSafeQuery/albums"

            val elements = searchCategoryCards(driver, url, limit)

            return@withDriver elements.mapNotNull { e ->
                val coverUrl = e.findElements(By.tagName("img"))?.firstOrNull()?.getAttribute("src")
                val fullSizeCoverUrl = coverUrl?.replace("00001e02", "0000b273")

                val nameElement = e.findElements(By.cssSelector("a[href^='/album/']"))?.firstOrNull()
                val name = nameElement?.text ?: return@mapNotNull null

                val dateElement = e.findElements(By.tagName("time"))?.firstOrNull()
                val date = dateElement?.getAttribute("datetime")?.toIntOrNull()

                val artistElement = e.findElements(By.cssSelector("a[href^='/artist/']"))
                val artistNames = artistElement?.map { it.text }

                AlbumMetadata(
                    title = name,
                    coverImageUrl = fullSizeCoverUrl,
                    year = date,
                    description = null,
                    artistName = artistNames?.joinToString()
                )
            }.also {
                logger.info("Successfully scraped ${it.size} albums from Spotify.")
            }
        }
        catch (e: Exception) {
            logger.error("Failed to scrape albums from Spotify.", e)
            return@withDriver null
        }
    }
}