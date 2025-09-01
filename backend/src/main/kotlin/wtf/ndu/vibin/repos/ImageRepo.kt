package wtf.ndu.vibin.repos

import org.jetbrains.exposed.sql.transactions.transaction
import wtf.ndu.vibin.db.ImageEntity
import wtf.ndu.vibin.dto.ImageDto
import wtf.ndu.vibin.utils.ImageUtils

object ImageRepo {

    fun createImage(originalUrl: String, smallUrl: String, largeUrl: String?, colorScheme: ImageUtils.ColorScheme? = null): ImageEntity = transaction {
        val colorSchemeEntity = colorScheme?.let {
            ColorSchemeRepo.createColorSchemeInternal(
                primary = ImageUtils.getHexFromColor(it.primary),
                light = ImageUtils.getHexFromColor(it.light),
                dark = ImageUtils.getHexFromColor(it.dark)
            )
        }
        ImageEntity.new {
            this.originalPath = originalUrl
            this.smallPath = smallUrl
            this.largePath = largeUrl
            this.colorScheme = colorSchemeEntity
        }
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
            originalUrl = entity.originalPath,
            smallUrl = entity.smallPath,
            largeUrl = entity.largePath,
            colorScheme = entity.colorScheme?.let { ColorSchemeRepo.toDto(it) }
        )
    }
}