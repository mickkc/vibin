package wtf.ndu.vibin.utils

import wtf.ndu.vibin.config.EnvUtil
import java.io.File
import kotlin.io.path.Path

object PathUtils {

    /**
     * Gets a path for storing a thumbnail of the specified type and name.
     *
     * @param name The name of the thumbnail file.
     * @return A File object representing the path to the thumbnail.
     */
    fun getThumbnailPath(name: String): File {
        val basePath = EnvUtil.getOrDefault(EnvUtil.THUMBNAIL_DIR, EnvUtil.DEFAULT_THUMBNAIL_DIR)
        return getFile(basePath, name)
    }

    /**
     * Gets a thumbnail file from a given relative path.
     *
     * @param path The relative path to the thumbnail file.
     * @return A File object representing the thumbnail file.
     */
    fun getThumbnailFileFromPath(path: String): File {
        val basePath = EnvUtil.getOrDefault(EnvUtil.THUMBNAIL_DIR, EnvUtil.DEFAULT_THUMBNAIL_DIR)
        return getFile(basePath, path)
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
        val songsDir = EnvUtil.getOrDefault(EnvUtil.MUSIC_DIR, EnvUtil.DEFAULT_MUSIC_DIR)
        return getFile(songsDir, path)
    }

    fun getUploadFileFromPath(path: String): File {

        val uploadsPath = EnvUtil.getOrDefault(EnvUtil.UPLOADS_DIR, EnvUtil.DEFAULT_UPLOADS_DIR)
        return getFile(uploadsPath, path)
    }

    private fun getFile(base: String, relativePath: String): File {

        val path = Path(base, relativePath)

        if (!path.normalize().startsWith(Path(base).normalize())) {
            throw SecurityException("Attempted directory traversal attack: $relativePath")
        }

        return path.toFile().also { it.parentFile.mkdirs() }
    }

    fun getDefaultImage(type: String, quality: String = "large"): File {
        val actualQuality = when (quality.lowercase()) {
            "large", "small" -> quality.lowercase()
            else -> "large"
        }
        val resource = this::class.java.getResource("/img/default_${type}_$actualQuality.png")
        return File(resource.toURI())
    }

    private val allowedNameChars = ('a'..'z') + ('A'..'Z') + ('0'..'9') + listOf(
        '-', '_', '.', ' '
    )

    fun sanitizePath(path: String): String {
        return path.split("/", "\\").joinToString(File.separator) { component ->
            sanitizePathComponent(component)
        }
    }

    fun sanitizePathComponent(component: String): String {
        return component.map { c -> if (c in allowedNameChars) c else '_' }.joinToString("")
    }
}