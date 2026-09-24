package de.mickkc.vibin.db

import de.mickkc.vibin.mail.MailThread.PRIORITY_NORMAL
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object PendingMailTable : LongIdTable("pending_mail") {
    val priority = integer("priority").default(PRIORITY_NORMAL)
    val to = varchar("to", 255)
    val subject = varchar("subject", 255)
    val body = text("body")
}

class PendingMailEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<PendingMailEntity>(PendingMailTable)

    var priority by PendingMailTable.priority
    var to by PendingMailTable.to
    var subject by PendingMailTable.subject
    var body by PendingMailTable.body
}