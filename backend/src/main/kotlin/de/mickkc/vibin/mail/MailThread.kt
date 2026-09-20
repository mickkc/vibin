package de.mickkc.vibin.mail

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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

    const val MAX_MAIL_ATTEMPTS = 5

    const val PRIORITY_2FA_CODES = 100
    const val PRIORITY_LOGIN_NOTIFICATIONS = 10
    const val PRIORITY_NORMAL = 0

    suspend fun scheduleMail(to: String, subject: String, body: String, priority: Int = PRIORITY_NORMAL) {
        mutex.withLock {
            scheduledList.add(ScheduledMail(priority, to, subject, body))
        }
        startJobIfNotRunning()
    }

    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    suspend fun startJobIfNotRunning() {
        if (job == null || job?.isActive == true) return
        job = withContext(newSingleThreadContext("MailSender")) {
            launch {
                jobFunction()
            }
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