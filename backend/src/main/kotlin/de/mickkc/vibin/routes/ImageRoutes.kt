package de.mickkc.vibin.routes

import de.mickkc.vibin.images.ImageCache
import de.mickkc.vibin.utils.AccessCryptoUtil
import de.mickkc.vibin.repos.ImageRepo
import de.mickkc.vibin.utils.PathUtils
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.ExperimentalKtorApi

@OptIn(ExperimentalKtorApi::class)
fun Application.configureImageRoutes() = routing {

    authenticate("tokenAuth") {

        // Authenticated endpoint to get image by ID (used in app)

        get("/api/images/default") {
            val type = call.getStringParameter("type")
            val quality = call.getIntOrDefault("quality", 0)

            val file = PathUtils.getDefaultImage(type, quality) ?: notFound()

            val image = ImageCache.getImageFile(file, quality, "default_$type")
            if (image == null || !image.exists()) {
                notFound()
            }

            call.respondFile(image)
        }.describe {
            operationId = "getDefaultImage"
            summary = "Get a default image"
            tag("Images")
            parameters {
                q("type", "track", required = true,
                    description = "The image type: `album`, `artist`, `playlist`, `track` or `user`.")
                q("quality", 64, required = false,
                    description = "The width and height of the image in pixels. 0 means the original size.")
            }
            authResponses {
                successResponse<ByteArray>("The default image for the specified type.")
                notFoundResponse("Image file")
            }
        }

        get("/api/images/by-id/{id}") {
            val id = call.getLongParameter("id")

            val quality = call.getIntOrDefault("quality", 0)

            val image = ImageRepo.getById(id) ?: notFound()
            val imageFile = ImageCache.getImageFile(image, quality)
            if (imageFile == null || !imageFile.exists()) {
                notFound()
            }

            call.respondFile(imageFile)
        }.describe {
            operationId = "getImageById"
            summary = "Get an image by its Id"
            tag("Images")
            parameters {
                p("id", 1L)
                q("quality", 64, required = false,
                    description = "The width and height of the image in pixels. 0 means the original size.")
            }
            authResponses {
                successResponse<ByteArray>("The image for the specified Id.")
                notFoundResponse("Image or file")
            }
        }
    }

    // Public endpoint to get image by checksum with signature validation (used in audio metadata)
    get("/api/images/by-checksum/{checksum}") {
        val checksum = call.getStringParameter("checksum")
        val sig = call.getStringParameter("sig")
        val exp = call.getLongParameter("exp")

        val quality = call.getIntOrDefault("quality", 0)

        if (!AccessCryptoUtil.validateSignature(checksum, exp, sig)) {
            unauthorized("Invalid or expired image signature")
        }

        val image = ImageRepo.getByChecksum(checksum) ?: notFound()
        val imageFile = ImageCache.getImageFile(image, quality)
        if (imageFile == null || !imageFile.exists()) {
            notFound()
        }

        call.respondFile(imageFile)
    }.describe {
        operationId = "getImageByChecksum"
        summary = "Get an image by its Checksum"
        tag("Images")
        parameters {
            p("checksum", "C7iUT7hEJH3fQ6vo8heyaRztGIs3hIe6")
            q("sig", "7QlNYUkZoFXnjcGR+2WLmw57JNDQXrQhIR7s3DfrtWY=", required = true,
                description = "The image signature")
            q("exp", 1782511260L, required = true,
                description = "Expiration timestamp in UNIX seconds")
            q("quality", 64, required = false,
                description = "The width and height of the image in pixels. 0 means the original size.")
        }
        responses {
            successResponse<ByteArray>("The image for the specified checksum.")
            unauthorizedResponse("Invalid or expired image signature")
            notFoundResponse("Image or file")
        }
    }
}