package de.mickkc.vibin.tasks.impl

import de.mickkc.vibin.processing.AudioFileProcessor
import de.mickkc.vibin.settings.Settings
import de.mickkc.vibin.settings.server.AddGenreAsTag
import de.mickkc.vibin.settings.server.IsFirstScan
import de.mickkc.vibin.tasks.BaseTask
import de.mickkc.vibin.tasks.TaskResult

object ScanForNewTracksTask : BaseTask() {

    override val id: String
        get() = "scan_for_new_tracks"

    override val runOnStartup: Boolean
        get() = true

    override suspend fun runTask(): TaskResult {

        if (Settings.get(IsFirstScan)) {
            Settings.set(IsFirstScan, false)

            logger.info("Skipping scan because the user may want to change settings before the first scan.")
            return TaskResult.success("Skipped scan because it's the first scan. Configure the parsing settings and run again.")
        }

        val allFiles = AudioFileProcessor.getMusicFilesInMusicDir()
        val addTags = Settings.get(AddGenreAsTag)

        var succeeded = 0
        var failed = 0

        allFiles.withIndex().forEach { (index, file) ->
            reportProgress("Scanning: ${file.name}", index + 1, allFiles.size)
            try {
                AudioFileProcessor.processSingleFile(file, addTags)
                succeeded++
            } catch (e: Exception) {
                logger.error("Failed to process file ${file.absolutePath}: ${e.message}", e)
                failed++
            }
        }

        logger.info("Found and processed $succeeded new tracks, $failed failures.")

        return TaskResult.success("Processed $succeeded new tracks, $failed failures.")
    }
}