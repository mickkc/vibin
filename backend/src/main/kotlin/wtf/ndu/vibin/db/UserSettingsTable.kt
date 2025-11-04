package wtf.ndu.vibin.db

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object UserSettingsTable : LongIdTable("user_setting") {
    val key = varchar("key", 255).index()
    val userId = long("user_id").index()
    val value = text("value")
}

/**
 * Entity class representing a key-value setting in the system.
 *
 * @property key The unique key identifying the [wtf.ndu.vibin.settings.Setting].
 * @property value The value associated with the setting.
 */
class UserSettingsEntity(id: EntityID<Long>) : LongEntity(id) {

    companion object : LongEntityClass<UserSettingsEntity>(UserSettingsTable)

    var key by UserSettingsTable.key
    var userId by UserSettingsTable.userId
    var value by UserSettingsTable.value
}