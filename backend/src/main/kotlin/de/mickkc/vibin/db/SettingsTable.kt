package de.mickkc.vibin.db

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

object SettingsTable : LongIdTable("setting") {
    val key = varchar("key", 255).uniqueIndex()
    val value = text("value")
}

/**
 * Entity class representing a key-value setting in the system.
 *
 * @property key The unique key identifying the [de.mickkc.vibin.settings.Setting].
 * @property value The value associated with the setting.
 */
class SettingsEntity(id: EntityID<Long>) : LongEntity(id) {

    companion object : LongEntityClass<SettingsEntity>(SettingsTable)

    var key by SettingsTable.key
    var value by SettingsTable.value
}