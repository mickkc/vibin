package de.mickkc.vibin.db

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

object UserSettingsTable : LongIdTable("user_setting") {
    val key = varchar("key", 255).index()
    val userId = long("user_id").index()
    val value = text("value")
}

/**
 * Entity class representing a key-value setting in the system.
 *
 * @property key The unique key identifying the [de.mickkc.vibin.settings.Setting].
 * @property value The value associated with the setting.
 */
class UserSettingsEntity(id: EntityID<Long>) : LongEntity(id) {

    companion object : LongEntityClass<UserSettingsEntity>(UserSettingsTable)

    var key by UserSettingsTable.key
    var userId by UserSettingsTable.userId
    var value by UserSettingsTable.value
}