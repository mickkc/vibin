package de.mickkc.vibin.mail

import de.mickkc.vibin.config.EnvUtil
import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import org.openqa.selenium.InvalidArgumentException
import org.slf4j.LoggerFactory
import java.util.*

object MailSender {

    private val logger = LoggerFactory.getLogger(MailSender::class.java)

    val configured: Boolean
        get() = fromAddress != null && session != null

    var fromAddress: String? = null

    var session: Session? = null

    fun init() {

        val props = Properties()

        val host = EnvUtil.get(EnvUtil.SMTP_HOST)
        if (host == null) {
            logger.info("No SMTP Host provided, no mails will be sent.")
            return
        }

        fromAddress = EnvUtil.get(EnvUtil.SMTP_FROM)
        if (fromAddress == null) {
            logger.error("No sender email address provided.")
            throw InvalidArgumentException("No sender email address provided.")
        }

        val auth = EnvUtil.getBooleanOrError(EnvUtil.SMTP_AUTH, EnvUtil.DEFAULT_SMTP_AUTH)
        val useStartTls = EnvUtil.getBooleanOrError(EnvUtil.SMTP_STARTTLS, EnvUtil.DEFAULT_SMTP_STARTTLS)
        val useTlsSsl = EnvUtil.getBooleanOrError(EnvUtil.SMTP_SSL, EnvUtil.DEFAULT_SMTP_SSL)

        val portStr = EnvUtil.get(EnvUtil.SMTP_PORT)
        val port: Int

        when (portStr) {
            null -> port = if (useStartTls) 587 else if (useTlsSsl) 465 else 25
            else -> {
                val parsedPort = portStr.toIntOrNull()
                if (parsedPort == null || parsedPort < 1 || parsedPort > 65535) {
                    logger.error("Invalid SMTP Port '$portStr', expected an integer between 1 and 65535.")
                    throw InvalidArgumentException("Invalid SMTP Port '$portStr'.")
                }
                port = parsedPort
            }
        }

        if (useStartTls && useTlsSsl) {
            logger.warn("Both STARTTLS and TLS/SSL configured, STARTTLS will be used.")
        }

        if (useStartTls) {
            val tlsREquired = EnvUtil.getBooleanOrError(EnvUtil.SMTP_STARTTLS_REQUIRED, EnvUtil.DEFAULT_SMTP_STARTTLS_REQUIRED)

            props.apply {
                put("mail.smtp.host", host)
                put("mail.smtp.port", port.toString())
                put("mail.smtp.auth", auth.toString())
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.starttls.required", tlsREquired.toString())
            }
        }
        else if (useTlsSsl) {
            props.apply {
                put("mail.smtp.host", host)
                put("mail.smtp.port", port.toString())
                put("mail.smtp.auth", auth.toString())
                put("mail.smtp.ssl.enable", "true")
            }
        }
        else {
            logger.warn("Using unsecure SMTP (no tls or ssl configured)!")
            props.apply {
                put("mail.smtp.host", host)
                put("mail.smtp.port", port.toString())
                put("mail.smtp.auth", auth.toString())

            }
        }
        if (auth) {

            val username = EnvUtil.get(EnvUtil.SMTP_USERNAME)
            if (username == null) {
                logger.error("No SMTP_USERNAME defined, but SMTP_AUTH is set to true!")
                throw InvalidArgumentException("No SMTP_USERNAME defined.")
            }

            val password = EnvUtil.get(EnvUtil.SMTP_PASSWORD)
            if (password == null) {
                logger.error("No SMTP_PASSWORD defined, but SMTP_AUTH is set to true!")
                throw InvalidArgumentException("No SMTP_PASSWORD defined.")
            }

            session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(username, password)
                }
            })
        }
        else {
            session = Session.getInstance(props)
        }
    }

    fun sendMail(to: String, subject: String, body: String) {

        if (!configured) {
            logger.warn("SMTP is not configured, email with subject '$subject' can't be sent.")
            return
        }

        try {
            val message = MimeMessage(session)
            message.setFrom(fromAddress)
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
            message.subject = subject

            message.setContent(
                MimeMultipart().apply {
                    addBodyPart(MimeBodyPart().apply {
                        setContent(body, "text/html; charset=utf-8")
                    })
                }
            )

            Transport.send(message)
        }
        catch (e: Exception) {
            logger.error("Error sending mail with subject '$subject': ${e.localizedMessage}", e)
        }
    }
}