package de.mickkc.vibin.security

import de.mickkc.vibin.auth.CryptoUtil
import de.mickkc.vibin.db.SessionEntity
import de.mickkc.vibin.db.UserEntity
import de.mickkc.vibin.dto.users.UserEditDto
import de.mickkc.vibin.mail.MailSender
import de.mickkc.vibin.repos.SecurityRepo
import de.mickkc.vibin.repos.UserRepo
import de.mickkc.vibin.security.exception.LoginNotificationAlreadyHandledException
import de.mickkc.vibin.settings.Settings
import de.mickkc.vibin.settings.user.SendMailOnNewLogin
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object LoginNotificationManager {

    val logger: Logger = LoggerFactory.getLogger(LoginNotificationManager::class.java)

    /**
     * Sends a login notification to the specified user, if SMTP-support is enabled, and the user has an email address
     * associated with the account and enabled this feature.
     *
     * @param deviceId The device ID. A mail will only be sent if it is not already trusted.
     * @param session The session the Email should be sent for.
     * @param user The user to send the Email to, if they have an Email address.
     */
    fun sendIfNotTrusted(deviceId: String, session: SessionEntity, user: UserEntity) {
        if (SecurityRepo.isDeviceIdTrusted(deviceId)) {
            return
        }

        if (!Settings.get(SendMailOnNewLogin, user.id.value)) {
            return
        }

        if (user.email.isNullOrBlank()) {
            logger.warn("Can't send login notification email because user ${user.username} has no Email address configured.")
            return
        }

        if (!MailSender.configured) {
            logger.warn("Can't send login notification email because SMTP is not configured.")
            return
        }

        val invalidationToken = CryptoUtil.createToken(64)

        // TODO: Actually send mail

        SecurityRepo.sentLoginNotificationEmail(deviceId, invalidationToken, session)
        logger.info("Sent login notification Email for user ${user.username}.")
    }

    /**
     * Called when the user clicked on the "Don't notify again" button.
     *
     * Marks the device ID as trusted, and invalidates all other tokens for that device ID.
     *
     * @param token The token (part of the URL on the button)
     */
    fun trustDeviceIdFromLoginEmail(token: String) {
        val notification = SecurityRepo.getLoginNotificationFromToken(token)
                ?: throw LoginNotificationAlreadyHandledException()

        SecurityRepo.trustDeviceId(notification.deviceId)
        SecurityRepo.deleteLoginNotificationsWithDeviceId(notification.deviceId)
    }

    /**
     * Called when a user clicked on the "Change password" button.
     *
     * Invalidates all sessions started with the affected device ID and changes the password to the one the user has
     * provided after clicking the button.
     *
     * @param token The token (part of the URL on the button)
     * @param newPassword The new password provided by the user
     */
    suspend fun changePasswordFromLoginEmail(token: String, newPassword: String) {
        val notification = SecurityRepo.getLoginNotificationFromToken(token)
            ?: throw LoginNotificationAlreadyHandledException()

        val user = SecurityRepo.getUserFromNotification(notification)
        SecurityRepo.invalidateSessionFromNotification(notification)

        UserRepo.updateOrCreateUser(user.id.value, UserEditDto(
            password = newPassword
        ))
    }
}