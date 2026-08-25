package de.mickkc.vibin.routes

import de.mickkc.vibin.images.ImageCache
import de.mickkc.vibin.utils.AccessCryptoUtil
import de.mickkc.vibin.repos.ImageRepo
import de.mickkc.vibin.utils.PathUtils
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

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
    }
}