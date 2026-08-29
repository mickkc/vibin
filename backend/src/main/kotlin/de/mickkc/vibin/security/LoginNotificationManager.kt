package de.mickkc.vibin.security

import com.github.benmanes.caffeine.cache.Caffeine
import de.mickkc.vibin.auth.CryptoUtil
import de.mickkc.vibin.config.EnvUtil
import de.mickkc.vibin.config.EnvUtil.EXTERNAL_URL
import de.mickkc.vibin.db.SessionEntity
import de.mickkc.vibin.db.UserEntity
import de.mickkc.vibin.db.security.LoginNotificationEntity
import de.mickkc.vibin.mail.MailLayout
import de.mickkc.vibin.mail.MailSender
import de.mickkc.vibin.mail.NewLoginMailContent
import de.mickkc.vibin.repos.SecurityRepo
import de.mickkc.vibin.repos.UserRepo
import de.mickkc.vibin.security.exception.LoginNotificationAlreadyHandledException
import de.mickkc.vibin.security.exception.LoginNotificationExpiredException
import de.mickkc.vibin.settings.Settings
import de.mickkc.vibin.settings.server.IncludeIpInLoginNotification
import de.mickkc.vibin.settings.server.IncludeLocationInLoginNotification
import de.mickkc.vibin.settings.server.LoginNotificationTokenValidity
import de.mickkc.vibin.settings.user.SendMailOnNewLogin
import de.mickkc.vibin.templates.TemplateTranslator
import de.mickkc.vibin.utils.DateTimeUtils
import io.ktor.server.html.*
import io.ktor.server.plugins.*
import kotlinx.html.html
import kotlinx.html.stream.appendHTML
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

object LoginNotificationManager {

    val logger: Logger = LoggerFactory.getLogger(LoginNotificationManager::class.java)

    private val notificationLocks = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterAccess(1, TimeUnit.HOURS)
        .build<String, ReentrantLock> { ReentrantLock() }

    private fun <T>withLock(token: String, block: () -> T): T {
        val lock = notificationLocks.get(token)

        lock.lock()
        try {
            return block()
        }
        finally {
            lock.unlock()
        }
    }

    /**
     * Sends a login notification to the specified user, if SMTP-support is enabled, and the user has an email address
     * associated with the account and enabled this feature.
     *
     * @param deviceId The device ID. A mail will only be sent if it is not already trusted.
     * @param session The session the Email should be sent for.
     * @param user The user to send the Email to, if they have an Email address.
     */
    suspend fun sendIfNotTrusted(deviceId: String, deviceName: String, ipFromHeader: String, session: SessionEntity, user: UserEntity) {
        if (SecurityRepo.isDeviceIdTrusted(deviceId, deviceName, user)) {
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

        val externalUrl = EnvUtil.getOrNull(EXTERNAL_URL)?.trim()?.removeSuffix("/")
        if (externalUrl == null) {
            logger.error("Can't send login notification because the external URL is not set. Please set the ${EXTERNAL_URL.name} environment variable.")
            return
        }

        val invalidationToken = CryptoUtil.createToken(64)
        val ip = IpLookupManager.findIp(ipFromHeader)
        val location = if (ip != null && Settings.get(IncludeLocationInLoginNotification)) IpLookupManager.lookupIpAddress(ip) else null

        MailSender.sendMail(
            to = user.email!!,
            subject = TemplateTranslator.getTranslation("mails.new_sign_in.subject"),
            body = buildString {
                appendHTML().html {
                    insert(MailLayout("mails.new_sign_in.subject")) {
                        headerTextKey = "mails.new_sign_in.subject"
                        primaryButton = MailLayout.MailLayoutButton(
                            titleKey = "mails.new_sign_in.buttons.secure_account",
                            url = "$externalUrl/api/mail/reset-password?tk=$invalidationToken"
                        )
                        secondaryButton = MailLayout.MailLayoutButton(
                            titleKey = "mails.new_sign_in.buttons.trust_device",
                            url = "$externalUrl/api/mail/trust-device?tk=$invalidationToken"
                        )
                        content {
                            insert(NewLoginMailContent(
                                deviceName = deviceName,
                                ip = if (Settings.get(IncludeIpInLoginNotification)) ip else null,
                                ipLocation = location
                            )) {}
                        }
                    }
                }
            }
        )

        SecurityRepo.sentLoginNotificationEmail(deviceId, deviceName, invalidationToken, session, user)
        logger.info("Sent login notification Email for user ${user.username}.")
    }

    fun validateNotificationAge(notificationEntity: LoginNotificationEntity): Boolean {
        val duration = Settings.get(LoginNotificationTokenValidity)

        val age = DateTimeUtils.now() - notificationEntity.createdAt
        return age < duration
    }

    /**
     * Called when the user clicked on the "Don't notify again" button.
     *
     * Marks the device ID as trusted, and invalidates all other tokens for that device ID.
     *
     * @param token The token (part of the URL on the button)
     * @throws LoginNotificationAlreadyHandledException The notification has already been handled (trusted device or
     *                                                  changed password)
     * @throws LoginNotificationExpiredException The notification has expired because the token is too old.
     */
    fun trustDeviceIdFromLoginEmail(token: String) = withLock(token) {
        val notification = SecurityRepo.getLoginNotificationFromToken(token)
                ?: throw LoginNotificationAlreadyHandledException()

        if (!validateNotificationAge(notification)) {
            SecurityRepo.deleteLoginNotification(notification)
            throw LoginNotificationExpiredException()
        }

        val user = SecurityRepo.getUserFromNotification(notification)

        SecurityRepo.trustDeviceId(notification.deviceIdHash, notification.deviceName, user)
        SecurityRepo.deleteLoginNotificationsWithDeviceId(notification.deviceIdHash, notification.deviceName, user)
    }

    /**
     * Called when a user clicked on the "Change password" button.
     *
     * Invalidates the affected session and changes the password to the one the user has provided after clicking the
     * button.
     *
     * TODO: Invalidate all sessions from untrusted device IDs after https://codeberg.org/mickkc/vibin/issues/73 was implemented.
     *
     * @param token The token (part of the URL on the button)
     * @param newPassword The new password provided by the user
     * @throws LoginNotificationAlreadyHandledException The notification has already been handled (trusted device or
     *                                                  changed password)
     * @throws LoginNotificationExpiredException The notification has expired because the token is too old.
     */
    fun changePasswordFromLoginEmail(token: String, newPassword: String) = withLock(token) {
        val notification = SecurityRepo.getLoginNotificationFromToken(token)
            ?: throw LoginNotificationAlreadyHandledException()

        if (!validateNotificationAge(notification)) {
            SecurityRepo.deleteLoginNotification(notification)
            throw LoginNotificationExpiredException()
        }

        val user = SecurityRepo.getUserFromNotification(notification)
        if (!UserRepo.updatePassword(user.id.value, newPassword)) {
            throw NotFoundException("Could not reset password for user ${user.username} because they were not found in the database.")
        }

        SecurityRepo.invalidateSessionsFromNotification(notification)
        SecurityRepo.deleteLoginNotificationsWithDeviceId(notification.deviceIdHash, notification.deviceName, user)
    }

    fun isLoginNotificationTokenValid(token: String): Boolean = withLock(token) {
        val notification = SecurityRepo.getLoginNotificationFromToken(token) ?: return@withLock false
        return@withLock validateNotificationAge(notification)
    }
}