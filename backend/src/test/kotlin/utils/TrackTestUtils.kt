package utils

import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction
import wtf.ndu.vibin.db.tracks.TrackEntity
import wtf.ndu.vibin.repos.UserRepo

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
        checksum: String? = null
    ): TrackEntity {

        val splitArtists = artists.split(",").map { it.trim() }
        val artists = splitArtists.map {
            ArtistTestUtils.getOrCreateArtist(it)
        }.distinctBy { it.id.value }
        val album = AlbumTestUtils.getOrCreateAlbum(album)

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
            }
        }
    }
}