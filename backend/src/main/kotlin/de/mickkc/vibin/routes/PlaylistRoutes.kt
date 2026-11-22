package de.mickkc.vibin.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import de.mickkc.vibin.dto.PaginatedDto
import de.mickkc.vibin.dto.SuccessDto
import de.mickkc.vibin.dto.playlists.PlaylistDataDto
import de.mickkc.vibin.dto.playlists.PlaylistDto
import de.mickkc.vibin.dto.playlists.PlaylistEditDto
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.PlaylistRepo
import io.ktor.openapi.jsonSchema
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.ExperimentalKtorApi

@OptIn(ExperimentalKtorApi::class)
fun Application.configurePlaylistRoutes() = routing {
    authenticate("tokenAuth") {

        getP("/api/playlists", PermissionType.VIEW_PLAYLISTS) {

            val userId = call.getUserId() ?: unauthorized()
            val params = call.getPaginatedSearchParams()
            val onlyOwn = call.getBooleanOrDefault("onlyOwn", false)

            // Get the playlists for the requested page
            val (playlists, total) = PlaylistRepo.getAll(params, userId, onlyOwn)

            call.respond(PaginatedDto(
                items = PlaylistRepo.toDto(playlists),
                total = total.toInt(),
                pageSize = params.pageSize,
                currentPage = params.page
            ))
        }.describe {
            operationId = "getAllPlaylists"
            summary = "Get all playlists"
            tag("Playlists")
            parameters {
                paginatedSearch()
                q("onlyOwn", false, required = false,
                    description = "Only show playlists the user owns or collaborates on")
            }
            authResponses(PermissionType.VIEW_PLAYLISTS) {
                successResponse<PaginatedDto<PlaylistDto>>("A paginated list of playlists")
            }
        }

        getP("/api/playlists/{playlistId}", PermissionType.VIEW_PLAYLISTS, PermissionType.VIEW_TRACKS) {
            val userId = call.getUserId() ?: unauthorized()
            val playlistId = call.getLongParameter("playlistId")

            val playlist = PlaylistRepo.getById(playlistId, userId) ?: notFound()

            call.respond(PlaylistRepo.toDataDto(playlist, userId))
        }.describe {
            operationId = "getPlaylistById"
            summary = "Get a playlist and all of its tracks"
            tag("Playlists")
            parameters {
                p("playlistId", 1L)
            }
            authResponses(PermissionType.VIEW_PLAYLISTS, PermissionType.VIEW_TRACKS) {
                successResponse<PlaylistDataDto>("The playlist and all of its tracks")
                notFoundResponse("Playlist")
            }
        }

        getP("/api/playlists/users/{userId}", PermissionType.VIEW_PLAYLISTS) {
            val requestingUserId = call.getUserId() ?: unauthorized()
            val userId = call.getLongParameter("userId")

            if (requestingUserId != userId && !call.hasPermissions(PermissionType.VIEW_USERS)) {
                noPermission(PermissionType.VIEW_USERS)
            }

            val playlists = PlaylistRepo.getAllForUser(userId, userId == requestingUserId)

            call.respond(PlaylistRepo.toDto(playlists))
        }.describe {
            operationId = "getPlaylistsForUser"
            summary = "Get all playlists for a user"
            description = "When requesting playlists for yourself, private playlists will be included. " +
                    "Otherwise, only public playlists are included."
            tag("Playlists")
            parameters {
                p("userId", 1L)
            }
            authResponses(PermissionType.VIEW_PLAYLISTS, PermissionType.VIEW_USERS) {
                successResponse<List<PlaylistDto>>("All playlists for the user")
            }
            optionalPermissions(
                PermissionType.VIEW_USERS to "Include playlists from other users"
            )
        }

        getP("/api/playlists/random", PermissionType.VIEW_PLAYLISTS) {
            val userId = call.getUserId() ?: unauthorized()
            val limit = call.getIntOrDefault("limit", 1)

            val playlists = PlaylistRepo.getRandom(limit, userId)

            call.respond(PlaylistRepo.toDto(playlists))
        }.describe {
            operationId = "getRandomPlaylists"
            summary = "Get random playlists"
            tag("Playlists")
            parameters {
                q("limit", 5, required = false, description = "Maximum amount of playlists to return")
            }
            authResponses(PermissionType.VIEW_PLAYLISTS) {
                successResponse<List<PlaylistDto>>("A list of random playlists")
            }
        }

        suspend fun getValidatedEditDto(call: RoutingCall): PlaylistEditDto {
            val editDto = call.receive<PlaylistEditDto>()
            editDto.validate()

            if (editDto.name.isBlank()) {
                missingParameter("name")
            }

            // Check permissions for public/private playlists
            if ((editDto.isPublic == true && !call.hasPermissions(PermissionType.CREATE_PUBLIC_PLAYLISTS) ||
                (editDto.isPublic == false && !call.hasPermissions(PermissionType.CREATE_PRIVATE_PLAYLISTS)))) {
                noPermission(if (editDto.isPublic) PermissionType.CREATE_PUBLIC_PLAYLISTS else PermissionType.CREATE_PRIVATE_PLAYLISTS)
            }

            return editDto
        }

        postP("/api/playlists", PermissionType.MANAGE_PLAYLISTS) {

            val user = call.getUser() ?: unauthorized()
            val editDto = getValidatedEditDto(call)

            // Create the playlist
            val created = PlaylistRepo.createOrUpdatePlaylist(user, editDto, null)!!

            call.respond(PlaylistRepo.toDto(created))
        }.describe {
            operationId = "createPlaylist"
            summary = "Create a playlist"
            tag("Playlists")
            requestBody {
                description = "The playlist edit data. At least the name must be non-null"
                schema = jsonSchema<PlaylistEditDto>()
            }
            authResponses(PermissionType.MANAGE_PLAYLISTS, PermissionType.CREATE_PUBLIC_PLAYLISTS, PermissionType.CREATE_PRIVATE_PLAYLISTS) {
                successResponse<PlaylistDto>("The created playlist")
                missingParameterResponse("name")
                invalidParameterResponse("name", "name is longer than 255 characters")
            }
            optionalPermissions(
                PermissionType.CREATE_PUBLIC_PLAYLISTS to "Create public playlists",
                PermissionType.CREATE_PRIVATE_PLAYLISTS to "Create private playlists"
            )
        }

        putP("/api/playlists/{playlistId}", PermissionType.MANAGE_PLAYLISTS) {
            val user = call.getUser() ?: unauthorized()
            val playlistId = call.getLongParameter("playlistId")
            val editDto = getValidatedEditDto(call)

            // Get the playlist to check ownership
            val playlist = PlaylistRepo.getByIdIfAllowed(playlistId, user.id.value, PermissionType.EDIT_COLLABORATIVE_PLAYLISTS)
                ?: notFound()

            // Update the playlist
            val updated = PlaylistRepo.createOrUpdatePlaylist(user, editDto, playlist.id.value) ?: notFound()

            call.respond(PlaylistRepo.toDto(updated))
        }.describe {
            operationId = "updatePlaylist"
            summary = "Update a playlist"
            tag("Playlists")
            requestBody {
                description = "The playlist edit data. Null values will not be updated"
                schema = jsonSchema<PlaylistEditDto>()
            }
            parameters {
                p("playlistId", 1L)
            }
            authResponses(PermissionType.MANAGE_PLAYLISTS, PermissionType.CREATE_PUBLIC_PLAYLISTS, PermissionType.CREATE_PRIVATE_PLAYLISTS) {
                successResponse<PlaylistDto>("The updated playlist")
                notFoundResponse("Playlists")
            }
            optionalPermissions(
                PermissionType.CREATE_PUBLIC_PLAYLISTS to "Change to a public playlists",
                PermissionType.CREATE_PRIVATE_PLAYLISTS to "Change to a private playlists",
                PermissionType.EDIT_COLLABORATIVE_PLAYLISTS to "Update playlists the user is a collaborator on"
            )
        }

        deleteP("/api/playlists/{playlistId}", PermissionType.DELETE_OWN_PLAYLISTS) {
            val userId = call.getUserId() ?: unauthorized()
            val playlistId = call.getLongParameter("playlistId")

            // Get the playlist to check ownership
            val playlist = PlaylistRepo.getByIdIfAllowed(playlistId, userId, PermissionType.DELETE_COLLABORATIVE_PLAYLISTS)
                ?: notFound()

            PlaylistRepo.deletePlaylist(playlist.id.value)

            call.success()
        }.describe {
            operationId = "deletePlaylist"
            summary = "Delete a playlist"
            tag("Playlists")
            parameters {
                p("playlistId", 1L)
            }
            authResponses(PermissionType.DELETE_OWN_PLAYLISTS) {
                successResponse<SuccessDto>("Success")
                notFoundResponse("Playlist")
            }
            optionalPermissions(
                PermissionType.DELETE_COLLABORATIVE_PLAYLISTS to "Delete playlists the user is a collaborator on"
            )
        }

        getP("/api/playlists/{playlistId}/image", PermissionType.VIEW_PLAYLISTS) {
            val userId = call.getUserId() ?: unauthorized()
            val playlistId = call.getLongParameter("playlistId")
            val quality = call.getIntOrDefault("quality", 0)

            val playlist = PlaylistRepo.getById(playlistId, userId) ?: notFound()

            val imageBytes = PlaylistRepo.getCoverImageBytes(playlist, quality) ?: notFound()

            call.respondBytes(imageBytes, contentType = ContentType.Image.JPEG)
        }.describe {
            operationId = "getPlaylistImage"
            summary = "Get the cover image for a playlist"
            description = "If not explicitly set, an image is generated from the first 4 album covers of the playlist."
            tag("Playlists")
            parameters {
                p("playlistId", 1L)
                q("quality", 0, required = false,
                    description = "The width and height of the image. 0 means the original size.")
            }
            responses {
                successResponse<ByteArray>("The playlist image")
                notFoundResponse("Image or file")
            }
        }
    }
}
