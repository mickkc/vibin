package wtf.ndu.vibin.db.tags

import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import wtf.ndu.vibin.db.ModifiableLongIdEntity
import wtf.ndu.vibin.db.ModifiableLongIdTable

object TagTable : ModifiableLongIdTable("tag") {
    val name = varchar("name", 255).index()
    val description = text("description").default("")
    val color = varchar("color", 7).nullable()
}

/**
 * Tag entity representing a tag that can be associated with various entities.
 *
 * @property name The name of the tag.
 * @property description A description of the tag.
 * @property color The color associated with the tag in HEX format. (optional)
 */
class TagEntity(id: EntityID<Long>) : ModifiableLongIdEntity(id, TagTable) {
    companion object : LongEntityClass<TagEntity>(TagTable)

    var name by TagTable.name
    var description by TagTable.description
    var color by TagTable.color
}