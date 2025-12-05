package de.mickkc.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import de.mickkc.vibin.dto.KeyValueDto
import de.mickkc.vibin.dto.PaginatedDto
import de.mickkc.vibin.dto.albums.AlbumEditDto
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.AlbumRepo
import de.mickkc.vibin.repos.TrackRepo
import de.mickkc.vibin.utils.ImageUtils

fun Application.configureAlbumRoutes() = routing {

    authenticate("tokenAuth") {

        getP("/api/albums", PermissionType.VIEW_ALBUMS) {

            val params = call.getPaginatedSearchParams() ?: return@getP
            val showSingles = call.getBooleanOrDefault("showSingles", true) ?: return@getP
            val userId = call.getUserId() ?: return@getP call.unauthorized()

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

            val newAlbum = AlbumRepo.create(createDto)
            call.respond(AlbumRepo.toDto(newAlbum))
        }

        getP("/api/albums/autocomplete", PermissionType.VIEW_ALBUMS) {

            val query = call.getStringParameter("query") ?: return@getP
            val limit = call.getIntOrDefault("limit", 10) ?: return@getP

            val albumNames = AlbumRepo.autocomplete(query, limit)

            call.respond(albumNames)
        }

        getP("/api/albums/{albumId}", PermissionType.VIEW_ALBUMS) {

            val albumId = call.getLongParameter("albumId") ?: return@getP
            val album = AlbumRepo.getById(albumId) ?: return@getP call.notFound()
            val userId = call.getUserId() ?: return@getP call.unauthorized()

            call.respond(AlbumRepo.toDataDto(album, userId))
        }

        deleteP("/api/albums/{albumId}", PermissionType.DELETE_ALBUMS) {
            val albumId = call.getLongParameter("albumId") ?: return@deleteP
            val deleted = AlbumRepo.delete(albumId)

            if (!deleted) {
                return@deleteP call.notFound()
            }

            call.success()
        }

        getP("/api/albums/artists/{artistId}", PermissionType.VIEW_ALBUMS, PermissionType.VIEW_TRACKS) {

            val artistId = call.getLongParameter("artistId") ?: return@getP
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

            val albumId = call.getLongParameter("albumId") ?: return@putP
            val editDto = call.receive<AlbumEditDto>()

            val updated = AlbumRepo.update(albumId, editDto) ?: return@putP call.notFound()
            call.respond(AlbumRepo.toDto(updated))
        }

        getP("/api/albums/{albumId}/cover", PermissionType.VIEW_ALBUMS) {
            val albumId = call.parameters["albumId"]?.toLongOrNull() ?: return@getP call.missingParameter("albumId")
            val album = AlbumRepo.getById(albumId) ?: return@getP call.notFound()
            val quality = call.request.queryParameters["quality"]?.toIntOrNull() ?: 0
            val cover = AlbumRepo.getAlbumCover(album)

            val file = ImageUtils.getFileOrDefault(cover, quality, "album") ?: return@getP call.notFound()

            call.respondFile(file)
        }
    }
}