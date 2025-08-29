package wtf.ndu.vibin.db

import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
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
    val coverId = reference("cover_id", ImageTable).nullable()
    val path = varchar("path", 1024).uniqueIndex()
    val checksum = text("checksum").uniqueIndex()
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
    var cover by ImageEntity optionalReferencedOn TrackTable.coverId
    var path by TrackTable.path
    var checksum by TrackTable.checksum
    var artists by ArtistEntity via TrackArtistConnection
    var tags by TagEntity via TrackTagConnection

    override fun delete() {
        PathUtils.getTrackFileFromPath(path).delete()
        super.delete()
    }
}