package de.mickkc.vibin.db.tags

import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import de.mickkc.vibin.db.ModifiableLongIdEntity
import de.mickkc.vibin.db.ModifiableLongIdTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere

object TagTable : ModifiableLongIdTable("tag") {
    val name = varchar("name", 255).index()
    val description = text("description").default("")
    val importance = integer("importance").default(0)
}

/**
 * Tag entity representing a tag that can be associated with various entities.
 *
 * @property name The name of the tag.
 * @property description A description of the tag.
 * @property importance An integer between 1 an 10 representing the importance of the tag.
 */
class TagEntity(id: EntityID<Long>) : ModifiableLongIdEntity(id, TagTable) {
    companion object : LongEntityClass<TagEntity>(TagTable)

    var name by TagTable.name
    var description by TagTable.description
    var importance by TagTable.importance

    override fun delete() {

        val tagId = this.id.value

        TrackTagConnection.deleteWhere {
            TrackTagConnection.tag eq tagId
        }

        super.delete()
    }
}