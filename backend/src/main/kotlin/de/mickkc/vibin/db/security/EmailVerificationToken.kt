package de.mickkc.vibin.db.security

import de.mickkc.vibin.db.StringEntityClass
import de.mickkc.vibin.db.StringIdTable
import de.mickkc.vibin.db.UserEntity
import de.mickkc.vibin.db.UserTable
import de.mickkc.vibin.utils.DateTimeUtils
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.id.EntityID

object EmailVerificationTokenTable : StringIdTable("email_verification_token", "token_hash", 64) {
    val userId = reference("user_id", UserTable)
    val emailAddress = varchar("email_address", 255)
    val createdAt = long("created_at").clientDefault { DateTimeUtils.now() }
}

class EmailVerificationTokenEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : StringEntityClass<EmailVerificationTokenEntity>(EmailVerificationTokenTable)

    val tokenHash by EmailVerificationTokenTable.id
    var user by UserEntity referencedOn EmailVerificationTokenTable.userId
    var emailAddress by EmailVerificationTokenTable.emailAddress
    var createdAt by EmailVerificationTokenTable.createdAt
}