package wtf.ndu.vibin.db

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import wtf.ndu.vibin.utils.DateTimeUtils

/**
 * A long ID table that includes createdAt and updatedAt fields.
 * @property createdAt The timestamp when the record was created.
 * @property updatedAt The timestamp when the record was last updated, nullable.
 */
open class ModifiableLongIdTable(name: String = "") : LongIdTable(name) {
    val createdAt = long("created_at").clientDefault { DateTimeUtils.now() }
    val updatedAt = long("updated_at").nullable().default(null)
}


/**
 * A long ID entity that includes createdAt and updatedAt fields.
 * @param table The table associated with this entity.
 * @property createdAt The timestamp when the record was created.
 * @property updatedAt The timestamp when the record was last updated, nullable.
 */
open class ModifiableLongIdEntity(id: EntityID<Long>, val table: ModifiableLongIdTable) : LongEntity(id) {
    var createdAt by table.createdAt
    var updatedAt by table.updatedAt
}