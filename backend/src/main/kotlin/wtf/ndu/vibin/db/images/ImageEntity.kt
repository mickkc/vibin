package wtf.ndu.vibin.db.images

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import wtf.ndu.vibin.utils.PathUtils

object ImageTable : LongIdTable("image") {
    val sourceChecksum = varchar("sourceChecksum", 32)
    val smallPath = varchar("small_url", 1024)
    val mediumPath = varchar("medium_url", 1024).nullable()
    val largePath = varchar("large_url", 1024).nullable()
    val colorScheme = reference("color_scheme_id", ColorSchemeTable).nullable().default(null)
}

/**
 * Image entity representing image URLs in various sizes.
 *
 * @property sourceChecksum The checksum of the source image.
 * @property smallPath The URL of the small-sized image (128x128).
 * @property mediumPath The URL of the medium-sized image (256x256), nullable.
 * @property largePath The URL of the large-sized image (512x512), nullable.
 */
class ImageEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ImageEntity>(ImageTable)

    var sourceChecksum by ImageTable.sourceChecksum
    var smallPath by ImageTable.smallPath
    var mediumPath by ImageTable.mediumPath
    var largePath by ImageTable.largePath
    var colorScheme by ColorSchemeEntity optionalReferencedOn ImageTable.colorScheme

    override fun delete() {

        PathUtils.getThumbnailFileFromPath(smallPath).delete()
        mediumPath?.let { PathUtils.getThumbnailFileFromPath(it).delete() }
        largePath?.let { PathUtils.getThumbnailFileFromPath(it).delete() }

        super.delete()
    }
}