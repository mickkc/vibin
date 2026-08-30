package de.mickkc.vibin.db.security

import de.mickkc.vibin.db.SessionEntity
import de.mickkc.vibin.db.SessionTable
import de.mickkc.vibin.utils.DateTimeUtils
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object LoginNotificationTable : LongIdTable("login_notification") {
    val tokenHash = binary("token_hash", 256)
    val sessionId = reference("session_id", SessionTable)
    val deviceIdHash = binary("device_id_hash", 256)
    val deviceName = varchar("device_name", 255)
    val createdAt = long("created_at").clientDefault { DateTimeUtils.now() }
}

class LoginNotificationEntity (id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<LoginNotificationEntity>(LoginNotificationTable)

    var tokenHash by LoginNotificationTable.tokenHash
    var session by SessionEntity.Companion referencedOn LoginNotificationTable.sessionId
    var deviceIdHash by LoginNotificationTable.deviceIdHash
    var deviceName by LoginNotificationTable.deviceName
    var createdAt by LoginNotificationTable.createdAt
}