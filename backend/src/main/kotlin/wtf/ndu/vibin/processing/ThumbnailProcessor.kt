package wtf.ndu.vibin.processing

import org.slf4j.LoggerFactory
import wtf.ndu.vibin.db.images.ImageEntity
import wtf.ndu.vibin.repos.ImageRepo
import wtf.ndu.vibin.utils.ChecksumUtil
import wtf.ndu.vibin.utils.ImageUtils
import wtf.ndu.vibin.utils.PathUtils
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

object ThumbnailProcessor {

    private val logger = LoggerFactory.getLogger(ThumbnailProcessor::class.java)

    /**
     * Processes the given image data to create thumbnails of various sizes and stores them.
     *
     * @param imageData The byte array of the original image data.
     * @return An ImageEntity representing the stored thumbnails, or null if processing fails.
     */
    fun getImage(imageData: ByteArray): ImageEntity? {

        try {
            val checksum = ChecksumUtil.getChecksum(imageData)
            val existing = ImageRepo.getBySourceChecksum(checksum)

            if (existing != null) {
                return existing
            }

            val img = getImageFromByteArray(imageData)
            val colorScheme = ImageUtils.getColorThemeFromImage(img)

            val originalFile = PathUtils.getThumbnailPath("$checksum.jpg")

            originalFile.outputStream().use {
                it.write(imageData)
            }

            return ImageRepo.createImage(
                checksum = checksum,
                sourcePath = originalFile.absolutePath,
                colorScheme = colorScheme
            )
        }
        catch (e: Exception) {
            logger.error("Error processing image", e)
            return null
        }
    }

    /**
     * Converts a byte array of image data into a BufferedImage.
     *
     * @param imageData The byte array containing the image data.
     * @return A BufferedImage representation of the image data.
     */
    fun getImageFromByteArray(imageData: ByteArray): BufferedImage {
        return ImageIO.read(imageData.inputStream())
    }

    fun getImageFromFile(imageFile: File): BufferedImage {
        return ImageIO.read(imageFile)
    }

    /**
     * Scales the given image to the specified size and converts it to a JPEG byte array.
     *
     * @param image The image to scale.
     * @param size The desired size (width and height) of the scaled image. If null, the original size is used.
     * @return A byte array containing the JPEG data of the scaled image.
     */
    fun scaleImage(image: BufferedImage, size: Int? = null, imageScale: Int = Image.SCALE_SMOOTH, square: Boolean = false): ByteArray {
        val image = if (square && image.width != image.height) {
            // Make the image square by cropping the longer side
            val minSize = minOf(image.width, image.height)
            image.getSubimage(
                (image.width - minSize) / 2,
                (image.height - minSize) / 2,
                minSize,
                minSize
            )
        }
        else {
            image
        }
        val scaledImage = size?.let { image.getScaledInstance(size, size, imageScale) } ?: image
        val bufferedImage = convertUsingConstructor(scaledImage)
        return scaledImage.let {
            ByteArrayOutputStream().use {
                ImageIO.write(bufferedImage, "jpg", it)
                it.toByteArray()
            }
        }
    }

    /**
     * Converts a generic Image to a BufferedImage.
     *
     * @param image The Image to convert.
     * @return A BufferedImage representation of the input Image.
     * @throws IllegalArgumentException if the image dimensions are invalid.
     */
    @Throws(IllegalArgumentException::class)
    fun convertUsingConstructor(image: Image): BufferedImage {
        val width = image.getWidth(null)
        val height = image.getHeight(null)
        require(!(width <= 0 || height <= 0)) { "Image dimensions are invalid" }
        val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        bufferedImage.graphics.drawImage(image, 0, 0, null)
        return bufferedImage
    }

}