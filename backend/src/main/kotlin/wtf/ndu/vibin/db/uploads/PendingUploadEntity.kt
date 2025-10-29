package wtf.ndu.vibin.db.uploads

import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import wtf.ndu.vibin.db.ModifiableLongIdEntity
import wtf.ndu.vibin.db.ModifiableLongIdTable
import wtf.ndu.vibin.db.UserEntity
import wtf.ndu.vibin.db.UserTable
import wtf.ndu.vibin.db.tags.TagEntity
import wtf.ndu.vibin.utils.PathUtils

object PendingUploadTable : ModifiableLongIdTable("pending_upload") {
    val filePath = varchar("file_path", 1024).uniqueIndex()
    val title = varchar("title", 512)
    val album = varchar("album", 512)
    val explicit = bool("explicit").default(false)
    val trackNumber = integer("track_number").nullable()
    val trackCount = integer("track_count").nullable()
    val discNumber = integer("disc_number").nullable()
    val discCount = integer("disc_count").nullable()
    val year = integer("year").nullable()
    val comment = text("comment").default("")
    val lyrics = text("lyrics").nullable()
    val coverUrl = text("cover_url").nullable()
    val uploaderId = reference("uploader_id", UserTable)
}

class PendingUploadEntity(id: EntityID<Long>) : ModifiableLongIdEntity(id, PendingUploadTable) {
    companion object : LongEntityClass<PendingUploadEntity>(PendingUploadTable)

    var filePath by PendingUploadTable.filePath
    var title by PendingUploadTable.title
    val artists by PendingArtistEntity referrersOn PendingArtistTable.uploadId
    var album by PendingUploadTable.album
    var explicit by PendingUploadTable.explicit
    var trackNumber by PendingUploadTable.trackNumber
    var trackCount by PendingUploadTable.trackCount
    var discNumber by PendingUploadTable.discNumber
    var discCount by PendingUploadTable.discCount
    var year by PendingUploadTable.year
    var comment by PendingUploadTable.comment
    var tags by TagEntity via PendingTagConnection
    var lyrics by PendingUploadTable.lyrics
    var coverUrl by PendingUploadTable.coverUrl
    var uploader by UserEntity referencedOn PendingUploadTable.uploaderId

    override fun delete() {

        val uploadId = this.id.value

        val file = PathUtils.getUploadFileFromPath(filePath)
        if (file.exists()) {
            file.delete()
        }

        PendingArtistEntity.find { PendingArtistTable.uploadId eq uploadId }.forEach { it.delete() }
        PendingTagConnection.deleteWhere { PendingTagConnection.uploadId eq uploadId }

        super.delete()
    }
}