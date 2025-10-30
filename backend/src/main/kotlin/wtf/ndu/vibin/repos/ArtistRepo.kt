package wtf.ndu.vibin.repos

import io.ktor.server.plugins.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Case
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.intLiteral
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import wtf.ndu.vibin.db.artists.ArtistEntity
import wtf.ndu.vibin.db.artists.ArtistTable
import wtf.ndu.vibin.db.artists.TrackArtistConnection
import wtf.ndu.vibin.db.images.ImageEntity
import wtf.ndu.vibin.dto.ArtistDto
import wtf.ndu.vibin.dto.artists.ArtistEditData
import wtf.ndu.vibin.parsing.Parser
import wtf.ndu.vibin.processing.ThumbnailProcessor
import wtf.ndu.vibin.routes.PaginatedSearchParams
import wtf.ndu.vibin.utils.DateTimeUtils

object ArtistRepo {

    fun count(): Long = transaction {
        return@transaction ArtistEntity.all().count()
    }

    fun getById(id: Long): ArtistEntity? = transaction {
        return@transaction ArtistEntity.findById(id)
    }

    fun getImage(artist: ArtistEntity): ImageEntity? = transaction {
        return@transaction artist.image
    }

    /**
     * Retrieves an existing artist by name or creates a new one if it doesn't exist.
     *
     * @param name The name of the artist to retrieve or create.
     * @return The existing or newly created ArtistEntity.
     */
    fun getOrCreateArtist(name: String): ArtistEntity = transaction {
        return@transaction ArtistEntity.find { ArtistTable.name.lowerCase() eq name.lowercase() }.firstOrNull()
            ?: ArtistEntity.new {
                this.name = name
                this.image = null
            }
    }

    fun updateOrCreateArtist(id: Long?, data: ArtistEditData): ArtistEntity = transaction {
        val artist = if (id == null) {
            if (data.name == null) {
                throw IllegalStateException("name")
            }
            ArtistEntity.new {
                this.name = data.name
                this.description = data.description ?: ""
                this.image = null
            }
        } else {
            ArtistEntity.findByIdAndUpdate(id) { a ->
                data.name?.takeIf { it.isNotEmpty() }?.let {  a.name = it; }
                data.description?.let { a.description = it }
                a.updatedAt = DateTimeUtils.now()
            }
        }

        if (artist == null) {
            throw NotFoundException("Artist with id $id not found")
        }

        if (data.imageUrl != null && data.imageUrl.isNotEmpty()) {
            val data = runBlocking { Parser.downloadCoverImage(data.imageUrl) }
            val image = data?.let { ThumbnailProcessor.getImage(data) }
            artist.image = image
        }

        return@transaction artist
    }

    fun autocomplete(query: String, limit: Int = 10): List<String> = transaction {
        return@transaction ArtistTable.select(ArtistTable.name)
            .where { ArtistTable.name.lowerCase() like "%${query.lowercase()}%" }
            .orderBy(
                (Case()
                    .When(ArtistTable.name.lowerCase() like "${query.lowercase()}%", intLiteral(1))
                    .Else(intLiteral(0))) to SortOrder.DESC,
                ArtistTable.name to SortOrder.ASC
            )
            .limit(limit)
            .map { it[ArtistTable.name] }
    }

    fun deleteArtist(artistId: Long): Boolean = transaction {
        val artist = ArtistEntity.findById(artistId) ?: return@transaction false
        artist.delete()
        return@transaction true
    }

    fun getUnusedArtists(): Pair<SizedIterable<ArtistEntity>, Long> = transaction {
        val unusedArtists = ArtistEntity.find {
            ArtistTable.id notInSubQuery (TrackArtistConnection.select(TrackArtistConnection.artist))
        }
        val count = unusedArtists.count()
        return@transaction unusedArtists to count
    }

    fun deleteAll(artists: SizedIterable<ArtistEntity>) = transaction {
        artists.forEach { it.delete() }
    }

    fun getAll(params: PaginatedSearchParams): Pair<List<ArtistEntity>, Long> = transaction {
        val artists = ArtistEntity.find { ArtistTable.name.lowerCase() like "%${params.query.lowercase()}%" }
        val count = artists.count()
        val results = artists
            .orderBy(ArtistTable.name to SortOrder.ASC)
            .limit(params.pageSize)
            .offset(params.offset)
            .toList()
        return@transaction results to count
    }

    fun idsToDisplayString(ids: List<Long>): String = transaction {
        val names = ArtistEntity.find { ArtistTable.id inList ids }
            .orderBy(ArtistTable.name to SortOrder.ASC)
            .map { it.name }
        return@transaction names.joinToString(", ")
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
            description = artistEntity.description,
            image = artistEntity.image?.let { ImageRepo.toDto(it) },
            createdAt = artistEntity.createdAt,
            updatedAt = artistEntity.updatedAt
        )
    }
}