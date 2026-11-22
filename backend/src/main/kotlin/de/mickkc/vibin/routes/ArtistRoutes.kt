package de.mickkc.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import de.mickkc.vibin.dto.PaginatedDto
import de.mickkc.vibin.dto.SuccessDto
import de.mickkc.vibin.dto.artists.ArtistDto
import de.mickkc.vibin.dto.artists.ArtistEditData
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.ArtistRepo
import io.ktor.openapi.jsonSchema
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.ExperimentalKtorApi

@OptIn(ExperimentalKtorApi::class)
fun Application.configureArtistRoutes() = routing {

    authenticate("tokenAuth") {
        getP("/api/artists", PermissionType.VIEW_ARTISTS) {

            val params = call.getPaginatedSearchParams()
            val userId = call.getUserId() ?: unauthorized()

            val (artists, total) = ArtistRepo.getAll(params, userId)

            call.respond(PaginatedDto(
                items = ArtistRepo.toDto(artists),
                total = total.toInt(),
                currentPage = params.page,
                pageSize = params.pageSize
            ))
        }.describe {
            operationId = "getAllArtists"
            summary = "Get all artists"
            tag("Artists")
            parameters {
                paginatedSearch()
            }
            authResponses(PermissionType.VIEW_ARTISTS) {
                successResponse<PaginatedDto<ArtistDto>>("A paginated list of artists")
            }
        }

        getP("/api/artists/{id}", PermissionType.VIEW_ARTISTS) {
            val id = call.getLongParameter("id")
            val artist = ArtistRepo.getById(id) ?: notFound()
            call.respond(ArtistRepo.toDto(artist))
        }.describe {
            operationId = "getArtist"
            summary = "Get an artist"
            tag("Artists")
            parameters {
                p("id", 1L, description = "The artist's Id")
            }
            authResponses(PermissionType.VIEW_ARTISTS) {
                successResponse<ArtistDto>("The artist")
                notFoundResponse("Artist")
            }
        }

        getP("/api/artists/ids", PermissionType.VIEW_ARTISTS) {
            val idsParam = call.getStringParameter("ids")
            val ids = idsParam.split(",").mapNotNull { it.toLongOrNull() }
            val artists = ArtistRepo.getByIds(ids)
            call.respond(ArtistRepo.toDto(artists))
        }.describe {
            operationId = "getMultipleArtists"
            summary = "Get multiple artists"
            description = "Only artists that could be found will be returned."
            tag("Artists")
            parameters {
                q("ids", "1,2,3,4", required = true, description = "A comma-separated list of Ids")
            }
            authResponses(PermissionType.VIEW_ARTISTS) {
                successResponse<List<ArtistDto>>("A list of artists matching the Ids, sorted by name (may not be in the same order as the Ids)")
            }
        }

        postP("/api/artists", PermissionType.MANAGE_ARTISTS) {
            val data = call.receive<ArtistEditData>()
            data.validate()

            try {
                val updatedArtist = ArtistRepo.updateOrCreateArtist(null, data)
                call.respond(ArtistRepo.toDto(updatedArtist))
            }
            catch (e400: IllegalStateException) {
                missingParameter(e400.message!!)
            }
            catch (_: NotFoundException) {
                notFound()
            }
        }.describe {
            operationId = "createArtist"
            summary = "Create an artist"
            tag("Artists")
            requestBody {
                description = "The artist edit data. At least the artist name must be non-null."
                schema = jsonSchema<ArtistEditData>()
            }
            authResponses(PermissionType.MANAGE_ARTISTS) {
                successResponse<ArtistDto>("The created artist")
                missingParameterResponse("name")
                invalidParameterResponse("name", "name is longer than 255 characters")
            }
        }

        putP("/api/artists/{id}", PermissionType.MANAGE_ARTISTS) {
            val id = call.getLongParameter("id")
            val data = call.receive<ArtistEditData>()

            data.validate()

            try {
                val updatedArtist = ArtistRepo.updateOrCreateArtist(id, data)
                call.respond(ArtistRepo.toDto(updatedArtist))
            }
            catch (_: NotFoundException) {
                notFound()
            }
        }.describe {
            operationId = "updateArtist"
            summary = "Update an artist"
            tag("Artists")
            parameters {
                p("id", 1L, description = "The artist's Id")
            }
            requestBody {
                description = "The artist edit data. Null values will not be overridden."
                schema = jsonSchema<ArtistEditData>()
            }
            authResponses(PermissionType.MANAGE_ARTISTS) {
                successResponse<ArtistDto>("The updated artist")
                notFoundResponse("Artist")
                invalidParameterResponse("name", "name is longer than 255 characters")
            }
        }

        deleteP("/api/artists/{id}", PermissionType.DELETE_ARTISTS) {
            val id = call.getLongParameter("id")
            val success = ArtistRepo.deleteArtist(id)

            if (!success) {
                notFound()
            }

            call.success()
        }.describe {
            operationId = "deleteArtist"
            summary = "Delete an artist"
            tag("Artists")
            parameters {
                p("id", 1L, description = "The artist's Id")
            }
            authResponses(PermissionType.DELETE_ARTISTS) {
                successResponse<SuccessDto>("Success")
                notFoundResponse("Artist")
            }
        }

        getP("/api/artists/{artistId}/image", PermissionType.VIEW_ARTISTS) {
            val artistId = call.getLongParameter("artistId")
            val quality = call.getIntOrDefault("quality", 0)
            val imageId = ArtistRepo.getImageId(artistId)

            call.respondRedirect("/api/images/${if (imageId != null) "by-id/${imageId}" else "default" }?type=artist&quality=$quality")
        }.describe {
            operationId = "getArtistImage"
            summary = "Get an artist's image"
            description = "Redirects to the image of an artist, or the default placeholder if the artist does not have a image"
            tag("Artists")
            parameters {
                p("artistId", 1L)
            }
            authResponses(PermissionType.VIEW_ARTISTS) {
                redirectResponse("`/api/images/by-id/{imageId}` or `/api/images/default`")
                notFoundResponse("Artist")
            }
        }

        getP("/api/artists/autocomplete", PermissionType.VIEW_ARTISTS) {
            val query = call.getStringParameter("query")
            val limit = call.getIntOrDefault("limit", 10)

            val artistNames = ArtistRepo.autocomplete(query, limit)
            call.respond(artistNames)
        }.describe {
            operationId = "getArtistAutocomplete"
            summary = "Get artist name autocompletions"
            tag("Artists")
            parameters {
                q<String>("query", "Ric", required = true)
                q("limit", 10, required = false, description = "Maximum number of completions to return.")
            }
            authResponses(PermissionType.VIEW_ALBUMS) {
                successResponse<List<String>>("Autocompleted names",
                    exampleValue = listOf("Rick Astley", "Richaaarrrd"))
            }
        }
    }
}