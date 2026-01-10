package de.mickkc.vibin.tasks.impl

import de.mickkc.vibin.repos.TrackRepo
import de.mickkc.vibin.tasks.BaseTask
import de.mickkc.vibin.tasks.TaskResult
import de.mickkc.vibin.utils.PathUtils

object RemoveDeletedTracksTask : BaseTask() {
    override val id: String
        get() = "remove_deleted_tracks"

    override val runOnStartup: Boolean
        get() = true

    override suspend fun runTask(): TaskResult {

        val tracks = TrackRepo.getTrackIdsWithPath().withIndex()
        val idsToDelete = mutableListOf<Long>()

        for ((index, trackEntry) in tracks) {
            if (isInterrupted()) {
                logger.info("Task interrupted. Stopping at index $index.")
                break
            }

            val file = PathUtils.getTrackFileFromPath(trackEntry.second)
            if (!file.exists()) {
                idsToDelete.add(trackEntry.first)
                logger.info("Removing track with ID ${trackEntry.first} as its file does not exist.")
            }
        }

        if (idsToDelete.isNotEmpty()) {
            TrackRepo.deleteTracksByIds(idsToDelete)
            runAfterCompletion(DeleteUnusedAlbumsTask)
            runAfterCompletion(DeleteUnusedArtistsTask)
        }
        else {
            return TaskResult.success("No tracks with missing files found.")
        }

        return TaskResult.success("Removed ${idsToDelete.size} tracks with missing files.")
    }
}