package de.mickkc.vibin.mail

import de.mickkc.vibin.config.EnvUtil
import de.mickkc.vibin.config.EnvUtil.SMTP_DELAY_MS
import de.mickkc.vibin.config.EnvUtil.SMTP_RETRIES
import de.mickkc.vibin.mail.MailThread.jobFunction
import jakarta.mail.IllegalWriteException
import jakarta.mail.SendFailedException
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.eclipse.angus.mail.util.MailConnectException
import org.slf4j.LoggerFactory
import java.net.UnknownHostException
import kotlin.time.Duration.Companion.milliseconds

data class ScheduledMail(
    val priority: Int,
    val to: String,
    val subject: String,
    val body: String
)

// TODO:
//  - Make delay between retries configurable and longer with each attempt
//  - Fix race condition: If scheduleMail is called while the job finished (all mails sent) but is still running, it does
//    does not restart it, so the mail is never sent.
//  - Close the dispatcher (Runtime.getRuntime().addShutdownHook should work)
//  - Persist pending mails in DB so they can get sent after a restart
//  - (maybe?) Save failed mails to files so they are not completely lost
object MailThread {

    private val sendDelay = EnvUtil.getOrError(SMTP_DELAY_MS).milliseconds
    private val maxRetries = EnvUtil.getOrError(SMTP_RETRIES).coerceAtLeast(1)
    private val scheduledList = mutableListOf<ScheduledMail>()
    private val mutex = Mutex()
    private val logger = LoggerFactory.getLogger(MailThread::class.java)
    private var job: Job? = null
    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    private val mailDispatcher = newSingleThreadContext("MailThread")
    @OptIn(ExperimentalCoroutinesApi::class)
    private val scope = CoroutineScope(SupervisorJob() + mailDispatcher)


    const val PRIORITY_2FA_CODES = 100
    const val PRIORITY_LOGIN_NOTIFICATIONS = 10
    const val PRIORITY_NORMAL = 0

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
        if (!MailSender.configured) {
            logger.warn("Not adding mail with subject '$subject' to the queue because SMTP is not configured.")
            return
        }
        mutex.withLock {
            scheduledList.add(ScheduledMail(priority, to, subject, body))
        }
        startJobIfNotRunning()
    }

    /**
     * Starts the [jobFunction] in the `MailSender` thread context if it is not already running.
     */
    fun startJobIfNotRunning() {
        if (job?.isActive == true) return
        job = scope.launch {
            jobFunction()
        }
    }

    private suspend fun jobFunction() {
        logger.info("Starting mail sender...")
        var sentMails = 0
        var failedMails = 0

        while (mutex.withLock { scheduledList.isNotEmpty() }) {
            val mail = mutex.withLock {
                scheduledList.maxByOrNull { it.priority }?.also { scheduledList.remove(it) }
            }
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
                        is MailConnectException -> true // Could be a temporary timeout
                        is IllegalWriteException, is IllegalStateException, is UnknownHostException -> false
                        else -> true
                    }

                    if (!retry) {
                        logger.error("Could not send mail with subject '${mail.subject}'. The error is marked as not retryable, so sending this mail will not be retried.", e)
                        failed = true
                        break
                    }

                    attempts++
                    logger.error("Could not send mail with subject '${mail.subject}' (Attempt $attempts/$maxRetries)", e)
                }
            }
            if (attempts == maxRetries) {
                logger.warn("$maxRetries attempts reached while trying to send mail with subject '${mail.subject}'!")
                failed = true
            }
            if (failed) failedMails++
            delay(sendDelay)
        }
        logger.info("No mails left to send. Sent $sentMails mails, $failedMails failures.")
    }
}