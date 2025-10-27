package wtf.ndu.vibin.tasks.impl

import wtf.ndu.vibin.repos.ImageRepo
import wtf.ndu.vibin.tasks.BaseTask
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class DeleteUnusedImagesTask : BaseTask() {

    override val id: String
        get() = "delete_unused_images"

    override val interval: Duration
        get() = 24.hours

    override suspend fun runTask() {

        val (unusedImages, count) = ImageRepo.getUnusedImages()

        logger.info("Found $count unused images to delete.")

        if (count == 0L) {
            return
        }

        ImageRepo.deleteAll(unusedImages)

        logger.info("Deleted $count unused images.")
    }
}