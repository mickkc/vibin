package de.mickkc.vibin.tasks.impl

import de.mickkc.vibin.repos.SecurityRepo
import de.mickkc.vibin.tasks.BaseTask
import de.mickkc.vibin.tasks.TaskResult
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

object DeleteExpiredEmailsTask : BaseTask() {

    override val id: String
        get() = "delete_expired_emails"

    override val interval: Duration?
        get() = 6.hours

    override suspend fun runTask(): TaskResult {
        val deletedVerificationMail = SecurityRepo.deleteExpiredVerificationEmails()
        val deletedTotpMails = SecurityRepo.deleteExpiredTotpMails()

        return TaskResult.success("Deleted $deletedVerificationMail expired verification mails and $deletedTotpMails expired TOTP mails.")
    }
}