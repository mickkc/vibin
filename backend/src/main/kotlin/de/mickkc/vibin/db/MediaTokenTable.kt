package de.mickkc.vibin.db

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object MediaTokenTable : LongIdTable() {
    val user = reference("user_id", UserTable)
    val token = varchar("token", 255).uniqueIndex()
    val deviceId = varchar("device_id", 64).nullable()
    val createdAt = long("created_at")
}

class MediaTokenEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<MediaTokenEntity>(MediaTokenTable)

    var user by UserEntity referencedOn MediaTokenTable.user
    var token by MediaTokenTable.token
    var deviceId by MediaTokenTable.deviceId
    var createdAt by MediaTokenTable.createdAt
}