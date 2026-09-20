package de.mickkc.vibin.mail

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import de.mickkc.vibin.mail.MailThread.jobFunction
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.milliseconds

data class ScheduledMail(
    val priority: Int,
    val to: String,
    val subject: String,
    val body: String
)

object MailThread {

    private val sendDelay = 200.milliseconds
    private val scheduledList = mutableListOf<ScheduledMail>()
    private val mutex = Mutex()
    private val logger = LoggerFactory.getLogger(MailThread::class.java)
    private var job: Job? = null
    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    private val mailDispatcher = newSingleThreadContext("MailThread")
    @OptIn(ExperimentalCoroutinesApi::class)
    private val scope = CoroutineScope(SupervisorJob() + mailDispatcher)

    const val MAX_MAIL_ATTEMPTS = 5

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

    suspend fun jobFunction() {
        logger.info("Starting mail sender...")
        var sentMails = 0
        var failedMails = 0

        while (scheduledList.isNotEmpty()) {
            mutex.withLock {
                val mail = scheduledList.maxBy { it.priority }
                var attempts = 0
                while (attempts < MAX_MAIL_ATTEMPTS) {
                    try {
                        MailSender.sendMail(mail.to, mail.subject, mail.body)
                        sentMails++
                        break
                    }
                    catch (e: Exception) {
                        attempts++
                        logger.error("Could not send mail with subject '${mail.subject}' (Attempt $attempts/$MAX_MAIL_ATTEMPTS)", e)
                    }
                }
                if (attempts == MAX_MAIL_ATTEMPTS) {
                    logger.warn("$MAX_MAIL_ATTEMPTS attempts reached while trying to send mail with subject '${mail.subject}'!")
                    failedMails++
                }
                scheduledList.remove(mail)
            }
            delay(sendDelay)
        }
        logger.info("No mails left to send. Sent $sentMails mails, $failedMails failures.")
    }
}