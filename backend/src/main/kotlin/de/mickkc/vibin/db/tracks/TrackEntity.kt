package de.mickkc.vibin.db.tracks

import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import de.mickkc.vibin.db.ModifiableLongIdEntity
import de.mickkc.vibin.db.ModifiableLongIdTable
import de.mickkc.vibin.db.UserEntity
import de.mickkc.vibin.db.UserTable
import de.mickkc.vibin.db.albums.AlbumEntity
import de.mickkc.vibin.db.albums.AlbumTable
import de.mickkc.vibin.db.artists.ArtistEntity
import de.mickkc.vibin.db.artists.TrackArtistConnection
import de.mickkc.vibin.db.images.ImageEntity
import de.mickkc.vibin.db.images.ImageTable
import de.mickkc.vibin.db.tags.TagEntity
import de.mickkc.vibin.db.tags.TrackTagConnection
import de.mickkc.vibin.repos.LyricsRepo
import de.mickkc.vibin.repos.PlaylistTrackRepo
import de.mickkc.vibin.repos.TrackRelationRepo
import de.mickkc.vibin.utils.PathUtils

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
    val volumeOffset = double("volume_offset").nullable().default(null)
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
 * @property path The file path of the track.
 * @property checksum The checksum of the track file.
 * @property uploader The user who uploaded the track. (optional)
 * @property volumeOffset The volume offset for playback normalization. (optional)
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
    var volumeOffset by TrackTable.volumeOffset

    override fun delete() {

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

        // Delete track relations
        TrackRelationRepo.deleteAllRelationsForTrack(trackId)

        super.delete()
    }
}