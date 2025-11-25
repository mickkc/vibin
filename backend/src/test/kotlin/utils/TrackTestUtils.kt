package utils

import de.mickkc.vibin.db.images.ImageEntity
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction
import de.mickkc.vibin.db.tracks.TrackEntity
import de.mickkc.vibin.repos.UserRepo

object TrackTestUtils {
    suspend fun createTrack(
        title: String,
        album: String,
        artists: String,
        trackNumber: Int? = null,
        trackCount: Int? = null,
        discNumber: Int? = null,
        discCount: Int? = null,
        duration: Long? = null,
        bitrate: Int? = null,
        channels: Int? = null,
        sampleRate: Int? = null,
        path: String? = null,
        year: Int? = null,
        comment: String? = null,
        uploaderId: Long? = null,
        checksum: String? = null,
        tags: List<String>? = null,
        coverChecksum: String? = null,
    ): TrackEntity {

        val splitArtists = artists.split(",").map { it.trim() }
        val artists = splitArtists.map {
            ArtistTestUtils.getOrCreateArtist(it)
        }.distinctBy { it.id.value }
        val album = AlbumTestUtils.getOrCreateAlbum(album)
        val tags = tags?.map {
            TagTestUtils.getOrCreateTag(it)
        }?.distinctBy { it.id.value } ?: emptyList()

        return transaction {
            TrackEntity.new {
                this.title = title
                this.cover = null
                this.trackNumber = trackNumber
                this.trackCount = trackCount
                this.discNumber = discNumber
                this.discCount = discCount
                this.duration = duration
                this.bitrate = bitrate
                this.channels = channels
                this.sampleRate = sampleRate
                this.path = path ?: "/music/${title.replace(" ", "_")}.mp3"
                this.year = year
                this.comment = comment ?: ""
                this.uploader = uploaderId?.let { UserRepo.getById(uploaderId)!! }
                this.album = album
                this.artists = SizedCollection(artists)
                this.checksum = checksum ?: "checksum_${title.hashCode()}"
                this.tags = SizedCollection(tags)
                this.cover = coverChecksum?.let {
                    ImageEntity.new {
                        this.sourceChecksum = it
                        this.sourcePath = "/images/covers/$it.jpg"
                    }
                }
            }
        }
    }
}