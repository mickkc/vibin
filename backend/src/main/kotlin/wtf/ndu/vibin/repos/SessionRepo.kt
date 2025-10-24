package wtf.ndu.vibin.repos

import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import wtf.ndu.vibin.auth.CryptoUtil
import wtf.ndu.vibin.db.MediaTokenEntity
import wtf.ndu.vibin.db.MediaTokenTable
import wtf.ndu.vibin.db.SessionEntity
import wtf.ndu.vibin.db.SessionTable
import wtf.ndu.vibin.db.UserEntity
import wtf.ndu.vibin.utils.DateTimeUtils

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

    fun invalidateAllMediaTokensForUser(userId: Long) = transaction {
        MediaTokenEntity.find { MediaTokenTable.user eq userId }.forEach { it.delete() }
    }

    fun createMediaToken(user: UserEntity, deviceId: String): MediaTokenEntity = transaction {
        val token = CryptoUtil.createToken()

        MediaTokenEntity.find { (MediaTokenTable.user eq user.id) and (MediaTokenTable.deviceId eq deviceId) }
            .forEach { it.delete() }

        MediaTokenEntity.new {
            this.user = user
            this.deviceId = deviceId
            this.token = token
            this.createdAt = DateTimeUtils.now()
        }
    }

    fun getUserFromMediaToken(token: String): UserEntity? = transaction {
        MediaTokenEntity.find { MediaTokenTable.token eq token }.firstOrNull()?.user
    }

    fun deleteMediaToken(userId: Long, deviceId: String) = transaction {
        MediaTokenEntity.find { (MediaTokenTable.user eq userId) and (MediaTokenTable.deviceId eq deviceId) }
            .forEach { it.delete() }
    }
}