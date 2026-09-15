package de.mickkc.vibin.streaming

import de.mickkc.vibin.config.EnvUtil
import de.mickkc.vibin.db.tracks.TrackEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.locks.ReentrantLock

/**
 * Caches fully transcoded copies of tracks on disk, keyed by track checksum, format and bitrate.
 *
 * There are 4 possible scenarios when streaming:
 *
 * 1. Client requests the original file:
 *      - The file can simply be returned
 *      - Ktor handles the range headers
 *      - No transcoding necessary
 *      - Fastest
 * 2. Client requests a cached stream:
 *      - The cache gets returned if it exists
 *      - If not, see below
 * 3. Client requests a non-cached stream from the beginning (`bytes=0-`):
 *      - Track needs to be transcoded
 *      - Since the request starts at the beginning, just like transcoding, and does not have an end, the transcoding
 *          result can simply be sent back to the client, while also caching it for subsequent plays.
 * 4. Client requests a non-cached stream with a specific range:
 *      - ffmpeg can only start transcoding at timestamps
 *      - Timestamps can not be reliably calculated from the byte range
 *      - This means yhe full file must be transcoded first
 *      - By far the slowest
 *      - Shouldn't really be a problem since you usually start listening to a track from the beginning
 *      - Once the track is cached, this is just as fast as the other scenarios
 */
object TranscodeCache {

    private val logger: Logger = LoggerFactory.getLogger(TranscodeCache::class.java)
    private val locks = ConcurrentHashMap<String, ReentrantLock>()

    // Detached from any single request's lifetime so a client disconnecting mid-stream doesn't stop transcoding
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        val cacheDir = getCacheDir()
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    fun getCacheDir(): File = EnvUtil.getOrError(EnvUtil.TRANSCODE_CACHE_DIR)

    private fun extensionFor(format: String) = when (format) {
        "mp3" -> "mp3"
        "opus" -> "webm"
        "aac" -> "aac"
        else -> format
    }

    private fun codecFor(format: String) = when (format) {
        "mp3" -> "libmp3lame"
        "opus" -> "libopus"
        "aac" -> "aac"
        else -> throw IllegalArgumentException("Unsupported format: $format")
    }

    private fun containerFor(format: String) = when (format) {
        "mp3" -> "mp3"
        "opus" -> "webm"
        "aac" -> "adts"
        else -> throw IllegalArgumentException("Unsupported format: $format")
    }

    private fun cacheKey(checksum: String, format: String, bitrate: Int) = "${checksum}_${format}_$bitrate"

    private fun cacheFileFor(key: String, format: String) = File(getCacheDir(), "$key.${extensionFor(format)}")

    /**
     * Gets the cached audio file for a track with a specific format and bitrate.
     *
     * @param track The track to get the cached file for (based on its checksum)
     * @param format The target format (mp3, opus, aac)
     * @param bitrate The bitrate in kbps
     * @return The cached file for that track, format and bitrate (null if not found)
     */
    fun getCachedFile(track: TrackEntity, format: String, bitrate: Int): File? {
        val cacheFile = cacheFileFor(cacheKey(track.checksum, format, bitrate), format)
        return cacheFile.takeIf { it.exists() }
    }

    /**
     * Returns the cached transcoded file for [track] at [format]/[bitrate], transcoding [sourceFile] into the cache
     * first if needed. Blocks the calling thread until the whole file is ready.
     *
     * @param track The track to get/create the cached file for
     * @param sourceFile The track's original file
     * @param format The target format (mp3, opus, aac)
     * @param bitrate The bitrate in kbps
     * @return The transcoded, cached file
     */
    fun getOrCreate(track: TrackEntity, sourceFile: File, format: String, bitrate: Int): File {
        val key = cacheKey(track.checksum, format, bitrate)
        val cacheFile = cacheFileFor(key, format)

        if (cacheFile.exists()) return cacheFile

        val lock = locks.computeIfAbsent(key) { ReentrantLock() }
        lock.lock()
        try {
            if (cacheFile.exists()) return cacheFile

            val tempFile = File(getCacheDir(), "$key.tmp")
            transcodeToFile(sourceFile, tempFile, format, bitrate)
            finishCacheFile(tempFile, cacheFile)
            return cacheFile
        }
        finally {
            lock.unlock()
        }
    }

    /**
     * Starts transcoding [sourceFile] in the background, writing the output into both the cache file and the returned
     * session's queue as it's produced. The transcode runs to completion regardless of whether the caller keeps
     * draining the queue, so a client disconnecting mid-playback doesn't stop the cache from being populated for later
     * requests.
     *
     * [TeeSession.EOF] marks the end of the stream, [TeeSession.failure] contains a potential error if transcoding failed.
     *
     * @param track The track to get/create the cached file for
     * @param sourceFile The track's original file
     * @param format The target format (mp3, opus, aac)
     * @param bitrate The bitrate in kbps
     * @return A [TeeSession] that can be drained to get the transcoding output
     */
    fun streamAndCache(track: TrackEntity, sourceFile: File, format: String, bitrate: Int): TeeSession {
        val key = cacheKey(track.checksum, format, bitrate)
        val cacheFile = cacheFileFor(key, format)
        val session = TeeSession()

        backgroundScope.launch {
            try {
                val lock = locks.computeIfAbsent(key) { ReentrantLock() }
                lock.lock()
                try {
                    if (cacheFile.exists()) {
                        streamFileTo(cacheFile, session.queue)
                    }
                    else {
                        val tempFile = File(getCacheDir(), "$key.tmp")
                        transcodeAndTee(sourceFile, tempFile, format, bitrate, session.queue)
                        finishCacheFile(tempFile, cacheFile)
                    }
                }
                finally {
                    lock.unlock()
                }
            }
            catch (e: Exception) {
                logger.error("Error while streaming/caching transcode for $key", e)
                session.failure = e
            }
            finally {
                session.queue.put(TeeSession.EOF)
            }
        }

        return session
    }

