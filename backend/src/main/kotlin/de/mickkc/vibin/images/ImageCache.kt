package de.mickkc.vibin.images

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import de.mickkc.vibin.config.EnvUtil
import de.mickkc.vibin.db.images.ImageEntity
import de.mickkc.vibin.processing.ThumbnailProcessor
import de.mickkc.vibin.utils.PathUtils
import java.io.File

object ImageCache {

    private val logger: Logger = LoggerFactory.getLogger(ImageCache::class.java)
    private val imageCachePath = File(EnvUtil.getOrDefault(EnvUtil.IMAGE_CACHE_DIR, EnvUtil.DEFAULT_IMAGE_CACHE_DIR))

    init {
        if (!imageCachePath.exists()) {
            imageCachePath.mkdirs()
        }
    }

    fun getImageFile(imageEntity: ImageEntity, size: Int): File? {
        val imageFile = File(imageCachePath, "${imageEntity.id.value}_$size.jpg")
        if (imageFile.exists()) {
            return imageFile
        }

        val originalFile = PathUtils.getThumbnailFileFromPath(imageEntity.sourcePath)
        return getImageFile(originalFile, size, imageEntity.id.value.toString())
    }

    fun getImageFile(originalFile: File, size: Int, name: String): File? {

        if (size <= 0) {
            return originalFile
        }

        try {
            val imageFile = File(imageCachePath, "${name}_$size.jpg")
            if (imageFile.exists()) {
                return imageFile
            }

            logger.info("Resizing image $name to $size")

            if (!originalFile.exists()) {
                logger.error("Original image file does not exist: ${originalFile.absolutePath}")
                return null
            }

            val originalImage = ThumbnailProcessor.getImageFromFile(originalFile)
            val scaledImage = ThumbnailProcessor.scaleImage(originalImage, size, square = true)
            imageFile.outputStream().use {
                it.write(scaledImage)
            }

            logger.info("Cached resized image at: ${imageFile.absolutePath}")

            return imageFile
        }
        catch (e: Exception) {
            logger.error("Error getting resized image file", e)
            return null
        }
    }

    fun evictCacheForImageId(imageId: Long) {
        val cachedFiles = imageCachePath.listFiles { file ->
            file.name.startsWith("${imageId}_")
        } ?: return

        for (file in cachedFiles) {
            try {
                file.delete()
                logger.info("Evicted cached image file: ${file.absolutePath}")
            } catch (e: Exception) {
                logger.error("Error evicting cached image file: ${file.absolutePath}", e)
            }
        }
    }
}