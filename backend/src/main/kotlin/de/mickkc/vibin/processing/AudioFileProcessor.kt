package de.mickkc.vibin.processing

import org.jetbrains.exposed.sql.SizedCollection
import org.slf4j.LoggerFactory
import de.mickkc.vibin.config.EnvUtil
import de.mickkc.vibin.db.tracks.TrackEntity
import de.mickkc.vibin.parsing.Parser
import de.mickkc.vibin.parsing.TrackMetadata
import de.mickkc.vibin.parsing.parsers.preparser.PreParseException
import de.mickkc.vibin.repos.TagRepo
import de.mickkc.vibin.repos.TrackRepo
import de.mickkc.vibin.settings.server.AddGenreAsTag
import de.mickkc.vibin.settings.Settings
import de.mickkc.vibin.utils.ChecksumUtil
import java.io.File

object AudioFileProcessor {

    private val logger = LoggerFactory.getLogger(AudioFileProcessor::class.java)

    /**
     * List of supported audio file extensions.
     */
    private val supportedExtensions = listOf(
        "mp3", // MP3
        "ogg", // Ogg Vorbis
        "flac", // FLAC
        "wma", // WMA
        "mp4", "m4a", "m4b", // MP4 audio variants
        "mpc", // Musepack
        "ape", // Monkey's Audio
        "ofr", "ofs", // OptimFROG
        "tta", // TTA
        "wv", // WavPack
        "spx", // Speex
        "wav", // WAV
        "aiff", "aif", "aifc", // AIFF
        "au", "snd", // AU
        "ra", "rm", // RealAudio
        "opus", // Opus
        "mp2" // MP2
    )

    /**
     * Reprocesses all audio files in the configured music directory.
     */
    suspend fun reprocessAll(): Int {
        val baseDir = EnvUtil.getOrDefault(EnvUtil.MUSIC_DIR, EnvUtil.DEFAULT_MUSIC_DIR)
        val musicDir = File(baseDir)
        if (!musicDir.exists() || !musicDir.isDirectory) {
            logger.error("MUSIC_DIR path '$baseDir' does not exist or is not a directory.")
            return 0
        }
        val processedTracks = processAllFilesInDirectory(musicDir)
        logger.info("Reprocessing complete. Total tracks processed: ${processedTracks.size}")
        return processedTracks.size
    }

    /**
     * Processes all audio files in the specified directory and its subdirectories.
     *
     * @param directoryFile The root directory to start processing from.
     * @return A list of TrackEntity objects representing the processed tracks.
     */
    suspend fun processAllFilesInDirectory(directoryFile: File): List<TrackEntity> {

        val addGenreAsTags = Settings.get(AddGenreAsTag)

        val allFiles = directoryFile.walkTopDown().filter { it.isFile }.toList()
        val audioFiles = allFiles.filter { file ->
            val extension = file.extension.lowercase()
            supportedExtensions.contains(extension)
        }

        logger.info("Found ${audioFiles.size} audio files in directory ${directoryFile.absolutePath}")

        val processedTracks = audioFiles.mapNotNull { file ->
            try {
                processSingleFile(file, addGenreAsTags)
            } catch (e: Exception) {
                logger.error("Error processing file: ${file.absolutePath}", e)
                return@mapNotNull null
            }
        }

        logger.info("Processed ${processedTracks.size} audio files successfully")
        return processedTracks
    }

    /**
     * Processes a single audio file, extracting metadata and storing it in the database.
     *
     * @param file The audio file to process.
     * @param addScannedTags Whether to add genre information as tags.
     * @return The created TrackEntity, or null if processing failed or the track already exists.
     */
    suspend fun processSingleFile(file: File, addScannedTags: Boolean = true): TrackEntity? {

        val checksum = ChecksumUtil.getChecksum(file)
        val existingTrack = TrackRepo.getByChecksum(checksum)
        if (existingTrack != null) {
            logger.info("Track already exists in database for file: ${file.absolutePath}, skipping processing.")
            return null
        }

        var metadata: TrackMetadata

        try {
            metadata = Parser.parse(file)
        }
        catch (e: PreParseException) {
            logger.error("Pre-parse failed for file: ${file.absolutePath}, skipping file. Error: ${e.message}", e)
            return null
        }

        if (metadata.trackInfo.album == null) {
            logger.warn("No album name found in metadata for file: ${file.absolutePath}")
            return null
        }

        var track = TrackRepo.createTrack(file, metadata)

        if (metadata.trackInfo.coverImageUrl != null) {
            val coverImageData = Parser.downloadCoverImage(metadata.trackInfo.coverImageUrl)
            if (coverImageData != null) {
                logger.info("Processing cover image for track ID: ${track.id.value}, title: '${track.title}'")
                val image = ThumbnailProcessor.getImage(coverImageData)
                track = TrackRepo.update(track) { this.cover = image }
            }
        }

        if (addScannedTags && metadata.trackInfo.tags != null) {
            logger.info("Adding ${metadata.trackInfo.tags.size} tags to track ID: ${track.id.value}, title: '${track.title}'")
            val genreTag = metadata.trackInfo.tags.map { tag -> TagRepo.getOrCreateTag(tag) }
            track = TrackRepo.update(track) { this.tags = SizedCollection(genreTag) }
        }

        logger.info("Processed file: ${file.absolutePath} into track ID: ${track.id.value}, title: '${track.title}'")
        return track
    }
}