    /**
     * Streams the content from a file to a [LinkedBlockingQueue].
     *
     * @param file The file to stream the data from
     * @param queue The queue to stream the data to
     */
    private fun streamFileTo(file: File, queue: LinkedBlockingQueue<ByteArray>) {
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                queue.put(buffer.copyOf(read))
            }
        }
    }

    /**
     * Transcode the source audio file into a temporary file and tees the result to the queue.
     *
     * @param sourceFile The track's original file
     * @param tempFile The target file
     * @param format The target format (mp3, opus, aac)
     * @param bitrate The bitrate in kbps
     * @param queue The queue the transcoded data gets tee'd (?) to
     */
    private suspend fun transcodeAndTee(sourceFile: File, tempFile: File, format: String, bitrate: Int, queue: LinkedBlockingQueue<ByteArray>) = coroutineScope {
        val args = listOf(
            "-hide_banner", "-loglevel", "error", "-i", sourceFile.absolutePath,
            "-vn", "-c:a", codecFor(format), "-b:a", "${bitrate}k", "-f", containerFor(format), "-"
        )

        val process = ProcessBuilder(listOf("ffmpeg") + args).start()

        val stderr = async(Dispatchers.IO) {
            process.errorStream.bufferedReader().use { it.readText() }
        }

        // Stream transcoding result into temporary file and tee to the session's queue
        tempFile.outputStream().use { fileOut ->
            process.inputStream.use { procOut ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    // Read from process
                    val read = procOut.read(buffer)
                    // Check if finished
                    if (read < 0) break
                    // Write to file
                    fileOut.write(buffer, 0, read)
                    // Write to queue
                    queue.put(buffer.copyOf(read))
                }
            }
        }

        val exitCode = process.waitFor()
        val errorOutput = stderr.await()

        if (exitCode != 0) {
            tempFile.delete()
            throw RuntimeException("ffmpeg transcoding failed with exit code $exitCode: $errorOutput")
        }
    }

    /**
     * Transcodes the source audio file into the specified output file using the provided format and bitrate.
     *
     * @param sourceFile The source file to transcode from
     * @param outputFile The target file to transcode to
     * @param format The target format (mp3, opus, aac)
     * @param bitrate The target bitrate in kbps
     * @throws RuntimeException ffmpeg transcoding failed
     */
    private fun transcodeToFile(sourceFile: File, outputFile: File, format: String, bitrate: Int) {
        val args = listOf(
            "-hide_banner", "-loglevel", "error", "-y", "-i", sourceFile.absolutePath,
            "-vn", "-c:a", codecFor(format), "-b:a", "${bitrate}k", "-f", containerFor(format), outputFile.absolutePath
        )

        val process = ProcessBuilder(listOf("ffmpeg") + args)
            .redirectErrorStream(true)
            .start()

        val exitCode = process.waitFor()
        if (exitCode != 0 || !outputFile.exists()) {
            outputFile.delete()
            throw RuntimeException("ffmpeg transcoding failed with exit code $exitCode")
        }
    }

    /**
     * Called when transcoding has finished. Moves the cached file from a temporary location to the cache directory.
     *
     * @param tempFile The location of the file currently containing transcoded data
     * @param cacheFile The target location it gets moved/renamed to
     */
    private fun finishCacheFile(tempFile: File, cacheFile: File) {
        if (cacheFile.exists()) {
            tempFile.delete()
            return
        }
        if (!tempFile.renameTo(cacheFile)) {
            tempFile.delete()
            throw RuntimeException("Failed to move transcoded file into cache: ${cacheFile.absolutePath}")
        }
        logger.info("Cached transcoded track at: ${cacheFile.absolutePath}")
    }

    /**
     * Deletes all cached files for the track with the specified checksum.
     *
     * @param checksum The checksum of the track to delete the cache for
     */
    fun evictCacheForTrack(checksum: String) {
        val cachedFiles = getCacheDir().listFiles { file -> file.name.startsWith("${checksum}_") } ?: return
        for (file in cachedFiles) {
            try {
                file.delete()
            }
            catch (e: Exception) {
                logger.error("Error evicting cached transcode file: ${file.absolutePath}", e)
            }
        }
    }
}

/**
 * A live tee of a transcode in progress: chunks appear on [queue] as ffmpeg produces them, the same bytes it also writes
 * to the cache file. [EOF] is enqueued exactly once, when the transcode succeeded or failed (see [failure] to check).
 */
class TeeSession {
    val queue: LinkedBlockingQueue<ByteArray> = LinkedBlockingQueue()

    /**
     * Null if transcoding was successful, contains the error if it failed.
     * Must be checked AFTER [EOF] has been sent.
     */
    @Volatile
    var failure: Throwable? = null

    companion object {
        val EOF: ByteArray = ByteArray(0)
    }
}
