package wtf.ndu.vibin.repos

import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import wtf.ndu.vibin.db.*
import wtf.ndu.vibin.dto.AlbumDto

object AlbumRepo {

    private val logger = LoggerFactory.getLogger(AlbumRepo::class.java)

    /**
     * Retrieves an existing album by title or creates a new one if it doesn't exist.
     *
     * @param title The title of the album to retrieve or create.
     * @return The existing or newly created AlbumEntity.
     */
    fun getOrCreateAlbum(title: String): AlbumEntity = transaction {
        return@transaction AlbumEntity.find { AlbumTable.title.lowerCase() eq title.lowercase() }.firstOrNull()
            ?: AlbumEntity.new { this.title = title }
    }

    fun toDto(albumEntity: AlbumEntity): AlbumDto = transaction {
        return@transaction toDtoInternal(albumEntity)
    }

    fun toDto(albumEntities: List<AlbumEntity>): List<AlbumDto> = transaction {
        return@transaction albumEntities.map { toDtoInternal(it) }
    }

    private fun toDtoInternal(albumEntity: AlbumEntity): AlbumDto = transaction {
        return@transaction AlbumDto(
            id = albumEntity.id.value,
            title = albumEntity.title,
            cover = albumEntity.cover?.let { ImageRepo.toDto(it) },
            artists = ArtistRepo.toDto(getArtistsForAlbum(albumEntity)),
            songsAmount = getSongAmountForAlbum(albumEntity),
            createdAt = albumEntity.createdAt,
            updatedAt = albumEntity.updatedAt
        )
    }

    private fun getArtistsForAlbum(album: AlbumEntity): List<ArtistEntity> {
        return ArtistEntity.find {
            ArtistTable.id inSubQuery (
                    TrackArtistConnection
                        .select(TrackArtistConnection.artist)
                        .where { TrackArtistConnection.track inSubQuery (
                                TrackTable
                                    .select(TrackTable.id)
                                    .where { TrackTable.albumId eq album.id })
                        })
        }.toList()
    }

    private fun getSongAmountForAlbum(album: AlbumEntity): Long = TrackEntity.find { TrackTable.albumId eq album.id }.count()
}