package de.mickkc.vibin.tasks.impl

import de.mickkc.vibin.processing.VolumeDetector
import de.mickkc.vibin.repos.TrackRepo
import de.mickkc.vibin.tasks.BaseTask
import de.mickkc.vibin.tasks.TaskResult
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class CalculateMissingVolumeLevelsTask : BaseTask() {

    override val id: String
        get() = "calculate_missing_volume_levels"

    override val interval: Duration
        get() = 6.hours

    override suspend fun runTask(): TaskResult {

        val tracksWithoutVolumeLevel = TrackRepo.findTracksWithoutVolumeLevel()
        if (tracksWithoutVolumeLevel.isEmpty()) {
            return TaskResult.success("No tracks without volume level found.")
        }

        tracksWithoutVolumeLevel.forEach { track ->
            try {
                VolumeDetector.detectVolumeLevel(track)
            }
            catch (e: Exception) {
                logger.error("Failed to detect volume level for track ID ${track.id.value}: ${e.message}")
            }
        }

        return TaskResult.success("Processed ${tracksWithoutVolumeLevel.size} tracks without volume levels.")
    }
}