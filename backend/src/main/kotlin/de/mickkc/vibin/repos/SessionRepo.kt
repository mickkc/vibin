package de.mickkc.vibin.repos

import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import de.mickkc.vibin.auth.CryptoUtil
import de.mickkc.vibin.db.MediaTokenEntity
import de.mickkc.vibin.db.MediaTokenTable
import de.mickkc.vibin.db.SessionEntity
import de.mickkc.vibin.db.SessionTable
import de.mickkc.vibin.db.UserEntity
import de.mickkc.vibin.dto.SessionDto
import de.mickkc.vibin.settings.Settings
import de.mickkc.vibin.settings.server.SessionExpirationDuration
import de.mickkc.vibin.utils.DateTimeUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.deleteWhere

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
    fun addSession(user: UserEntity, token: String, deviceId: String): SessionEntity = transaction {
        val hashedDeviceId = CryptoUtil.hashPassword(deviceId, user.salt)
        SessionEntity.new {
            this.user = user
            this.token = token
            this.deviceIdHash = hashedDeviceId
        }
    }

    fun removeSession(token: String) = transaction {
        SessionEntity.find { SessionTable.token eq token }.forEach { it.delete() }
    }

    /**
     * Checks if the session is expired and deletes it if it is.
     * The session is expired if it was not used within the configured duration.
     *
     * @see SessionExpirationDuration the configured duration how long a session is valid.
     * @param session The session to check
     * @return The session if it is valid, null if not
     */
    private fun deleteIfExpired(session: SessionEntity): SessionEntity? {
        val expirationDuration = Settings.get(SessionExpirationDuration)
        val start = DateTimeUtils.now() - expirationDuration
        if (session.lastUsed < start) {
            session.delete()
            return null
        }

        return session
    }

    /**
     * Validates the provided session token and returns the associated user ID if valid.
     * Updates the last used timestamp of the session upon successful validation.
     *
     * @param token The session token to validate.
     * @return The user ID associated with the token if valid, or null if invalid.
     */
    fun validateAndUpdateToken(token: String): Long? = transaction {
        SessionEntity
            .find { SessionTable.token eq token }
            .firstOrNull()
            ?.run { deleteIfExpired(this) }
            ?.also { it.lastUsed = DateTimeUtils.now() }
            ?.user?.id?.value
    }

    fun invalidateAllSessionsForUser(userId: Long) = transaction {
        SessionEntity.find { SessionTable.userId eq userId }.forEach { it.delete() }
    }

    fun invalidateAllMediaTokensForUser(userId: Long) = transaction {
        MediaTokenEntity.find { MediaTokenTable.user eq userId }.forEach { it.delete() }
    }

    fun invalidateAllOtherSessionsForUser(userId: Long, excludedToken: String) = transaction {
        SessionEntity.find { (SessionTable.userId eq userId) and (SessionTable.token neq excludedToken) }
            .forEach { it.delete() }
    }

    fun invalidateAllOtherMediaTokensForUser(userId: Long, excludedDeviceId: String) = transaction {
        MediaTokenEntity.find { (MediaTokenTable.user eq userId) and (MediaTokenTable.deviceId neq excludedDeviceId) }
            .forEach { it.delete() }
    }

    fun removeAllExpired(): Int = transaction {
        val expirationDuration = Settings.get(SessionExpirationDuration)
        val start = DateTimeUtils.now() - expirationDuration
        SessionTable.deleteWhere {
            SessionTable.lastUsed less start
        }
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

    fun getAllSessionsForUser(userId: Long): List<SessionEntity> = transaction {
        SessionEntity.find { SessionTable.userId eq userId }.mapNotNull { deleteIfExpired(it) }.toList()
    }

    fun getUserFromSessionId(sessionId: Long): UserEntity? = transaction {
        SessionEntity.findById(sessionId)?.run { deleteIfExpired(this) }?.user
    }

    fun invalidateSessionById(sessionId: Long) = transaction {
        SessionEntity.findById(sessionId)?.delete()
    }

    fun toSessionDto(entity: SessionEntity): SessionDto {
        return toSessionDtoInternal(entity)
    }

    fun toSessionDto(entities: List<SessionEntity>): List<SessionDto> {
        return entities.map { toSessionDtoInternal(it) }
    }

    internal fun toSessionDtoInternal(entity: SessionEntity): SessionDto {
        return SessionDto(
            id = entity.id.value,
            createdAt = entity.createdAt,
            lastUsed = entity.lastUsed
        )
    }
}