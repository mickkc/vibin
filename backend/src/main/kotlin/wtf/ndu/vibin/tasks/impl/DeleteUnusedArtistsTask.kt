package wtf.ndu.vibin.tasks.impl

import wtf.ndu.vibin.repos.ArtistRepo
import wtf.ndu.vibin.tasks.BaseTask
import wtf.ndu.vibin.tasks.TaskResult
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class DeleteUnusedArtistsTask : BaseTask() {

    override val id: String
        get() = "delete_unused_artists"

    override val interval: Duration
        get() = 24.hours

    override suspend fun runTask(): TaskResult {

        val (artists, count) = ArtistRepo.getUnusedArtists()

        logger.info("Found $count unused artists to delete.")

        if (count == 0L) {
            return TaskResult.success("No unused artists to delete.")
        }

        ArtistRepo.deleteAll(artists)

        logger.info("Deleted $count unused artists.")

        return TaskResult.success("Deleted $count unused artists.")
    }
}