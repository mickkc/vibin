package de.mickkc.vibin.watcher

import de.mickkc.vibin.config.EnvUtil
import de.mickkc.vibin.processing.AudioFileProcessor
import de.mickkc.vibin.repos.TrackRepo
import de.mickkc.vibin.settings.Settings
import de.mickkc.vibin.settings.server.AddGenreAsTag
import de.mickkc.vibin.utils.PathUtils
import io.methvin.watcher.DirectoryChangeEvent
import io.methvin.watcher.DirectoryWatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object DirectoryWatcher {

    private val logger = LoggerFactory.getLogger(de.mickkc.vibin.watcher.DirectoryWatcher::class.java)
    var watcher: DirectoryWatcher? = null

    private val fsEvents = Channel<DirectoryChangeEvent>(capacity = Channel.UNLIMITED)

    private const val DEBOUNCE_DELAY_MS = 500L


    fun configure() {
        val musicDir = EnvUtil.getOrNull(EnvUtil.MUSIC_DIR)
        if (musicDir == null) {
            logger.warn("MUSIC_DIR is not set. Directory watcher will not be started.")
            return
        }

        if (!musicDir.exists()) {
            logger.warn("MUSIC_DIR path '$musicDir' does not exist. Directory watcher will not be started.")
            return
        }

        watcher = DirectoryWatcher.builder()
            .path(musicDir.toPath())
            .fileHashing(false)
            .listener { event ->
                fsEvents.trySend(event)
            }
            .build()
    }

    suspend fun start() {
        if (watcher == null) {
            logger.warn("Directory watcher is not configured. Call configure() before start().")
            return
        }
        startEventProcessor()
    }

    fun stop() {
        watcher?.close()
    }

    suspend fun startEventProcessor() = withContext(Dispatchers.IO) {

        val buffer = mutableListOf<DirectoryChangeEvent>()

        while (true) {
            buffer.clear()

            // Block until the first event is received
            val firstEvent = fsEvents.receive()
            buffer.add(firstEvent)

            delay(DEBOUNCE_DELAY_MS) // wait for more events

            // Drain the channel for any additional events
            while (true) {
                val event = fsEvents.tryReceive().getOrNull() ?: break
                buffer.add(event)
            }

            processEvents(buffer)
        }
    }

    suspend fun processEvents(events: List<DirectoryChangeEvent>) {
        val created = events.filter { it.eventType() == DirectoryChangeEvent.EventType.CREATE }
        val modified = events.filter { it.eventType() == DirectoryChangeEvent.EventType.MODIFY }
        val deleted = events.filter { it.eventType() == DirectoryChangeEvent.EventType.DELETE }

        val genresAsTag = Settings.get(AddGenreAsTag)

        created.forEach {
            val file = it.path().toFile()

            if (!AudioFileProcessor.isSupportedAudioFile(file)) {
                logger.info("Created file is not a supported audio file: $file")
                return@forEach
            }

            if (!file.exists()) {
                logger.warn("Created file does not exist: $file")
                return@forEach
            }

            val isStable = waitForStableFile(file)
            if (!isStable) {
                logger.warn("File is not stable after waiting: $file")
                return@forEach
            }

            logger.info("File created: $file")

            AudioFileProcessor.processSingleFile(file, genresAsTag)
        }

        modified.forEach {
            val file = it.path().toFile()
            logger.info("File modified: ${file.absolutePath}. Since audio files are usually not edited, no action is taken.")
        }

        deleted.forEach {
            val file = it.path().toFile()
            if (file.exists()) {
                logger.warn("File deleted event received, but file still exists: $file ")
                return@forEach
            }

            logger.info("File deleted: $file")

            val relativePath = PathUtils.getTrackPathFromFile(file)
            val track = TrackRepo.getByPath(relativePath)

            if (track != null) {
                TrackRepo.delete(track)
                logger.info("Deleted track from database: ${track.path}")
            } else {
                logger.warn("No track found in database for deleted file: $relativePath")
            }
        }
    }

    /**
     * Waits until the specified file is stable (i.e., finished copying) for a given duration.
     *
     * @param file The file to monitor
     * @param stableDuration The duration the file size must remain unchanged to be considered stable
     * @param timeout The maximum time to wait for the file to become stable
     * @return True if the file became stable within the timeout, false otherwise
     */
    suspend fun waitForStableFile(
        file: File,
        stableDuration: Duration = 1.seconds,
        timeout: Duration = 30.seconds
    ): Boolean {
        val deadline = System.currentTimeMillis() + timeout.inWholeMilliseconds
        var lastSize = -1L
        var stableSince = 0L

        while (System.currentTimeMillis() < deadline) {
            if (!file.exists()) return false

            val size = try {
                file.length()
            } catch (e: IOException) {
                delay(100)
                continue
            }

            if (size == lastSize) {
                if (stableSince == 0L) {
                    stableSince = System.currentTimeMillis()
                } else if (
                    System.currentTimeMillis() - stableSince >= stableDuration.inWholeMilliseconds
                ) {
                    return true
                }
            } else {
                stableSince = 0L
                lastSize = size
            }

            delay(200)
        }

        return false
    }

}