package wtf.ndu.vibin.repos

import org.jetbrains.exposed.sql.transactions.transaction
import wtf.ndu.vibin.db.images.ImageEntity
import wtf.ndu.vibin.db.images.ImageTable
import wtf.ndu.vibin.dto.ImageDto
import wtf.ndu.vibin.utils.ImageUtils

object ImageRepo {

    fun createImage(smallUrl: String, mediumUrl: String?, largeUrl: String?, checksum: String, colorScheme: ImageUtils.ColorScheme? = null): ImageEntity = transaction {
        val colorSchemeEntity = colorScheme?.let {
            ColorSchemeRepo.createColorSchemeInternal(
                primary = ImageUtils.getHexFromColor(it.primary),
                light = ImageUtils.getHexFromColor(it.light),
                dark = ImageUtils.getHexFromColor(it.dark)
            )
        }
        ImageEntity.new {
            this.sourceChecksum = checksum
            this.smallPath = smallUrl
            this.mediumPath = mediumUrl
            this.largePath = largeUrl
            this.colorScheme = colorSchemeEntity
        }
    }

    fun getBySourceChecksum(checksum: String): ImageEntity? = transaction {
        ImageEntity.find { ImageTable.sourceChecksum eq checksum }.firstOrNull()
    }

    /**
     * Converts an ImageEntity to an ImageDto.
     * Loads all lazy fields within a transaction.
     *
     * @param entity The ImageEntity to convert.
     * @return The corresponding ImageDto.
     */
    fun toDto(entity: ImageEntity): ImageDto = transaction {
        ImageDto(
            smallUrl = entity.smallPath,
            mediumUrl = entity.mediumPath,
            largeUrl = entity.largePath,
            colorScheme = entity.colorScheme?.let { ColorSchemeRepo.toDto(it) }
        )
    }
}