package de.mickkc.vibin.mail

import de.mickkc.vibin.auth.CryptoUtil
import de.mickkc.vibin.config.EnvUtil
import de.mickkc.vibin.config.EnvUtil.SMTP_DELAY_MS
import de.mickkc.vibin.config.EnvUtil.SMTP_FAILED_MAILS_DIR
import de.mickkc.vibin.config.EnvUtil.SMTP_MOCK
import de.mickkc.vibin.config.EnvUtil.SMTP_RETRIES
import de.mickkc.vibin.config.EnvUtil.SMTP_RETRY_DELAY_MS
import de.mickkc.vibin.db.PendingMailEntity
import de.mickkc.vibin.db.PendingMailTable
import de.mickkc.vibin.mail.MailThread.jobFunction
import de.mickkc.vibin.utils.DateTimeUtils
import jakarta.mail.IllegalWriteException
import jakarta.mail.SendFailedException
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.eclipse.angus.mail.util.MailConnectException
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.net.UnknownHostException
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
object MailThread {

    private val sendDelay = EnvUtil.getOrError(SMTP_DELAY_MS).milliseconds
    private val retryDelay = EnvUtil.getOrError(SMTP_RETRY_DELAY_MS).milliseconds
    private val failedMailsDir = EnvUtil.getOrNull(SMTP_FAILED_MAILS_DIR)
    private val maxRetries = EnvUtil.getOrError(SMTP_RETRIES).coerceAtLeast(1)
    private val mocking = EnvUtil.getOrNull(SMTP_MOCK) ?: false
    private val mutex = Mutex()
    private val logger = LoggerFactory.getLogger(MailThread::class.java)
    private var job: Job? = null
    @OptIn(DelicateCoroutinesApi::class)
    private val mailDispatcher = newSingleThreadContext("MailThread")
    private val scope = CoroutineScope(SupervisorJob() + mailDispatcher)


    const val PRIORITY_2FA_CODES = 100
    const val PRIORITY_LOGIN_NOTIFICATIONS = 10
    const val PRIORITY_NORMAL = 0

    init {
        failedMailsDir?.mkdirs()
        Runtime.getRuntime().addShutdownHook(Thread {
            logger.info("Closing mail dispatcher...")
            mailDispatcher.close()
        })
    }

    /**
     * Adds a mail to the queue in the database
     *
     * @param init The initialization block setting subject, priority, etc.
     * @return The created [PendingMailEntity]
     */
    private fun addPendingMailToDb(init: (PendingMailEntity) -> Unit): PendingMailEntity = transaction {
        PendingMailEntity.new(init)
    }

    /**
     * Checks if there are any pending mails in the database
     *
     * @return True if there are mails that need to be sent, false otherwise
     */
    private fun hasPendingMails(): Boolean = transaction {
        PendingMailEntity.all().any()
    }

    /**
     * Gets the next mail to send, deletes it from the database and returns it.
     * Mails are ordered by priority; the one with the highest priority gets sent first.
     *
     * @return The next mail to sent or null if there is none.
     */
    private fun getNextMail(): PendingMailEntity? = transaction {
        PendingMailEntity
            .all()
            .orderBy(PendingMailTable.priority to SortOrder.DESC)
            .limit(1).
            singleOrNull()
            ?.apply { delete() }
    }

    /**
     * Schedules sending a mail in the background thread. This will add the mail to the queue and start the job,
     * if it is not already running.
     *
     * @param to The recipient's email address
     * @param subject The email's subject
     * @param body The email body as an HTML string
     * @param priority The priority of the mail. Mails with higher priorities are sent first. See: [PRIORITY_2FA_CODES],
     *                 [PRIORITY_LOGIN_NOTIFICATIONS], etc.
     */
    suspend fun scheduleMail(to: String, subject: String, body: String, priority: Int = PRIORITY_NORMAL) {
        if (mocking) return
        if (!MailSender.configured) {
            logger.warn("Not adding mail with subject '$subject' to the queue because SMTP is not configured.")
            return
        }
        mutex.withLock {
            addPendingMailToDb {
                it.priority = priority
                it.to = to
                it.subject = subject
                it.body = body
            }
        }
        startJobIfNotRunning()
    }

    /**
     * Starts the [jobFunction] in the `MailSender` thread context if it is not already running.
     */
    suspend fun startJobIfNotRunning() = mutex.withLock {
        if (job?.isActive == true) return
        job = scope.launch {
            jobFunction()
        }
    }

    private suspend fun jobFunction() {
        logger.info("Starting mail sender...")
        var sentMails = 0
        var failedMails = 0

        while (mutex.withLock { hasPendingMails().also { if (!it) job = null } }) {
            val mail = mutex.withLock { getNextMail() }
            if (mail == null) break

            var attempts = 0
            var failed = false
            while (attempts < maxRetries) {
                try {
                    MailSender.sendMail(mail.to, mail.subject, mail.body)
                    sentMails++
                    break
                }
                catch (e: Exception) {
                    val retry = when (e) {
                        is SendFailedException -> {
                            e.invalidAddresses.isNullOrEmpty()
                        }
                        is MailConnectException -> {
                            when(e.nextException) {
                                is UnknownHostException -> false
                                else -> true // Could be a temporary timeout
                            }
                        }
                        is IllegalWriteException, is IllegalStateException, is UnknownHostException -> false
                        else -> true
                    }

                    if (!retry) {
                        logger.error("Could not send mail with subject '${mail.subject}'. The error is marked as not retryable, so sending this mail will not be retried.", e)
                        failed = true
                        break
                    }

                    attempts++
                    logger.error("Could not send mail with subject '${mail.subject}' (Attempt $attempts/$maxRetries, waiting ${retryDelay * attempts}ms)", e)
                    delay(retryDelay * attempts)
                }
            }
            if (attempts == maxRetries) {
                logger.warn("$maxRetries attempts reached while trying to send mail with subject '${mail.subject}'!")
                failed = true
            }
            if (failed) {
                failedMails++
                writeFailedMailToDisk(mail)
            }
            delay(sendDelay)
        }
        logger.info("No mails left to send. Sent $sentMails mails, $failedMails failures.")
    }

    fun writeFailedMailToDisk(mail: PendingMailEntity) {
        if (failedMailsDir == null) return
        val file = failedMailsDir.resolve("mail-${mail.to.replace('@', '_')}-${DateTimeUtils.now()}-${CryptoUtil.createToken(8)}.html")

        file.writeText(buildString {
            appendLine("<!-- Recipient: ${mail.to} -->")
            appendLine("<!-- Subject: ${mail.subject} -->")
            appendLine()
            appendLine(mail.body)
        })
    }
}