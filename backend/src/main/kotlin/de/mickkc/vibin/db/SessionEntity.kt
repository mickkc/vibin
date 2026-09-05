package de.mickkc.vibin.db

import de.mickkc.vibin.db.security.LoginNotificationEntity
import de.mickkc.vibin.db.security.LoginNotificationTable
import de.mickkc.vibin.db.security.TotpMailTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import de.mickkc.vibin.utils.DateTimeUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere

enum class SessionState {
    Valid,
    TotpPending
}

object SessionTable : LongIdTable("session") {
    val userId = reference("user_id", UserTable)
    val token = varchar("token", 64).uniqueIndex()
    var deviceIdhash = binary("device_id_hash", 256).nullable()
    var deviceName = varchar("device_name", 255)
    val state = enumeration<SessionState>("state").default(SessionState.Valid)
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
    var deviceIdHash by SessionTable.deviceIdhash
    var deviceName by SessionTable.deviceName
    var state by SessionTable.state
    var createdAt by SessionTable.createdAt
    var lastUsed by SessionTable.lastUsed

    override fun delete() {

        val sessionId = id

        LoginNotificationEntity.find { LoginNotificationTable.sessionId eq sessionId }
            .forEach { it.delete() }

        TotpMailTable.deleteWhere { TotpMailTable.sessionId eq sessionId }

        super.delete()
    }
}