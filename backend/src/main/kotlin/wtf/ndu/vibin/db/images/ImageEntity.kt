package wtf.ndu.vibin.db.images

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import wtf.ndu.vibin.images.ImageCache
import wtf.ndu.vibin.utils.PathUtils

object ImageTable : LongIdTable("image") {
    val sourceChecksum = varchar("sourceChecksum", 32)
    val sourcePath = varchar("source_path", 1024)
    val colorScheme = reference("color_scheme_id", ColorSchemeTable).nullable().default(null)
}

/**
 * Image entity representing image URLs in various sizes.
 *
 * @property sourceChecksum The checksum of the source image.
 * @property sourcePath The file path of the source image.
 * @property colorScheme The associated color scheme for the image. (optional)
 */
class ImageEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ImageEntity>(ImageTable)

    var sourceChecksum by ImageTable.sourceChecksum
    var sourcePath by ImageTable.sourcePath
    var colorScheme by ColorSchemeEntity optionalReferencedOn ImageTable.colorScheme

    override fun delete() {

        PathUtils.getThumbnailFileFromPath(sourcePath).delete()
        ImageCache.evictCacheForImageId(id.value)

        super.delete()
    }
}