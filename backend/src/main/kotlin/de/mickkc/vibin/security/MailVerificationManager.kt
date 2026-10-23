package de.mickkc.vibin.security

import de.mickkc.vibin.auth.CryptoUtil
import de.mickkc.vibin.config.EnvUtil
import de.mickkc.vibin.config.EnvUtil.EXTERNAL_URL
import de.mickkc.vibin.db.UserEntity
import de.mickkc.vibin.mail.MailLayout
import de.mickkc.vibin.mail.MailSender
import de.mickkc.vibin.mail.MailThread
import de.mickkc.vibin.mail.VerifyMailContent
import de.mickkc.vibin.repos.SecurityRepo
import de.mickkc.vibin.templates.TemplateTranslator
import de.mickkc.vibin.utils.RateLimiter
import io.ktor.server.html.*
import kotlinx.html.html
import kotlinx.html.stream.appendHTML
import org.slf4j.LoggerFactory

object MailVerificationManager {

    private val logger = LoggerFactory.getLogger(MailVerificationManager::class.java)

    enum class MailVerificationResult {
        AlreadyVerified,
        InvalidEmail,
        NotConfigured,
        RateLimited,
        Sent
    }

    val rateLimiter = RateLimiter(2, 30)

    suspend fun sendVerificationMail(user: UserEntity, clientId: String? = null): MailVerificationResult {
        if (clientId != null && !rateLimiter.checkAndIncrement(clientId)) {
            return MailVerificationResult.RateLimited
        }

        if (user.emailVerified) {
            return MailVerificationResult.AlreadyVerified
        }

        val mailAddress = user.email
        if (mailAddress.isNullOrBlank()) {
            logger.warn("Can't send verification email because user ${user.username} has no Email address configured.")
            return MailVerificationResult.InvalidEmail
        }

        if (!MailSender.configured) {
            logger.warn("Can't send verification email because SMTP is not configured.")
            return MailVerificationResult.NotConfigured
        }

        val externalUrl = EnvUtil.getOrNull(EXTERNAL_URL)?.trim()?.removeSuffix("/")
        if (externalUrl == null) {
            logger.error("Can't send verification email because the external URL is not set. Please set the ${EXTERNAL_URL.name} environment variable.")
            return MailVerificationResult.NotConfigured
        }

        val verificationToken = CryptoUtil.createToken(64)

        MailThread.scheduleMail(
            to = mailAddress,
            subject = TemplateTranslator.getTranslation("mails.verify.title"),
            priority = MailThread.PRIORITY_NORMAL,
            body = buildString {
                appendHTML().html {
                    insert(MailLayout("mails.verify.title")) {
                        headerTextKey = "mails.verify.title"
                        primaryButton = MailLayout.MailLayoutButton(
                            titleKey = "mails.verify.verify",
                            url = "$externalUrl/api/mail/verify?tk=$verificationToken"
                        )
                        content {
                            insert(VerifyMailContent()) {}
                        }
                    }
                }
            }
        )

        SecurityRepo.sentVerificationEmail(user, mailAddress, verificationToken)
        logger.info("Sent verification Email to user ${user.username}.")
        return MailVerificationResult.Sent
    }
}