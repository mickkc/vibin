package de.mickkc.vibin.tasks.impl

import de.mickkc.vibin.tasks.BaseTask
import de.mickkc.vibin.tasks.TaskResult
import de.mickkc.vibin.utils.PathUtils
import de.mickkc.vibin.widgets.WidgetImageCache
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class DeleteExpiredWidgetImagesTask : BaseTask() {

    override val id: String
        get() = "remove_expired_widget_images"

    override val interval: Duration
        get() = 3.hours

    override val runOnStartup: Boolean
        get() = true

    override suspend fun runTask(): TaskResult {

        val (deletedImages, freedBytes) = WidgetImageCache.cleanupExpiredCache()
        val formattedBytes = PathUtils.formatFileSize(freedBytes)

        logger.info("Removed $deletedImages expired widget images from cache. Freed $formattedBytes.")

        if (deletedImages == 0) {
            return TaskResult.success("No expired widget images to remove.")
        }

        return TaskResult.success("Removed $deletedImages expired widget images from cache. Freed $formattedBytes.")
    }

}