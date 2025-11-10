package wtf.ndu.vibin.repos

import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.transactions.transaction
import wtf.ndu.vibin.db.UserTable
import wtf.ndu.vibin.db.albums.AlbumTable
import wtf.ndu.vibin.db.artists.ArtistTable
import wtf.ndu.vibin.db.images.ImageEntity
import wtf.ndu.vibin.db.images.ImageTable
import wtf.ndu.vibin.db.playlists.PlaylistTable
import wtf.ndu.vibin.db.tracks.TrackTable
import wtf.ndu.vibin.utils.ImageUtils

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
     * Deletes all provided ImageEntity instances within a single transaction.
     *
     * @param entities The list of ImageEntity instances to delete.
     */
    fun deleteAll(entities: SizedIterable<ImageEntity>) = transaction {
        entities.forEach { it.delete() }
    }
}