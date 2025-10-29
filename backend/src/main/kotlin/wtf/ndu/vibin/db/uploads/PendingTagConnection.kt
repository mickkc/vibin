package wtf.ndu.vibin.db.uploads

import org.jetbrains.exposed.sql.Table
import wtf.ndu.vibin.db.tags.TagTable

object PendingTagConnection : Table("pending_tag_connection") {
    val uploadId = reference("upload_id", PendingUploadTable)
    val tagId = reference("tag_id", TagTable.id)

    override val primaryKey = PrimaryKey(uploadId, tagId)

}