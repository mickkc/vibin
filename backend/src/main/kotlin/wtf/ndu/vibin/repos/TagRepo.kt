package wtf.ndu.vibin.repos

import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import wtf.ndu.vibin.db.tags.TagEntity
import wtf.ndu.vibin.db.tags.TagTable
import wtf.ndu.vibin.dto.TagDto

object TagRepo {

    fun getById(id: Long): TagEntity? = transaction {
        return@transaction TagEntity.findById(id)
    }

    fun getOrCreateTag(name: String): TagEntity = transaction {
        TagEntity.find { TagTable.name.lowerCase() eq name.lowercase() }.firstOrNull() ?: TagEntity.new {
            this.name = name
        }
    }

    fun toDto(tagEntity: TagEntity): TagDto = transaction {
        return@transaction toDtoInternal(tagEntity)
    }

    fun toDto(tagEntities: List<TagEntity>): List<TagDto> = transaction {
        return@transaction tagEntities.map { toDtoInternal(it) }
    }

    private fun toDtoInternal(tagEntity: TagEntity): TagDto {
        return TagDto(
            id = tagEntity.id.value,
            name = tagEntity.name,
            color = tagEntity.color,
            createdAt = tagEntity.createdAt,
            updatedAt = tagEntity.updatedAt
        )
    }
}