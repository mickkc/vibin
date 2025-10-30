package wtf.ndu.vibin.tasks.impl

import wtf.ndu.vibin.repos.AlbumRepo
import wtf.ndu.vibin.tasks.BaseTask
import wtf.ndu.vibin.tasks.TaskResult
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class DeleteUnusedAlbumsTask : BaseTask() {

    override val id: String
        get() = "delete_unused_albums"

    override val interval: Duration
        get() = 24.hours

    override suspend fun runTask(): TaskResult {

        val (albums, count) = AlbumRepo.getUnusedAlbums()

        logger.info("Found $count unused albums to delete.")

        if (count == 0L) {
            return TaskResult.success("No unused albums to delete.")
        }

        AlbumRepo.deleteAll(albums)

        logger.info("Deleted $count unused albums.")

        return TaskResult.success("Deleted $count unused albums.")
    }
}