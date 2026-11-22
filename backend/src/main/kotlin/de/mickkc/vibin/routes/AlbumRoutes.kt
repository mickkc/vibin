package de.mickkc.vibin.routes

import de.mickkc.vibin.dto.KeyValueDto
import de.mickkc.vibin.dto.PaginatedDto
import de.mickkc.vibin.dto.SuccessDto
import de.mickkc.vibin.dto.albums.AlbumDataDto
import de.mickkc.vibin.dto.albums.AlbumDto
import de.mickkc.vibin.dto.albums.AlbumEditDto
import de.mickkc.vibin.dto.tracks.MinimalTrackDto
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.AlbumRepo
import de.mickkc.vibin.repos.TrackRepo
import io.ktor.openapi.jsonSchema
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.ExperimentalKtorApi

@OptIn(ExperimentalKtorApi::class)
fun Application.configureAlbumRoutes() = routing {

    authenticate("tokenAuth") {

        getP("/api/albums", PermissionType.VIEW_ALBUMS) {

            val params = call.getPaginatedSearchParams()
            val showSingles = call.getBooleanOrDefault("showSingles", true)
            val userId = call.getUserId() ?: unauthorized()

            val (albums, total) = AlbumRepo.getAll(params, showSingles, userId)

            call.respond(PaginatedDto(
                items = AlbumRepo.toDto(albums),
                total = total.toInt(),
                pageSize = params.pageSize,
                currentPage = params.page
            ))
        }.describe {
            operationId = "getAllAlbums"
            summary = "Get all albums"
            tag("Albums")
            parameters {
                paginatedSearch()
                q("showSingles", true, required = false)
            }
            authResponses(PermissionType.VIEW_ALBUMS) {
                successResponse<PaginatedDto<AlbumDto>>("A paginated list of albums")
            }
        }

        postP("/api/albums", PermissionType.MANAGE_ALBUMS) {
            val createDto = call.receive<AlbumEditDto>()
            createDto.validate()

            if (createDto.title == null) {
                missingParameter("title")
            }

            val newAlbum = AlbumRepo.create(createDto)
            call.respond(AlbumRepo.toDto(newAlbum))
        }.describe {
            operationId = "createAlbum"
            summary = "Create a new album"
            tag("Albums")
            requestBody {
                description = "The data for the new album. At least the title must be non-null."
                schema = jsonSchema<AlbumEditDto>()
            }
            authResponses(PermissionType.MANAGE_ALBUMS) {
                successResponse<AlbumDto>("The created album")
                missingParameterResponse("title")
                invalidParameterResponse("title", "title is longer than 255 characters")
            }
        }

        getP("/api/albums/autocomplete", PermissionType.VIEW_ALBUMS) {

            val query = call.getStringParameter("query")
            val limit = call.getIntOrDefault("limit", 10)

            val albumNames = AlbumRepo.autocomplete(query, limit)

            call.respond(albumNames)
        }.describe {
            operationId = "getAlbumAutocomplete"
            summary = "Get album name autocompletions"
            tag("Albums")
            parameters {
                q<String>("query", "Whenev", required = true)
                q("limit", 10, required = false, description = "Maximum number of completions to return.")
            }
            authResponses(PermissionType.VIEW_ALBUMS) {
                successResponse<List<String>>("Autocompleted names",
                    exampleValue = listOf("Whenever You Need Somebody", "Whenever, Wherever"))
            }
        }

        getP("/api/albums/{albumId}", PermissionType.VIEW_ALBUMS) {

            val albumId = call.getLongParameter("albumId")
            val album = AlbumRepo.getById(albumId) ?: notFound()
            val userId = call.getUserId() ?: unauthorized()

            call.respond(AlbumRepo.toDataDto(album, userId))
        }.describe {
            operationId = "getAlbumById"
            summary = "Get an album and all of its tracks"
            tag("Albums")
            parameters {
                p("albumId", 1L)
            }
            authResponses(PermissionType.VIEW_ALBUMS) {
                successResponse<AlbumDataDto>("The album and all of its tracks")
                notFoundResponse("Album")
            }
        }

        deleteP("/api/albums/{albumId}", PermissionType.DELETE_ALBUMS) {
            val albumId = call.getLongParameter("albumId")
            val deleted = AlbumRepo.delete(albumId)

            if (!deleted) {
                notFound()
            }

            call.success()
        }.describe {
            operationId = "deleteAlbum"
            summary = "Delete an album and all of its tracks"
            tag("Albums")
            parameters {
                p("albumId", 1L)
            }
            authResponses(PermissionType.DELETE_ALBUMS) {
                successResponse<SuccessDto>("Album deleted successfully")
                notFoundResponse("Album")
            }
        }

        getP("/api/albums/artists/{artistId}", PermissionType.VIEW_ALBUMS, PermissionType.VIEW_TRACKS) {

            val artistId = call.getLongParameter("artistId")
            val albums = AlbumRepo.getByArtistId(artistId)

            val dtos = albums.map { (album, tracks) ->
                KeyValueDto(
                    key = AlbumRepo.toDto(album),
                    value = TrackRepo.toMinimalDto(tracks)
                )
            }

            call.respond(dtos)
        }.describe {
            operationId = "getArtistDiscography"
            summary = "Get an Artists discography"
            tag("Albums")
            parameters {
                p("artistId", 1L)
            }
            authResponses(PermissionType.VIEW_ALBUMS, PermissionType.VIEW_TRACKS) {
                successResponse<KeyValueDto<AlbumDto, MinimalTrackDto>>("The artist discography, where the key is the album, and the value a list of minimal tracks.")
            }
        }

        putP("/api/albums/{albumId}", PermissionType.MANAGE_ALBUMS) {

            val albumId = call.getLongParameter("albumId")
            val editDto = call.receive<AlbumEditDto>()

            editDto.validate()

            val updated = AlbumRepo.update(albumId, editDto) ?: notFound()
            call.respond(AlbumRepo.toDto(updated))
        }.describe {
            operationId = "updateAlbum"
            summary = "Update an album"
            tag("Albums")
            requestBody {
                schema = jsonSchema<AlbumEditDto>()
            }
            authResponses(PermissionType.MANAGE_ALBUMS) {
                successResponse<AlbumDataDto>("The updated album")
                notFoundResponse("Album")
                invalidParameterResponse("title", "title is longer than 255 characters.")
            }
        }

        getP("/api/albums/{albumId}/cover", PermissionType.VIEW_ALBUMS) {
            val albumId = call.getLongParameter("albumId")
            val album = AlbumRepo.getById(albumId) ?: notFound()
            val quality = call.getIntOrDefault("quality", 0)
            val imageId = AlbumRepo.getAlbumCover(album)?.id?.value

            call.respondRedirect("/api/images/${if (imageId != null) "by-id/${imageId}" else "default" }?type=album&quality=$quality")
        }.describe {
            operationId = "getAlbumCover"
            summary = "Get an album cover"
            description = "Redirects to the cover image of an album, or the default placeholder if the album does not have a cover"
            tag("Albums")
            parameters {
                p("albumId", 1L)
                q("quality", 0, required = false,
                    description = "The width and height of the image. 0 means the original size.")
            }
            authResponses(PermissionType.VIEW_ALBUMS) {
                redirectResponse("`/api/images/by-id/{imageId}` or `/api/images/default`")
                notFoundResponse("Album")
            }
        }
    }
}