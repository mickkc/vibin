package wtf.ndu.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import wtf.ndu.vibin.dto.UploadResultDto
import wtf.ndu.vibin.dto.tracks.TrackEditDto
import wtf.ndu.vibin.parsing.parsers.preparser.PreParseException
import wtf.ndu.vibin.permissions.PermissionType
import wtf.ndu.vibin.repos.TrackRepo
import wtf.ndu.vibin.uploads.PendingUpload
import wtf.ndu.vibin.uploads.UploadManager
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
fun Application.configureUploadRoutes() = routing {

    suspend fun RoutingCall.getValidatedUpload(): PendingUpload? {
        val uploadId = this.parameters["uploadId"]

        if (uploadId == null) {
            missingParameter("uploadId")
            return null
        }

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
            val filename = call.parameters["filename"] ?: return@postP call.missingParameter("filename")
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
            val userId = call.parameters["userId"]?.toLongOrNull() ?: return@getP call.missingParameter("userId")
            val requestingUserId = call.getUserId() ?: return@getP call.unauthorized()

            val uploadedTracks = TrackRepo.getUploadedByUser(userId, requestingUserId)

            call.respond(TrackRepo.toMinimalDto(uploadedTracks))
        }
    }
}
