package de.mickkc.vibin.tasks.impl

import de.mickkc.vibin.processing.AudioFileProcessor
import de.mickkc.vibin.settings.Settings
import de.mickkc.vibin.settings.server.IsFirstScan
import de.mickkc.vibin.tasks.BaseTask
import de.mickkc.vibin.tasks.TaskResult
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class ScanForNewTracksTask : BaseTask() {

    override val id: String
        get() = "scan_for_new_tracks"

    override val interval: Duration
        get() = 3.hours

    override val runOnStartup: Boolean
        get() = true

    override suspend fun runTask(): TaskResult {

        if (Settings.get(IsFirstScan)) {
            Settings.set(IsFirstScan, false)

            logger.info("Skipping scan because the user may want to change settings before the first scan.")
            return TaskResult.success("Skipped scan because it's the first scan. Configure the parsing settings and run again.")
        }

        val newTracks = AudioFileProcessor.reprocessAll()

        logger.info("Found and processed $newTracks new tracks.")

        return TaskResult.success("Processed $newTracks new tracks.")
    }
}