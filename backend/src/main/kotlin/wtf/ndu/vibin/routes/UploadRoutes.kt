package wtf.ndu.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import wtf.ndu.vibin.db.uploads.PendingUploadEntity
import wtf.ndu.vibin.dto.UploadResultDto
import wtf.ndu.vibin.dto.tracks.TrackEditDto
import wtf.ndu.vibin.permissions.PermissionType
import wtf.ndu.vibin.repos.UploadRepo
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
fun Application.configureUploadRoutes() = routing {

    suspend fun RoutingCall.getValidatedUpload(): PendingUploadEntity? {
        val uploadId = this.parameters["uploadId"]?.toLongOrNull()

        if (uploadId == null) {
            missingParameter("uploadId")
            return null
        }

        val upload = UploadRepo.getById(uploadId)

        if (upload == null) {
            notFound()
            return null
        }

        val userId = getUserId()

        if (userId == null) {
            unauthorized()
            return null
        }

        if (!UploadRepo.checkUploader(upload, userId)) {
            forbidden()
            return null
        }

        return upload
    }

    authenticate("tokenAuth") {

        getP("/api/uploads", PermissionType.UPLOAD_TRACKS) {

            val userId = call.getUserId() ?: return@getP call.unauthorized()

            val uploads = UploadRepo.getUploadsByUser(userId)

            call.respond(UploadRepo.toDto(uploads))
        }

        postP("/api/uploads", PermissionType.UPLOAD_TRACKS) {

            val userId = call.getUserId() ?: return@postP call.unauthorized()
            val filename = call.parameters["filename"] ?: return@postP call.missingParameter("filename")
            val base64data = call.receiveText()

            try {
                val data = Base64.decode(base64data)

                val upload = UploadRepo.addUpload(data, filename, userId) ?: return@postP call.unauthorized()

                call.respond(UploadRepo.toDto(upload))
            }
            catch (e: FileAlreadyExistsException) {
                call.conflict()
            }
        }

        putP("/api/uploads/{uploadId}/metadata", PermissionType.UPLOAD_TRACKS) {

            val upload = call.getValidatedUpload() ?: return@putP

            val metadata = call.receive<TrackEditDto>()

            val updatedUpload = UploadRepo.setMetadata(upload, metadata)

            call.respond(UploadRepo.toDto(updatedUpload))
        }

        deleteP("/api/uploads/{uploadId}", PermissionType.UPLOAD_TRACKS) {

            val upload = call.getValidatedUpload() ?: return@deleteP

            UploadRepo.delete(upload)

            call.success()
        }

        postP("/api/uploads/{uploadId}/apply", PermissionType.UPLOAD_TRACKS) {

            val upload = call.getValidatedUpload() ?: return@postP

            try {
                val track = UploadRepo.apply(upload)
                call.respond(UploadResultDto(success = true, id = track.id.value))
            }
            catch (_: FileAlreadyExistsException) {
                call.respond(UploadResultDto(success = false, didFileAlreadyExist = true))
            }
        }
    }
}
