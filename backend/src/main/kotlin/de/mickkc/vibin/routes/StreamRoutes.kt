package de.mickkc.vibin.routes

import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.TrackRepo
import de.mickkc.vibin.streaming.StreamBuilder
import de.mickkc.vibin.utils.AccessCryptoUtil
import de.mickkc.vibin.utils.PathUtils
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureStreamRoutes() = routing {

    authenticate("tokenAuth") {
        getP("/api/streams", PermissionType.STREAM_TRACKS) {
            val ids = call.getStringParameter("ids") ?: return@getP
            val idList = ids.split(",")
            val trackList = idList.mapNotNull { it.toLongOrNull()?.let { id -> TrackRepo.getById(id) } }

            if (idList.isEmpty() || trackList.size != idList.size) {
                return@getP call.invalidParameter("ids")
            }

            val streams = trackList.map { track -> StreamBuilder.build(track) }
            call.respond(streams)
        }
    }

    get("/api/streams/{trackId}") {
        val trackId = call.getLongParameter("trackId") ?: return@get
        val trackSig = call.getStringParameter("sig") ?: return@get
        val trackExp = call.getLongParameter("exp") ?: return@get

        val track = TrackRepo.getById(trackId) ?: return@get call.notFound()
        if (!AccessCryptoUtil.validateSignature(track.checksum, trackExp, trackSig)) {
            return@get call.unauthorized("Invalid or expired track signature")
        }

        //TODO: Add transcoding support
        val file = PathUtils.getTrackFileFromPath(track.path)
        if (!file.exists()) {
            return@get call.notFound()
        }

        call.respondFile(file)
    }

}