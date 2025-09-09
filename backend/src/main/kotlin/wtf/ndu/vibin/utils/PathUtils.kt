package wtf.ndu.vibin.utils

import wtf.ndu.vibin.config.EnvUtil
import wtf.ndu.vibin.processing.ThumbnailProcessor
import java.io.File

object PathUtils {

    /**
     * Gets a path for storing a thumbnail of the specified type and name.
     *
     * @param type The type of thumbnail (e.g., artist, album).
     * @param name The name of the thumbnail file.
     * @return A File object representing the path to the thumbnail.
     */
    fun getThumbnailPath(type: ThumbnailProcessor.ThumbnailType, name: String): File {
        val basePath = EnvUtil.getOrDefault(EnvUtil.THUMBNAIL_DIR, EnvUtil.DEFAULT_THUMBNAIL_DIR)
        return File(basePath, type.dir).apply { if (!exists()) mkdirs() }
            .resolve(name)
    }

    /**
     * Gets a thumbnail file from a given relative path.
     *
     * @param path The relative path to the thumbnail file.
     * @return A File object representing the thumbnail file.
     */
    fun getThumbnailFileFromPath(path: String): File {
        val basePath = EnvUtil.getOrDefault(EnvUtil.THUMBNAIL_DIR, EnvUtil.DEFAULT_THUMBNAIL_DIR)
        return File(basePath, path)
    }

    /**
     * Converts a File object to a relative track path based on the music directory.
     *
     * @param file The File object representing the track.
     * @return The relative path of the track as a String.
     */
    fun getTrackPathFromFile(file: File): String {
        val songsDir = File(EnvUtil.getOrDefault(EnvUtil.MUSIC_DIR, EnvUtil.DEFAULT_MUSIC_DIR))
        return file.toRelativeString(songsDir)
    }

    /**
     * Gets a track file from a given relative path.
     *
     * @param path The relative path of the track.
     * @return A File object representing the track file.
     */
    fun getTrackFileFromPath(path: String): File {
        val songsDir = File(EnvUtil.getOrDefault(EnvUtil.MUSIC_DIR, EnvUtil.DEFAULT_MUSIC_DIR))
        return File(songsDir, path)
    }

    fun getDefaultImage(type: String, quality: String = "large"): File {
        val actualQuality = when (quality.lowercase()) {
            "large", "small" -> quality.lowercase()
            else -> "large"
        }
        val resource = this::class.java.getResource("/img/default_${type}_$actualQuality.png")
        return File(resource.toURI())
    }
}