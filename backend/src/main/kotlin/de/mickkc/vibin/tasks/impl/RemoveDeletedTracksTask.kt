package de.mickkc.vibin.tasks.impl

import de.mickkc.vibin.repos.TrackRepo
import de.mickkc.vibin.tasks.BaseTask
import de.mickkc.vibin.tasks.TaskResult
import de.mickkc.vibin.utils.PathUtils
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class RemoveDeletedTracksTask : BaseTask() {
    override val id: String
        get() = "remove_deleted_tracks"

    override val interval: Duration
        get() = 8.hours

    override suspend fun runTask(): TaskResult {

        val tracks = TrackRepo.getTrackIdsWithPath()
        val idsToDelete = mutableListOf<Long>()

        for ((trackId, path) in tracks) {
            val file = PathUtils.getTrackFileFromPath(path)
            if (!file.exists()) {
                idsToDelete.add(trackId)
                logger.info("Removing track with ID $trackId as its file does not exist.")
            }
        }

        if (idsToDelete.isNotEmpty()) {
            TrackRepo.deleteTracksByIds(idsToDelete)
        }
        else {
            return TaskResult.success("No tracks with missing files found.")
        }

        return TaskResult.success("Removed ${idsToDelete.size} tracks with missing files.")
    }
}