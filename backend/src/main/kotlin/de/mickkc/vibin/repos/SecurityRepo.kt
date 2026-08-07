package de.mickkc.vibin.repos

import de.mickkc.vibin.db.SessionEntity
import de.mickkc.vibin.db.UserEntity
import de.mickkc.vibin.db.security.LoginNotificationEntity
import de.mickkc.vibin.db.security.LoginNotificationTable
import de.mickkc.vibin.db.security.TrustedDeviceIdTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.transactions.transaction

object SecurityRepo {

    /**
     * Checks if a device ID is already marked as trusted for the specified user.
     *
     * @param deviceId The device ID to check
     * @return true if the device ID is in the list of trusted IDs, false if not
     */
    fun isDeviceIdTrusted(deviceId: String, userId: Long): Boolean = transaction {
        TrustedDeviceIdTable.select(TrustedDeviceIdTable.id)
            .where { (TrustedDeviceIdTable.deviceId eq deviceId) and (TrustedDeviceIdTable.userId eq userId) }
            .any()
    }

    /**
     * Adds a device ID to the list of trusted Device IDs.
     *
     * Trusted device IDs will not cause a notification to be sent when logging in with them.
     *
     * @param deviceIdS The device ID to trust
     * @param userIdL The user ID to trust this device ID for
     */
    fun trustDeviceId(deviceIdS: String, userIdL: Long) = transaction {
        TrustedDeviceIdTable.insertIgnore {
            it[deviceId] = deviceIdS
            it[userId] = userIdL
        }//.execute(this)
    }

    /**
     * Creates a new login notification entity and stores it in the database.
     *
     * Called after an Email has been sent to a user.
     *
     * @param deviceId The device ID used to log in
     * @param token The token generated for this notification (not the session!)
     * @param session The session created by the login (used to invalidate later)
     */
    fun sentLoginNotificationEmail(deviceId: String, token: String, session: SessionEntity): LoginNotificationEntity = transaction {
        LoginNotificationEntity.new(token) {
            this.session = session
            this.deviceId = deviceId
        }
    }

    /**
     * Gets a sent login notification based on its token.
     *
     * @param token The token
     * @return The notification for that token, or null if does not exist.
     */
    fun getLoginNotificationFromToken(token: String): LoginNotificationEntity? = transaction {
        LoginNotificationEntity.findById(token)
    }

    /**
     * Deletes / Invalidates all messages sent because of logins from the specified device ID.
     *
     * After that, changing the password will not be possible from any Email that was sent with this device ID.
     *
     * @param deviceId The device ID to invalidate all messages for
     */
    fun deleteLoginNotificationsWithDeviceId(deviceId: String) = transaction {
        LoginNotificationEntity
            .find { LoginNotificationTable.deviceId eq deviceId }
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
     * Invalidates the session that the message was sent for.
     *
     * @param notificationEntity The message that has been sent to the user via Email.
     */
    @Deprecated("Invalidate all sessions with non-trusted device IDs after https://codeberg.org/mickkc/vibin/issues/73 was implemented.", level = DeprecationLevel.WARNING)
    fun invalidateSessionFromNotification(notificationEntity: LoginNotificationEntity) = transaction {
        SessionRepo.invalidateSessionById(notificationEntity.session.id.value)
    }
}