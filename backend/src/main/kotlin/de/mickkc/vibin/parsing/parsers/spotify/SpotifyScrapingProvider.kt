package de.mickkc.vibin.parsing.parsers.spotify

import de.mickkc.vibin.parsing.AlbumMetadata
import de.mickkc.vibin.parsing.ArtistMetadata
import de.mickkc.vibin.parsing.TrackInfoMetadata
import de.mickkc.vibin.parsing.parsers.AlbumSearchProvider
import de.mickkc.vibin.parsing.parsers.ArtistSearchProvider
import de.mickkc.vibin.parsing.parsers.FileParser
import de.mickkc.vibin.parsing.parsers.PreparseData
import de.mickkc.vibin.parsing.parsers.TrackSearchProvider
import de.mickkc.vibin.scraping.SpotifyScraper
import de.mickkc.vibin.settings.Settings
import de.mickkc.vibin.settings.server.ExtendedMetadata
import de.mickkc.vibin.settings.server.MetadataLimit

class SpotifyScrapingProvider : FileParser, TrackSearchProvider, ArtistSearchProvider, AlbumSearchProvider {

    private val scraper = SpotifyScraper()

    override suspend fun searchTrack(query: String): List<TrackInfoMetadata>? {

        val limit = Settings.get(MetadataLimit)

        return scraper.searchTracks(query, limit)
    }

    override suspend fun searchArtist(query: String): List<ArtistMetadata>? {

        val limit = Settings.get(MetadataLimit)
        val extended = Settings.get(ExtendedMetadata)

        return scraper.searchArtists(query, limit, extended)
    }

    override suspend fun parse(data: PreparseData): TrackInfoMetadata? {
        return searchTrack(data.audioFile.file.nameWithoutExtension)?.firstOrNull()
    }

    override suspend fun searchAlbum(query: String): List<AlbumMetadata>? {
        val limit = Settings.get(MetadataLimit)

        return scraper.searchAlbums(query, limit)
    }
}