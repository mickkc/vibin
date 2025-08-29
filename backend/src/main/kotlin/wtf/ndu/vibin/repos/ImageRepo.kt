package wtf.ndu.vibin.repos

import org.jetbrains.exposed.sql.transactions.transaction
import wtf.ndu.vibin.db.ImageEntity
import wtf.ndu.vibin.dto.ImageDto

object ImageRepo {

    fun createImage(originalUrl: String, smallUrl: String, largeUrl: String?): ImageEntity = transaction {
        ImageEntity.new {
            this.originalPath = originalUrl
            this.smallPath = smallUrl
            this.largePath = largeUrl
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
            largeUrl = entity.largePath
        )
    }
}