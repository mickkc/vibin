package wtf.ndu.vibin.repos

import org.jetbrains.exposed.sql.transactions.transaction
import wtf.ndu.vibin.db.SessionEntity
import wtf.ndu.vibin.db.SessionTable
import wtf.ndu.vibin.db.UserEntity

/**
 * Repository for managing [SessionEntity] instances.
 */
object SessionRepo {

    fun count(): Long = transaction {
        SessionEntity.all().count()
    }

    /**
     * Adds a new session for the specified user with the given token.
     *
     * @param user The user to associate with the new session.
     * @param token The token for the new session.
     * @return The newly created [SessionEntity].
     */
    fun addSession(user: UserEntity, token: String): SessionEntity = transaction {
        SessionEntity.new {
            this.user = user
            this.token = token
        }
    }

    fun removeSession(token: String) = transaction {
        SessionEntity.find { SessionTable.token eq token }.forEach { it.delete() }
    }

    fun getUserIdFromToken(token: String): Long? = transaction {
        SessionEntity.find { SessionTable.token eq token }.firstOrNull()?.user?.id?.value
    }

    fun invalidateAllSessionsForUser(userId: Long) = transaction {
        SessionEntity.find { SessionTable.userId eq userId }.forEach { it.delete() }
    }
}