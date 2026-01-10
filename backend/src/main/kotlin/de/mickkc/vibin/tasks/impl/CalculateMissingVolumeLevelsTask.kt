package de.mickkc.vibin.tasks.impl

import de.mickkc.vibin.processing.VolumeDetector
import de.mickkc.vibin.repos.TrackRepo
import de.mickkc.vibin.tasks.BaseTask
import de.mickkc.vibin.tasks.TaskResult

object CalculateMissingVolumeLevelsTask : BaseTask() {

    override val id: String
        get() = "calculate_missing_volume_levels"

    override suspend fun runTask(): TaskResult {

        val tracksWithoutVolumeLevel = TrackRepo.findTracksWithoutVolumeLevel()
        if (tracksWithoutVolumeLevel.isEmpty()) {
            return TaskResult.success("No tracks without volume level found.")
        }

        tracksWithoutVolumeLevel.withIndex().forEach { (index, track) ->

            if (isInterrupted()) {
                logger.info("Task $id was interrupted. Stopping further processing.")
                return TaskResult.success("Task was interrupted after processing $index out of ${tracksWithoutVolumeLevel.size} tracks.")
            }

            try {
                reportProgress("Processing: ${track.path.substringAfterLast('/')}", index + 1, tracksWithoutVolumeLevel.size)
                VolumeDetector.detectVolumeLevel(track)
            }
            catch (e: Exception) {
                logger.error("Failed to detect volume level for track ID ${track.id.value}: ${e.message}")
                reportProgress("Error: ${e.message}", index + 1, tracksWithoutVolumeLevel.size)
            }
        }

        return TaskResult.success("Processed ${tracksWithoutVolumeLevel.size} tracks without volume levels.")
    }
}