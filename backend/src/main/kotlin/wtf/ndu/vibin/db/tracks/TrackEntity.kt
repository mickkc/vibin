package wtf.ndu.vibin.db.tracks

import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import wtf.ndu.vibin.db.ModifiableLongIdEntity
import wtf.ndu.vibin.db.ModifiableLongIdTable
import wtf.ndu.vibin.db.UserEntity
import wtf.ndu.vibin.db.UserTable
import wtf.ndu.vibin.db.albums.AlbumEntity
import wtf.ndu.vibin.db.albums.AlbumTable
import wtf.ndu.vibin.db.artists.ArtistEntity
import wtf.ndu.vibin.db.artists.TrackArtistConnection
import wtf.ndu.vibin.db.images.ImageEntity
import wtf.ndu.vibin.db.images.ImageTable
import wtf.ndu.vibin.db.tags.TagEntity
import wtf.ndu.vibin.db.tags.TrackTagConnection
import wtf.ndu.vibin.repos.LyricsRepo
import wtf.ndu.vibin.repos.PlaylistTrackRepo
import wtf.ndu.vibin.utils.PathUtils

object TrackTable : ModifiableLongIdTable("track") {
    val title = varchar("title", 255).index()
    val albumId = reference("album_id", AlbumTable).index()
    val explicit = bool("explicit").default(false)
    val trackNumber = integer("track_number").nullable()
    val trackCount = integer("track_count").nullable()
    val discNumber = integer("disc_number").nullable()
    val discCount = integer("disc_count").nullable()
    val year = integer("year").nullable()
    val duration = long("duration").nullable()
    val comment = text("comment").default("")
    val bitrate = integer("bitrate").nullable()
    val sampleRate = integer("sample_rate").nullable()
    val channels = integer("channels").nullable()
    val coverId = reference("cover_id", ImageTable).nullable()
    val path = varchar("path", 1024).uniqueIndex()
    val checksum = text("checksum").uniqueIndex()
    val uploader = reference("uploader", UserTable).nullable()
}

/**
 * Track entity representing a musical track.
 *
 * @property title The title of the track.
 * @property album The album to which the track belongs.
 * @property explicit Indicates if the track has explicit content.
 * @property trackNumber The track number within the album. (optional)
 * @property trackCount The total number of tracks in the album. (optional)
 * @property discNumber The disc number if the album is a multi-disc set. (optional)
 * @property discCount The total number of discs in the album. (optional)
 * @property year The release year of the track. (optional)
 * @property duration The duration of the track in milliseconds. (optional)
 * @property comment Any additional comments about the track.
 * @property cover The cover image associated with the track. (optional)
 * @property artists The artists associated with the track.
 * @property tags The tags associated with the track.
 */
class TrackEntity(id: EntityID<Long>) : ModifiableLongIdEntity(id, TrackTable) {
    companion object : LongEntityClass<TrackEntity>(TrackTable)

    var title by TrackTable.title
    var album by AlbumEntity referencedOn TrackTable.albumId
    var explicit by TrackTable.explicit
    var trackNumber by TrackTable.trackNumber
    var trackCount by TrackTable.trackCount
    var discNumber by TrackTable.discNumber
    var discCount by TrackTable.discCount
    var year by TrackTable.year
    var duration by TrackTable.duration
    var comment by TrackTable.comment
    var bitrate by TrackTable.bitrate
    var sampleRate by TrackTable.sampleRate
    var channels by TrackTable.channels
    var cover by ImageEntity optionalReferencedOn TrackTable.coverId
    var path by TrackTable.path
    var checksum by TrackTable.checksum
    var artists by ArtistEntity via TrackArtistConnection
    var tags by TagEntity via TrackTagConnection
    var uploader by UserEntity.Companion optionalReferencedOn TrackTable.uploader

    override fun delete() {

        // Delete the cover image if it exists
        val cover = this.cover
        this.cover = null
        cover?.delete()

        val trackId = this.id.value

        // Remove associations to artists
        TrackArtistConnection.deleteWhere { TrackArtistConnection.track eq trackId }

        // Remove associations to tags
        TrackTagConnection.deleteWhere { TrackTagConnection.track eq trackId }

        // Remove associations to playlists
        PlaylistTrackRepo.deleteTrackFromAllPlaylists(this)

        // Delete the track file from the filesystem
        PathUtils.getTrackFileFromPath(path).delete()

        // Remove associations to lyrics
        LyricsRepo.deleteLyrics(this)

        super.delete()
    }
}