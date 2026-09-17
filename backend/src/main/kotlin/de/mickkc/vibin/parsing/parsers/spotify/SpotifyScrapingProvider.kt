package de.mickkc.vibin.parsing.parsers.spotify

import de.mickkc.vibin.db.UserEntity
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

    override suspend fun searchTrack(query: String, user: UserEntity?): List<TrackInfoMetadata>? {

        val limit = Settings.get(MetadataLimit)

        return scraper.searchTracks(query, limit)
    }

    override suspend fun searchArtist(query: String, user: UserEntity?): List<ArtistMetadata>? {

        val limit = Settings.get(MetadataLimit)
        val extended = Settings.get(ExtendedMetadata)

        return scraper.searchArtists(query, limit, extended)
    }

    override suspend fun parse(data: PreparseData, user: UserEntity?): TrackInfoMetadata? {
        return searchTrack(data.audioFile.file.nameWithoutExtension, user)?.firstOrNull()
    }

    override suspend fun searchAlbum(query: String, user: UserEntity?): List<AlbumMetadata>? {
        val limit = Settings.get(MetadataLimit)

        return scraper.searchAlbums(query, limit)
    }
}