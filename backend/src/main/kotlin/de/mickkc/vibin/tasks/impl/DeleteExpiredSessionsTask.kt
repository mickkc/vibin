package de.mickkc.vibin.tasks.impl

import de.mickkc.vibin.repos.SessionRepo
import de.mickkc.vibin.tasks.BaseTask
import de.mickkc.vibin.tasks.TaskResult
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

object DeleteExpiredSessionsTask : BaseTask() {

    override val id: String
        get() = "delete_expired_sessions"

    override val interval: Duration?
        get() = 1.days

    override val runOnStartup: Boolean
        get() = false

    override suspend fun runTask(): TaskResult {
        val deletedSessions = SessionRepo.removeAllExpired()
        return TaskResult.success("Deleted $deletedSessions expired sessions.")
    }
}