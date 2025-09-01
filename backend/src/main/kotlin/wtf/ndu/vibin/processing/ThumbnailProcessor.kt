package wtf.ndu.vibin.processing

import org.slf4j.LoggerFactory
import wtf.ndu.vibin.db.ImageEntity
import wtf.ndu.vibin.repos.ImageRepo
import wtf.ndu.vibin.utils.ImageUtils
import wtf.ndu.vibin.utils.PathUtils
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.math.max

object ThumbnailProcessor {

    private val logger = LoggerFactory.getLogger(ThumbnailProcessor::class.java)

    enum class ThumbnailType(val dir: String) {
        USER("users"), TRACK("tracks"), PLAYLIST("playlists"), ALBUM("albums"), ARTIST("artists")
    }

    /**
     * Processes the given image data to create thumbnails of various sizes and stores them.
     *
     * @param imageData The byte array of the original image data.
     * @param type The type of thumbnail to create.
     * @param name The base name to use for the thumbnail files.
     * @return An ImageEntity representing the stored thumbnails, or null if processing fails.
     */
    fun getImage(imageData: ByteArray, type: ThumbnailType, name: String): ImageEntity? {

        try {
            val img = getImage(imageData)
            val size = max(img.width, img.height)

            val original = scaleImage(img)
            val small = scaleImage(img, 128)
            val large = if (size > 512) scaleImage(img, 512) else null

            val colorScheme = ImageUtils.getColorThemeFromImage(img)

            val originalFile = PathUtils.getThumbnailPath(type, "$name.jpg")
            val smallFile = PathUtils.getThumbnailPath(type, "$name-128.jpg")
            val largeFile = large?.let { PathUtils.getThumbnailPath(type, "$name-512.jpg") }

            originalFile.writeBytes(original)
            smallFile.writeBytes(small)
            large?.let { largeFile?.writeBytes(it) }

            return ImageRepo.createImage(
                originalUrl = "${type.dir}/$name.jpg",
                smallUrl = "${type.dir}/$name-128.jpg",
                largeUrl = large?.let { "${type.dir}/$name-512.jpg" },
                colorScheme = colorScheme
            )
        }
        catch (e: Exception) {
            logger.error("Error processing image for $type with name $name", e)
            return null
        }
    }

    /**
     * Converts a byte array of image data into a BufferedImage.
     *
     * @param imageData The byte array containing the image data.
     * @return A BufferedImage representation of the image data.
     */
    fun getImage(imageData: ByteArray): BufferedImage {
        val image = ImageIO.read(imageData.inputStream())
        return convertUsingConstructor(image)
    }

    /**
     * Scales the given image to the specified size and converts it to a JPEG byte array.
     *
     * @param image The image to scale.
     * @param size The desired size (width and height) of the scaled image. If null, the original size is used.
     * @return A byte array containing the JPEG data of the scaled image.
     */
    fun scaleImage(image: Image, size: Int? = null, imageScale: Int = Image.SCALE_SMOOTH): ByteArray {
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