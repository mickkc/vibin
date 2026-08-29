package de.mickkc.vibin.repos

import de.mickkc.vibin.auth.CryptoUtil
import de.mickkc.vibin.db.SessionEntity
import de.mickkc.vibin.db.SessionTable
import de.mickkc.vibin.db.UserEntity
import de.mickkc.vibin.db.security.LoginNotificationEntity
import de.mickkc.vibin.db.security.LoginNotificationTable
import de.mickkc.vibin.db.security.TrustedDeviceIdTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.transactions.transaction

object SecurityRepo {

    /**
     * Checks if a device ID is already marked as trusted for the specified user.
     *
     * @param deviceId The device ID to check
     * @return true if the device ID is in the list of trusted IDs, false if not
     */
    fun isDeviceIdTrusted(deviceId: String, deviceName: String, user: UserEntity): Boolean = transaction {
        val hashed = CryptoUtil.hashPassword(deviceId, user.salt)
        TrustedDeviceIdTable.select(TrustedDeviceIdTable.id)
            .where { (TrustedDeviceIdTable.deviceIdHash eq hashed) and (TrustedDeviceIdTable.userId eq user.id.value) and (TrustedDeviceIdTable.deviceName eq deviceName) }
            .any()
    }

    /**
     * Adds a device ID to the list of trusted Device IDs.
     *
     * Trusted device IDs will not cause a notification to be sent when logging in with them.
     *
     * @param hashedDeviceId The hashed device ID (wit the user's salt) to trust
     * @param deviceNameStr The name of the device
     * @param user The user to trust this device ID for
     */
    fun trustDeviceId(hashedDeviceId: ByteArray, deviceNameStr: String, user: UserEntity) = transaction {
        TrustedDeviceIdTable.insertIgnore {
            it[deviceIdHash] = hashedDeviceId
            it[userId] = user.id.value
            it[deviceName] = deviceNameStr
        }
    }

    /**
     * Creates a new login notification entity and stores it in the database.
     *
     * Called after an Email has been sent to a user.
     *
     * @param deviceId The device ID used to log in
     * @param token The token generated for this notification (not the session!)
     * @param session The session created by the login (used to invalidate later)
     * @param user The user the mail has been sent to
     */
    fun sentLoginNotificationEmail(deviceId: String, deviceName: String, token: String, session: SessionEntity, user: UserEntity): LoginNotificationEntity = transaction {

        val hashedDeviceId = CryptoUtil.hashPassword(deviceId, user.salt)
        val hashedToken = CryptoUtil.hashPassword(token, user.salt)

        LoginNotificationEntity.new {
            this.tokenHash = hashedToken
            this.session = session
            this.deviceIdHash = hashedDeviceId
            this.deviceName = deviceName
        }
    }

    /**
     * Gets a sent login notification based on its token.
     *
     * @param token The token
     * @return The notification for that token, or null if does not exist.
     */
    fun getLoginNotificationFromToken(token: String): LoginNotificationEntity? = transaction {
        LoginNotificationEntity.all().firstOrNull {
            val hashed = CryptoUtil.hashPassword(token, it.session.user.salt)
            hashed.contentEquals(it.tokenHash)
        }
    }

    /**
     * Removes all login notifications before the specified timestamp.
     *
     * @param timestamp The timestamp in seconds. Notifications created before this timestamp are removed
     * @return The amount of deleted notifications
     */
    fun removeLoginNotificationsBefore(timestamp: Long): Int = transaction {
        LoginNotificationTable.deleteWhere { createdAt less timestamp }
    }

    /**
     * Deletes / Invalidates all messages sent because of logins from the specified device ID.
     *
     * After that, changing the password will not be possible from any Email that was sent with this device ID.
     *
     * @param deviceIdHash The device ID hash to invalidate all messages for
     * @param user The user to invalidate messages for
     */
    fun deleteLoginNotificationsWithDeviceId(deviceIdHash: ByteArray, user: UserEntity) = transaction {
        LoginNotificationEntity
            .find { (LoginNotificationTable.deviceIdHash eq deviceIdHash) and (LoginNotificationTable.sessionId inSubQuery SessionTable
                .select(SessionTable.id)
                .where { SessionTable.userId eq user.id.value })
            }
            .forEach { it.delete() }
    }

    fun deleteLoginNotification(notification: LoginNotificationEntity) = transaction {
        notification.delete()
    }

    /**
     * Gets the user entity that the notification was sent to.
     *
     * @param notificationEntity The message sent to a user.
     * @return The user it was sent to.
     */
    fun getUserFromNotification(notificationEntity: LoginNotificationEntity): UserEntity = transaction {
        notificationEntity.session.user
    }

    /**
     * Invalidates all session on non-trusted devices for a user.
     *
     * @param notificationEntity The message that has been sent to the user via Email.
     */
    fun invalidateSessionsFromNotification(notificationEntity: LoginNotificationEntity) = transaction {
        SessionEntity.find {
            (SessionTable.userId eq notificationEntity.session.user.id) and
            (SessionTable.deviceIdhash notInSubQuery TrustedDeviceIdTable.select(TrustedDeviceIdTable.deviceIdHash).where {
                TrustedDeviceIdTable.userId eq notificationEntity.session.user.id
            })
        }
        .forEach { it.delete() }
    }
}