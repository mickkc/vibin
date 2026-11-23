package de.mickkc.vibin.routes

import de.mickkc.vibin.dto.PendingUploadDto
import de.mickkc.vibin.dto.SuccessDto
import de.mickkc.vibin.dto.UploadResultDto
import de.mickkc.vibin.dto.tracks.MinimalTrackDto
import de.mickkc.vibin.dto.tracks.TrackEditDto
import de.mickkc.vibin.parsing.parsers.preparser.PreParseException
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.TrackRepo
import de.mickkc.vibin.repos.UserRepo
import de.mickkc.vibin.uploads.PendingUpload
import de.mickkc.vibin.uploads.UploadManager
import de.mickkc.vibin.utils.PathUtils
import io.ktor.openapi.jsonSchema
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.ExperimentalKtorApi
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class, ExperimentalKtorApi::class)
fun Application.configureUploadRoutes() = routing {

    fun RoutingCall.getValidatedUpload(): PendingUpload {
        val uploadId = getStringParameter("uploadId")

        val upload = UploadManager.getById(uploadId) ?: notFound()

        val userId = getUserId() ?: unauthorized()

        if (upload.uploaderId != userId) {
            forbidden("Upload was created by another user")
        }

        return upload
    }

    authenticate("tokenAuth") {

        getP("/api/uploads", PermissionType.UPLOAD_TRACKS) {

            val userId = call.getUserId() ?: unauthorized()

            val uploads = UploadManager.getUploadsByUser(userId)

            call.respond(uploads.map { UploadManager.toDto(it) })
        }.describe {
            operationId = "getUploads"
            summary = "Get pending uploads"
            tag("Uploads")
            authResponses(PermissionType.UPLOAD_TRACKS) {
                successResponse<List<PendingUploadDto>>("A list of pending uploads for the currently " +
                        "logged-in user.")
            }
        }

        postP("/api/uploads", PermissionType.UPLOAD_TRACKS) {

            val userId = call.getUserId() ?: unauthorized()
            val filename = call.getStringParameter("filename")
            val base64data = call.receiveText()

            try {
                val data = Base64.decode(base64data)

                val upload = UploadManager.addUpload(data, filename, userId) ?: unauthorized()

                call.respond(UploadManager.toDto(upload))
            }
            catch (_: FileAlreadyExistsException) {
                conflict("A file with the same path already exists.")
            }
            catch (_: PreParseException) {
                invalidParameter("file")
            }
        }.describe {
            operationId = "createUpload"
            summary = "Upload a file"
            tag("Uploads")
            requestBody {
                description = "The Base64 encoded audio file"
                schema = jsonSchema<String>()
            }
            parameters {
                q("filename", "track.mp3", required = true)
            }
            authResponses(PermissionType.UPLOAD_TRACKS) {
                successResponse<PendingUploadDto>("The created upload")
                conflictResponse("A file already exists at the same path.")
                invalidParameterResponse("file", "file is not an audio file.")
            }
        }

        putP("/api/uploads/{uploadId}/metadata", PermissionType.UPLOAD_TRACKS) {

            val upload = call.getValidatedUpload()

            val metadata = call.receive<TrackEditDto>()
            metadata.validate()

            try {
                val updatedUpload = UploadManager.setMetadata(upload.id, metadata)
                call.respond(UploadManager.toDto(updatedUpload))
            }
            catch (_: NotFoundException) {
                notFound()
            }
        }.describe {
            operationId = "updateUploadMetadata"
            summary = "Update metadata for a pending upload"
            tag("Uploads")
            parameters {
                p("uploadId", "45c258ee-396f-443f-b127-056f54c28e19")
            }
            requestBody {
                description = "The track edit data. Null values will not be updated."
                schema = jsonSchema<TrackEditDto>()
            }
            authResponses(PermissionType.UPLOAD_TRACKS) {
                successResponse<PendingUploadDto>("The updated upload")
                notFoundResponse("Upload")
                invalidParameterResponse("title", "title is longer than 255 characters")
            }
        }

        deleteP("/api/uploads/{uploadId}", PermissionType.UPLOAD_TRACKS) {

            val upload = call.getValidatedUpload()

            try {
                UploadManager.delete(upload.id)
                call.success()
            }
            catch (_: NotFoundException) {
                notFound()
            }
        }.describe {
            operationId = "deleteUpload"
            summary = "Delete a pending upload"
            tag("Uploads")
            parameters {
                p("uploadId", "45c258ee-396f-443f-b127-056f54c28e19")
            }
            authResponses(PermissionType.UPLOAD_TRACKS) {
                successResponse<SuccessDto>("Success")
                notFoundResponse("Upload")
            }
        }

        postP("/api/uploads/{uploadId}/apply", PermissionType.UPLOAD_TRACKS) {

            val upload = call.getValidatedUpload()

            try {
                val track = UploadManager.apply(upload.id)
                call.respond(UploadResultDto(success = true, id = track.id.value))
            }
            catch (_: FileAlreadyExistsException) {
                call.respond(UploadResultDto(success = false, didFileAlreadyExist = true))
            }
            catch (_: NotFoundException) {
                notFound()
            }
        }.describe {
            operationId = "applyUpload"
            summary = "Apply a pending upload"
            tag("Uploads")
            parameters {
                p("uploadId", "45c258ee-396f-443f-b127-056f54c28e19")
            }
            authResponses(PermissionType.UPLOAD_TRACKS) {
                successResponse<UploadResultDto>("Upload result")
                notFoundResponse("Upload")
            }
        }

        getP("/api/uploads/tracks/{userId}", PermissionType.VIEW_TRACKS) {
            val userId = call.getLongParameter("userId")
            val user = UserRepo.getById(userId) ?: notFound()

            val requestingUserId = call.getUserId() ?: unauthorized()

            val uploadedTracks = TrackRepo.getUploadedByUser(user.id.value, requestingUserId)

            call.respond(TrackRepo.toMinimalDto(uploadedTracks))
        }.describe {
            operationId = "getUploadsFromUser"
            summary = "Get all tracks uploaded by a specific user"
            tag("Uploads")
            parameters {
                p("userId", 1L)
            }
            authResponses(PermissionType.VIEW_TRACKS) {
                successResponse<List<MinimalTrackDto>>("Tracks uploaded by user")
                notFoundResponse("User")
            }
        }

        getP("/api/uploads/{uploadId}/cover", PermissionType.UPLOAD_TRACKS) {

            val upload = call.getValidatedUpload()

            val coverData = UploadManager.getCoverFile(upload)

            if (coverData == null) {
                PathUtils.getDefaultImage("track", 512)?.let {
                    return@getP call.respondFile(it)
                }
                notFound()
            }

            call.respondFile(coverData)
        }.describe {
            operationId = "getUploadCover"
            summary = "Get the cover file for a pending upload"
            tag("Uploads")
            parameters {
                p("uploadId", "45c258ee-396f-443f-b127-056f54c28e19")
            }
            authResponses(PermissionType.UPLOAD_TRACKS) {
                successResponse<ByteArray>("Image data")
                notFoundResponse("Upload")
            }
        }
    }
}
