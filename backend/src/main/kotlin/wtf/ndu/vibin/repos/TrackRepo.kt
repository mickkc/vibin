package wtf.ndu.vibin.repos

import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction
import wtf.ndu.vibin.db.AlbumEntity
import wtf.ndu.vibin.db.ArtistEntity
import wtf.ndu.vibin.db.TrackEntity
import wtf.ndu.vibin.db.TrackTable
import wtf.ndu.vibin.parsing.TrackMetadata
import wtf.ndu.vibin.utils.ChecksumUtil
import wtf.ndu.vibin.utils.PathUtils
import java.io.File

object TrackRepo {

    fun count(): Long = transaction {
        return@transaction TrackEntity.count()
    }

    fun getByChecksum(checksum: String): TrackEntity? = transaction {
        return@transaction TrackEntity.find { TrackTable.checksum eq checksum }.firstOrNull()
    }

    fun createTrack(file: File, metadata: TrackMetadata, album: AlbumEntity, artists: List<ArtistEntity>?, checksum: String? = null): TrackEntity = transaction {
        return@transaction TrackEntity.new {
            this.title = metadata.title
            this.trackNumber = metadata.trackNumber
            this.trackCount = metadata.trackCount
            this.discNumber = metadata.discNumber
            this.discCount = metadata.discCount
            this.year = metadata.year
            this.duration = metadata.durationMs
            this.comment = metadata.comment ?: ""
            this.explicit = metadata.explicit ?: false
            this.path = PathUtils.getTrackPathFromFile(file)
            this.checksum = checksum ?: ChecksumUtil.getChecksum(file)

            this.album = album
            this.artists = SizedCollection(artists ?: emptyList())
        }
    }

    fun update(track: TrackEntity, block: TrackEntity.() -> Unit): TrackEntity = transaction {
        val updated = track.apply(block)
        updated.updatedAt = System.currentTimeMillis()
        return@transaction updated
    }
}