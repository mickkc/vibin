package de.mickkc.vibin.tasks.impl

import de.mickkc.vibin.repos.SecurityRepo
import de.mickkc.vibin.settings.Settings
import de.mickkc.vibin.settings.server.LoginNotificationTokenValidity
import de.mickkc.vibin.tasks.BaseTask
import de.mickkc.vibin.tasks.TaskResult
import de.mickkc.vibin.utils.DateTimeUtils
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

object RemoveOutdatedLoginMessagesTask : BaseTask() {

    override val id: String
        get() = "remove_outdated_login_messages_task"

    override val interval: Duration?
        get() = 3.hours

    override suspend fun runTask(): TaskResult {

        val now = DateTimeUtils.now()
        val duration = Settings.get(LoginNotificationTokenValidity)

        val removedNotifications = SecurityRepo.removeLoginNotificationsBefore(now - duration)

        return TaskResult.success(
            "Removed $removedNotifications outdated login notifications."
        )
    }
}