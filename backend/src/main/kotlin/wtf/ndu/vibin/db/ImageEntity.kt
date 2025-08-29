package wtf.ndu.vibin.db

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import wtf.ndu.vibin.utils.PathUtils

object ImageTable : LongIdTable("image") {
    val originalPath = varchar("original_url", 1024)
    val smallPath = varchar("small_url", 1024)
    val largePath = varchar("large_url", 1024).nullable()
}

/**
 * Image entity representing image URLs in various sizes.
 *
 * @property originalPath The URL of the original image.
 * @property smallPath The URL of the small-sized image (128x128).
 * @property largePath The URL of the large-sized image (500x500), nullable.
 */
class ImageEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ImageEntity>(ImageTable)

    var originalPath by ImageTable.originalPath
    var smallPath by ImageTable.smallPath
    var largePath by ImageTable.largePath

    override fun delete() {

        PathUtils.getThumbnailFileFromPath(originalPath).delete()
        PathUtils.getThumbnailFileFromPath(smallPath).delete()
        largePath?.let { PathUtils.getThumbnailFileFromPath(it).delete() }

        super.delete()
    }
}