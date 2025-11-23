package de.mickkc.vibin.db

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import de.mickkc.vibin.utils.DateTimeUtils

object SessionTable : LongIdTable("session") {
    val userId = reference("user_id", UserTable)
    val token = varchar("token", 64).uniqueIndex()
    val createdAt = long("created_at").clientDefault { DateTimeUtils.now() }
    val lastUsed = long("last_used").clientDefault { DateTimeUtils.now() }
}

/**
 * Entity class representing a user session in the system.
 *
 * @property user The user associated with this session.
 * @property token The unique token identifying this session.
 */
class SessionEntity(id: EntityID<Long>) : LongEntity(id) {

    companion object : LongEntityClass<SessionEntity>(SessionTable)

    var user by UserEntity referencedOn SessionTable.userId
    var token by SessionTable.token
    var createdAt by SessionTable.createdAt
    var lastUsed by SessionTable.lastUsed
}