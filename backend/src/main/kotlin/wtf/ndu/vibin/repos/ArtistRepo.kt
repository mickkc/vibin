package wtf.ndu.vibin.repos

import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import wtf.ndu.vibin.db.artists.ArtistEntity
import wtf.ndu.vibin.db.artists.ArtistTable
import wtf.ndu.vibin.dto.ArtistDto

object ArtistRepo {

    /**
     * Retrieves an existing artist by name or creates a new one if it doesn't exist.
     *
     * @param name The name of the artist to retrieve or create.
     * @return The existing or newly created ArtistEntity.
     */
    fun getOrCreateArtist(name: String): ArtistEntity = transaction {
        return@transaction ArtistEntity.find { ArtistTable.name.lowerCase() eq name.lowercase() }.firstOrNull()
            ?: ArtistEntity.new { this.name = name }
    }

    fun toDto(artistEntity: ArtistEntity): ArtistDto = transaction {
        return@transaction toDtoInternal(artistEntity)
    }

    fun toDto(artistEntities: List<ArtistEntity>): List<ArtistDto> = transaction {
        return@transaction artistEntities.map { toDtoInternal(it) }
    }

    private fun toDtoInternal(artistEntity: ArtistEntity): ArtistDto {
        return ArtistDto(
            id = artistEntity.id.value,
            name = artistEntity.name,
            image = artistEntity.image?.let { ImageRepo.toDto(it) },
            sortName = artistEntity.sortName,
            tags = TagRepo.toDto(artistEntity.tags.toList()),
            createdAt = artistEntity.createdAt,
            updatedAt = artistEntity.updatedAt
        )
    }
}