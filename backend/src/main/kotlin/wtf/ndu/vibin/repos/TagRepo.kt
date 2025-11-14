package wtf.ndu.vibin.repos

import org.jetbrains.exposed.sql.Case
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.intLiteral
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import wtf.ndu.vibin.db.tags.TagEntity
import wtf.ndu.vibin.db.tags.TagTable
import wtf.ndu.vibin.dto.tags.TagDto
import wtf.ndu.vibin.dto.tags.TagEditDto

object TagRepo {

    fun getById(id: Long): TagEntity? = transaction {
        return@transaction TagEntity.findById(id)
    }

    fun getByIds(ids: List<Long>): List<TagEntity> = transaction {
        return@transaction TagEntity.find { TagTable.id inList ids }
            .orderBy(TagTable.name to SortOrder.ASC)
            .toList()
    }

    fun count(): Long = transaction {
        return@transaction TagEntity.all().count()
    }

    fun getAll(query: String = "", limit: Int? = null): List<TagEntity> = transaction {
        val tags = TagEntity.find { TagTable.name.lowerCase() like "%${query.lowercase()}%" }
        val results = tags
            .orderBy(
                (Case()
                    .When(TagTable.name.lowerCase() like "${query.lowercase()}%", intLiteral(1))
                    .Else(intLiteral(0))) to SortOrder.DESC,
                TagTable.name to SortOrder.ASC
            )
            .let { if (limit != null) it.limit(limit) else it }
            .toList()
        return@transaction results
    }

    fun getOrCreateTag(name: String): TagEntity = transaction {
        val existingTag = TagEntity.find { TagTable.name.lowerCase() eq name.lowercase() }.firstOrNull()
        if (existingTag != null) {
            return@transaction existingTag
        }
        return@transaction TagEntity.new {
            this.name = name
        }
    }

    fun autocomplete(query: String, limit: Int = 10): List<String> = transaction {
        TagTable.select(TagTable.name)
            .where { TagTable.name.lowerCase() like "%${query.lowercase()}%" }
            .orderBy(
                (Case()
                    .When(TagTable.name.lowerCase() like "${query.lowercase()}%", intLiteral(1))
                    .Else(intLiteral(0))) to SortOrder.DESC,
                TagTable.name to SortOrder.ASC
            )
            .limit(limit)
            .map { it[TagTable.name] }
    }

    fun create(editDto: TagEditDto): TagEntity = transaction {
        return@transaction TagEntity.new {
            this.name = editDto.name
            this.importance = editDto.importance
            this.description = editDto.description ?: ""
        }
    }

    fun update(tag: TagEntity, editDto: TagEditDto): TagEntity = transaction {
        tag.name = editDto.name
        tag.importance = editDto.importance
        editDto.description?.let { tag.description = it }
        return@transaction tag
    }

    fun delete(tag: TagEntity) = transaction {
        tag.delete()
    }

    fun getByName(name: String): TagEntity? = transaction {
        return@transaction TagEntity.find { TagTable.name.lowerCase() eq name.lowercase() }.firstOrNull()
    }

    fun doesTagExist(name: String): Boolean = transaction {
        return@transaction !TagEntity.find { TagTable.name.lowerCase() eq name.lowercase() }.empty()
    }

    fun toDto(tagEntity: TagEntity): TagDto = transaction {
        return@transaction toDtoInternal(tagEntity)
    }

    fun toDto(tagEntities: List<TagEntity>): List<TagDto> = transaction {
        return@transaction tagEntities.map { toDtoInternal(it) }
    }

    internal fun toDtoInternal(tagEntity: TagEntity): TagDto {
        return TagDto(
            id = tagEntity.id.value,
            name = tagEntity.name,
            description = tagEntity.description,
            importance = tagEntity.importance,
            createdAt = tagEntity.createdAt,
            updatedAt = tagEntity.updatedAt
        )
    }
}