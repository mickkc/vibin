package de.mickkc.vibin.routes

import de.mickkc.vibin.dto.KeyValueDto
import de.mickkc.vibin.dto.PaginatedDto
import de.mickkc.vibin.dto.albums.AlbumEditDto
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.AlbumRepo
import de.mickkc.vibin.repos.TrackRepo
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureAlbumRoutes() = routing {

    authenticate("tokenAuth") {

        getP("/api/albums", PermissionType.VIEW_ALBUMS) {

            val params = call.getPaginatedSearchParams()
            val showSingles = call.getBooleanOrDefault("showSingles", true)
            val userId = call.getUserId() ?: unauthorized()

            val (albums, total) = AlbumRepo.getAll(params, showSingles = showSingles, userId)

            call.respond(PaginatedDto(
                items = AlbumRepo.toDto(albums),
                total = total.toInt(),
                pageSize = params.pageSize,
                currentPage = params.page
            ))
        }

        postP("/api/albums", PermissionType.MANAGE_ALBUMS) {
            val createDto = call.receive<AlbumEditDto>()
            createDto.validate()

            if (createDto.title == null) {
                missingParameter("title")
            }

            val newAlbum = AlbumRepo.create(createDto)
            call.respond(AlbumRepo.toDto(newAlbum))
        }

        getP("/api/albums/autocomplete", PermissionType.VIEW_ALBUMS) {

            val query = call.getStringParameter("query")
            val limit = call.getIntOrDefault("limit", 10)

            val albumNames = AlbumRepo.autocomplete(query, limit)

            call.respond(albumNames)
        }

        getP("/api/albums/{albumId}", PermissionType.VIEW_ALBUMS) {

            val albumId = call.getLongParameter("albumId")
            val album = AlbumRepo.getById(albumId) ?: notFound()
            val userId = call.getUserId() ?: unauthorized()

            call.respond(AlbumRepo.toDataDto(album, userId))
        }

        deleteP("/api/albums/{albumId}", PermissionType.DELETE_ALBUMS) {
            val albumId = call.getLongParameter("albumId")
            val deleted = AlbumRepo.delete(albumId)

            if (!deleted) {
                notFound()
            }

            call.success()
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
        }

        putP("/api/albums/{albumId}", PermissionType.MANAGE_ALBUMS) {

            val albumId = call.getLongParameter("albumId")
            val editDto = call.receive<AlbumEditDto>()

            editDto.validate()

            val updated = AlbumRepo.update(albumId, editDto) ?: notFound()
            call.respond(AlbumRepo.toDto(updated))
        }

        getP("/api/albums/{albumId}/cover", PermissionType.VIEW_ALBUMS) {
            val albumId = call.getLongParameter("albumId")
            val album = AlbumRepo.getById(albumId) ?: notFound()
            val quality = call.getIntOrDefault("quality", 0)
            val imageId = AlbumRepo.getAlbumCover(album)?.id?.value

            call.respondRedirect("/api/images/${if (imageId != null) "by-id/${imageId}" else "default" }?type=album&quality=$quality")
        }
    }
}