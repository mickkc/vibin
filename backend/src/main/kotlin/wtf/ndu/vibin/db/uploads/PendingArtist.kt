package wtf.ndu.vibin.db.uploads

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object PendingArtistTable : LongIdTable("pending_artist") {
    val name = varchar("name", 512)
    val uploadId = reference("upload_id", PendingUploadTable)
}

class PendingArtistEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<PendingArtistEntity>(PendingArtistTable)

    var name by PendingArtistTable.name
    var upload by PendingUploadEntity referencedOn PendingArtistTable.uploadId
}