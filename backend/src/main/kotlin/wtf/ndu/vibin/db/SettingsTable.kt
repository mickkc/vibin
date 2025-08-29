package wtf.ndu.vibin.db

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object SettingsTable : LongIdTable("setting") {
    val key = varchar("key", 255).uniqueIndex()
    val value = text("value")
}

/**
 * Entity class representing a key-value setting in the system.
 *
 * @property key The unique key identifying the [wtf.ndu.vibin.settings.Setting].
 * @property value The value associated with the setting.
 */
class SettingsEntity(id: EntityID<Long>) : LongEntity(id) {

    companion object : LongEntityClass<SettingsEntity>(SettingsTable)

    var key by SettingsTable.key
    var value by SettingsTable.value
}