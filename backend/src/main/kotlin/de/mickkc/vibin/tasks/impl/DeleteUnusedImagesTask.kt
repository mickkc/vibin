package de.mickkc.vibin.tasks.impl

import de.mickkc.vibin.repos.ImageRepo
import de.mickkc.vibin.tasks.BaseTask
import de.mickkc.vibin.tasks.TaskResult
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class DeleteUnusedImagesTask : BaseTask() {

    override val id: String
        get() = "delete_unused_images"

    override val interval: Duration
        get() = 24.hours

    override suspend fun runTask(): TaskResult {

        val (unusedImages, count) = ImageRepo.getUnusedImages()

        logger.info("Found $count unused images to delete.")

        if (count == 0L) {
            return TaskResult.success("No unused images to delete.")
        }

        ImageRepo.deleteAll(unusedImages)

        logger.info("Deleted $count unused images.")

        return TaskResult.success("Deleted $count unused images.")
    }
}