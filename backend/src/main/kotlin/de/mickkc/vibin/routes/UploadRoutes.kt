package de.mickkc.vibin.routes

import de.mickkc.vibin.dto.UploadResultDto
import de.mickkc.vibin.dto.tracks.TrackEditDto
import de.mickkc.vibin.parsing.parsers.preparser.PreParseException
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.TrackRepo
import de.mickkc.vibin.repos.UserRepo
import de.mickkc.vibin.uploads.PendingUpload
import de.mickkc.vibin.uploads.UploadManager
import de.mickkc.vibin.utils.PathUtils
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
fun Application.configureUploadRoutes() = routing {

    suspend fun RoutingCall.getValidatedUpload(): PendingUpload? {
        val uploadId = getStringParameter("uploadId") ?: return null

        val upload = UploadManager.getById(uploadId)

        if (upload == null) {
            notFound()
            return null
        }

        val userId = getUserId()

        if (userId == null) {
            unauthorized()
            return null
        }

        if (upload.uploaderId != userId) {
            forbidden()
            return null
        }

        return upload
    }

    authenticate("tokenAuth") {

        getP("/api/uploads", PermissionType.UPLOAD_TRACKS) {

            val userId = call.getUserId() ?: return@getP call.unauthorized()

            val uploads = UploadManager.getUploadsByUser(userId)

            call.respond(uploads.map { UploadManager.toDto(it) })
        }

        postP("/api/uploads", PermissionType.UPLOAD_TRACKS) {

            val userId = call.getUserId() ?: return@postP call.unauthorized()
            val filename = call.getStringParameter("filename") ?: return@postP
            val base64data = call.receiveText()

            try {
                val data = Base64.decode(base64data)

                val upload = UploadManager.addUpload(data, filename, userId) ?: return@postP call.unauthorized()

                call.respond(UploadManager.toDto(upload))
            }
            catch (_: FileAlreadyExistsException) {
                call.conflict()
            }
            catch (_: PreParseException) {
                call.invalidParameter("file")
            }
        }

        putP("/api/uploads/{uploadId}/metadata", PermissionType.UPLOAD_TRACKS) {

            val upload = call.getValidatedUpload() ?: return@putP

            val metadata = call.receive<TrackEditDto>()

            try {
                val updatedUpload = UploadManager.setMetadata(upload.id, metadata)
                call.respond(UploadManager.toDto(updatedUpload))
            }
            catch (_: NotFoundException) {
                call.notFound()
            }
        }

        deleteP("/api/uploads/{uploadId}", PermissionType.UPLOAD_TRACKS) {

            val upload = call.getValidatedUpload() ?: return@deleteP

            try {
                UploadManager.delete(upload.id)
                call.success()
            }
            catch (_: NotFoundException) {
                call.notFound()
            }

        }

        postP("/api/uploads/{uploadId}/apply", PermissionType.UPLOAD_TRACKS) {

            val upload = call.getValidatedUpload() ?: return@postP

            try {
                val track = UploadManager.apply(upload.id)
                call.respond(UploadResultDto(success = true, id = track.id.value))
            }
            catch (_: FileAlreadyExistsException) {
                call.respond(UploadResultDto(success = false, didFileAlreadyExist = true))
            }
            catch (_: NotFoundException) {
                call.notFound()
            }
        }

        getP("/api/uploads/tracks/{userId}", PermissionType.VIEW_TRACKS) {
            val userId = call.getLongParameter("userId") ?: return@getP
            val user = UserRepo.getById(userId) ?: return@getP call.notFound()

            val requestingUserId = call.getUserId() ?: return@getP call.unauthorized()

            val uploadedTracks = TrackRepo.getUploadedByUser(user.id.value, requestingUserId)

            call.respond(TrackRepo.toMinimalDto(uploadedTracks))
        }

        getP("/api/uploads/{uploadId}/cover", PermissionType.UPLOAD_TRACKS) {

            val upload = call.getValidatedUpload() ?: return@getP

            val coverData = UploadManager.getCoverFile(upload)

            if (coverData == null) {
                PathUtils.getDefaultImage("track", 512)?.let {
                    return@getP call.respondFile(it)
                }
                return@getP call.notFound()
            }

            call.respondFile(coverData)
        }
    }
}
