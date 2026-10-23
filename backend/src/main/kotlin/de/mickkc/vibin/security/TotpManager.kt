package de.mickkc.vibin.security

import de.mickkc.vibin.db.SessionEntity
import de.mickkc.vibin.db.TotpType
import de.mickkc.vibin.db.UserEntity
import de.mickkc.vibin.mail.MailLayout
import de.mickkc.vibin.mail.MailSender
import de.mickkc.vibin.mail.MailThread
import de.mickkc.vibin.mail.TotpMailContent
import de.mickkc.vibin.repos.SecurityRepo
import de.mickkc.vibin.repos.UserRepo
import de.mickkc.vibin.templates.TemplateTranslator
import dev.samstevens.totp.code.DefaultCodeGenerator
import dev.samstevens.totp.code.DefaultCodeVerifier
import dev.samstevens.totp.code.HashingAlgorithm
import dev.samstevens.totp.qr.QrData
import dev.samstevens.totp.qr.ZxingPngQrGenerator
import dev.samstevens.totp.secret.DefaultSecretGenerator
import dev.samstevens.totp.time.SystemTimeProvider
import io.ktor.server.html.*
import kotlinx.html.html
import kotlinx.html.stream.appendHTML
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object TotpManager {

    const val RESET_TOTP_SECRET = ""

    private val logger: Logger = LoggerFactory.getLogger(TotpManager::class.java)

    val secretGenerator = DefaultSecretGenerator()
    val timeProvider = SystemTimeProvider()
    val codeGenerator = DefaultCodeGenerator()
    val codeVerifier = DefaultCodeVerifier(codeGenerator, timeProvider)
    val qrGenerator = ZxingPngQrGenerator()

    fun generateTotpSecret(user: UserEntity) {
        val secret = secretGenerator.generate()
        UserRepo.setTotp(user, totpSecret = secret)
    }

    /**
     * Returns a [ByteArray] of PNG image data displaying a QR-Code, that can be added by an authenticator app.
     * Sets [UserEntity.totpSecret] if not set already.
     *
     * @throws IllegalStateException Totp is already configured. For security reasons, the code can only be scanned once.
     * @param user The user to create the QR-Code for.
     * @return A [ByteArray] of PNG image data.
     */
    fun getQrCodeForUser(user: UserEntity): ByteArray {
        if (user.totpSecret == null) {
            generateTotpSecret(user)
        }

        if (user.totpType == TotpType.Totp) {
            throw IllegalStateException("Tried scanning a totp code when totp is already configured for user '${user.username}'.")
        }

        val data = QrData.Builder()
            .label(user.username)
            .secret(user.totpSecret)
            .issuer("Vibin'")
            .algorithm(HashingAlgorithm.SHA1)
            .digits(6)
            .period(30)
            .build()

        return qrGenerator.generate(data)
    }

    /**
     * Verifies totp for a user.
     * To make sure the user's authenticator app is correctly configured, the created code must be provided.
     *
     * This sets [UserEntity.totpType] to [TotpType.Totp], which requires providing the totp code on each future login.
     *
     * @param user The user to enable totp for.
     * @param code The code the user entered, used to check that the configuration is valid.
     * @return true if the code was successfully validated and the totp type was updated, false otherwise.
     */
    fun enableTotp(user: UserEntity, code: String): Boolean {
        val valid = validateTotp(user, code)
        if (valid) {
            UserRepo.setTotp(user, totpType = TotpType.Totp)
        }

        return valid
    }

    fun validateTotp(user: UserEntity, code: String): Boolean {
        return user.totpSecret != null && codeVerifier.isValidCode(user.totpSecret, code)
    }

    /**
     * Enables email-based totp for a user.
     * Email configuration and validity checks are performed when a code gets sent.
     *
     * @param user The user to enable email-based totps for.
     */
    fun enableEmailTotp(user: UserEntity) {
        UserRepo.setTotp(user, totpType = TotpType.Mail, totpSecret = RESET_TOTP_SECRET)
    }

    /**
     * Disables totp for a user.
     * Sets [UserEntity.totpType] to [TotpType.None] and resets [UserEntity.totpSecret] to null.
     *
     * @param user The user to disable TOTP for.
     */
    fun disableTotp(user: UserEntity) {
        UserRepo.setTotp(user, totpType = TotpType.None, totpSecret = RESET_TOTP_SECRET)
    }

    enum class TotpEmailResult {
        Disabled,
        InvalidEmail,
        EmailNotVerified,
        NotConfigured,
        Sent
    }

    suspend fun sendTotpEmail(user: UserEntity, session: SessionEntity): TotpEmailResult {
        if (user.totpType != TotpType.Mail) {
            return TotpEmailResult.Disabled
        }

        if (user.email.isNullOrBlank()) {
            logger.warn("Can't send TOTP email because user ${user.username} has no Email address configured.")
            return TotpEmailResult.InvalidEmail
        }

        if (user.emailVerified) {
            logger.warn("Can't send TOTP email because user ${user.username} has not verified their email address.")
            return TotpEmailResult.EmailNotVerified
        }

        if (!MailSender.configured) {
            LoginNotificationManager.logger.warn("Can't send TOTP email because SMTP is not configured.")
            return TotpEmailResult.NotConfigured
        }

        val totp = SecurityRepo.createTotpForMail(session)

        MailThread.scheduleMail(
            to = user.email!!,
            subject = TemplateTranslator.getTranslation("mails.totp.title"),
            priority = MailThread.PRIORITY_2FA_CODES,
            body = buildString {
                appendHTML().html {
                    insert(MailLayout("mails.totp.title")) {
                        headerTextKey = "mails.totp.title"
                        content {
                            insert(TotpMailContent(totp.code)) {}
                        }
                    }
                }
            }
        )

        return TotpEmailResult.Sent
    }
}