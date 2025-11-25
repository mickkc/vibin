package de.mickkc.vibin.tasks.impl

import de.mickkc.vibin.tasks.BaseTask
import de.mickkc.vibin.tasks.TaskResult
import de.mickkc.vibin.widgets.WidgetImageCache
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class RemoveExpiredRateLimitsTask : BaseTask() {

    override val id: String
        get() = "remove_expired_rate_limits"

    override val interval: Duration
        get() = 3.hours

    override suspend fun runTask(): TaskResult {

        val removed = WidgetImageCache.rateLimiter.cleanupOldEntries()

        if (removed) {
            logger.info("Removed expired rate limit entries.")
            return TaskResult.success("Removed expired rate limit entries.")
        } else {
            return TaskResult.success("No expired rate limit entries to remove.")
        }

    }

}