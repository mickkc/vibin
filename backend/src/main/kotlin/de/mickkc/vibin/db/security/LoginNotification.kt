package de.mickkc.vibin.db.security

import de.mickkc.vibin.db.SessionEntity
import de.mickkc.vibin.db.SessionTable
import de.mickkc.vibin.db.StringEntityClass
import de.mickkc.vibin.db.StringIdTable
import de.mickkc.vibin.utils.DateTimeUtils
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.id.EntityID

object LoginNotificationTable : StringIdTable("login_notification", columnName = "invalidate_token", columnLength = 64) {
    val sessionId = reference("session_id", SessionTable)
    val deviceId = varchar("device_id", 32)
    val createdAt = long("created_at").clientDefault { DateTimeUtils.now() }
}

class LoginNotificationEntity (id: EntityID<String>) : Entity<String>(id) {
    companion object : StringEntityClass<LoginNotificationEntity>(LoginNotificationTable)

    var session by SessionEntity.Companion referencedOn LoginNotificationTable.sessionId
    var deviceId by LoginNotificationTable.deviceId
    var createdAt by LoginNotificationTable.createdAt
}