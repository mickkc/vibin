package de.mickkc.vibin.db.security

import de.mickkc.vibin.db.SessionEntity
import de.mickkc.vibin.db.SessionTable
import de.mickkc.vibin.utils.DateTimeUtils
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

object TotpMailTable : LongIdTable("totp_mail") {
    val sessionId = reference("session_id", SessionTable).uniqueIndex()
    val code = varchar("code", 6)
    val sent = long("sent").clientDefault { DateTimeUtils.now() }
}

class TotpMailEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<TotpMailEntity>(TotpMailTable)

    var session by SessionEntity referencedOn TotpMailTable.sessionId
    var code by TotpMailTable.code
    var sent by TotpMailTable.sent
}