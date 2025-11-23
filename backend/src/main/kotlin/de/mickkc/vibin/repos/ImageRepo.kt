package de.mickkc.vibin.repos

import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.transactions.transaction
import de.mickkc.vibin.db.UserTable
import de.mickkc.vibin.db.albums.AlbumTable
import de.mickkc.vibin.db.artists.ArtistTable
import de.mickkc.vibin.db.images.ImageEntity
import de.mickkc.vibin.db.images.ImageTable
import de.mickkc.vibin.db.playlists.PlaylistTable
import de.mickkc.vibin.db.tracks.TrackTable
import de.mickkc.vibin.parsing.Parser
import de.mickkc.vibin.processing.ThumbnailProcessor
import de.mickkc.vibin.utils.ImageUtils

object ImageRepo {

    fun createImage(sourcePath: String, checksum: String, colorScheme: ImageUtils.ColorScheme? = null): ImageEntity = transaction {
        val colorSchemeEntity = colorScheme?.let {
            ColorSchemeRepo.createColorSchemeInternal(
                primary = ImageUtils.getHexFromColor(it.primary),
                light = ImageUtils.getHexFromColor(it.light),
                dark = ImageUtils.getHexFromColor(it.dark)
            )
        }
        ImageEntity.new {
            this.sourceChecksum = checksum
            this.sourcePath = sourcePath
            this.colorScheme = colorSchemeEntity
        }
    }

    fun getBySourceChecksum(checksum: String): ImageEntity? = transaction {
        ImageEntity.find { ImageTable.sourceChecksum eq checksum }.firstOrNull()
    }

    /**
     * Finds all images that are not referenced by any other entity.
     *
     * @return A list of unused ImageEntity instances.
     */
    fun getUnusedImages(): Pair<SizedIterable<ImageEntity>, Long> = transaction {
        val usedImageIds = mutableSetOf<Long>()

        // Collect image IDs from various entities that reference images
        usedImageIds.addAll(
            UserTable.select(UserTable.profilePictureId)
                .mapNotNull { it[UserTable.profilePictureId]?.value }
        )
        usedImageIds.addAll(
            TrackTable.select(TrackTable.coverId)
                .mapNotNull { it[TrackTable.coverId]?.value }
        )
        usedImageIds.addAll(
            AlbumTable.select(AlbumTable.cover)
                .mapNotNull { it[AlbumTable.cover]?.value }
        )
        usedImageIds.addAll(
            PlaylistTable.select(PlaylistTable.cover)
                .mapNotNull { it[PlaylistTable.cover]?.value }
        )
        usedImageIds.addAll(
            ArtistTable.select(ArtistTable.image)
                .mapNotNull { it[ArtistTable.image]?.value }
        )

        if (usedImageIds.isEmpty()) {
            return@transaction ImageEntity.all() to ImageEntity.count()
        }

        val unusedImages = ImageEntity.find { ImageTable.id notInList usedImageIds }
        return@transaction unusedImages to unusedImages.count()
    }

    /**
     * Retrieves an edited image based on the provided URL or returns a default value.
     *
     * @param imageUrl The URL of the image to retrieve.
     * @return A Pair where the first element indicates if an edit was made,
     *         and the second element is the ImageEntity or null.
     */
    suspend fun getUpdatedImage(imageUrl: String?): Pair<Boolean, ImageEntity?> {
        return if (imageUrl != null) {
            if (imageUrl.isNotEmpty()) {
                val imageData = Parser.downloadCoverImage(imageUrl)
                true to imageData?.let { ThumbnailProcessor.getImage(it) }
            }
            else {
                true to null
            }
        } else {
            false to null
        }
    }

    /**
     * Deletes all provided ImageEntity instances within a single transaction.
     *
     * @param entities The list of ImageEntity instances to delete.
     */
    fun deleteAll(entities: SizedIterable<ImageEntity>) = transaction {
        entities.forEach { it.delete() }
    }
}