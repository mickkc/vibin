package de.mickkc.vibin.routes

import de.mickkc.vibin.dto.Streamable
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.TrackRepo
import de.mickkc.vibin.streaming.StreamBuilder
import de.mickkc.vibin.utils.AccessCryptoUtil
import de.mickkc.vibin.utils.PathUtils
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.ExperimentalKtorApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalKtorApi::class)
fun Application.configureStreamRoutes() = routing {

    authenticate("tokenAuth") {
        getP("/api/streams", PermissionType.STREAM_TRACKS) {
            val ids = call.getStringParameter("ids")
            val idList = ids.split(",")
            val trackList = idList.mapNotNull { it.toLongOrNull()?.let { id -> TrackRepo.getById(id) } }

            if (idList.isEmpty() || trackList.size != idList.size) {
                invalidParameter("ids")
            }

            val streams = trackList.map { track -> StreamBuilder.build(track) }
            call.respond(streams)
        }.describe {
            operationId = "getStreamables"
            summary = "Get streamables for multiple tracks"
            tag("Streaming")
            parameters {
                q("ids", "1,2,3", required = true, description = "Comma-separated list of track Ids")
            }
            authResponses(PermissionType.STREAM_TRACKS) {
                successResponse<List<Streamable>>("List of streamables")
            }
        }
    }

    get("/api/streams/{trackId}") {
        val trackId = call.getLongParameter("trackId")
        val trackSig = call.getStringParameter("sig")
        val trackExp = call.getLongParameter("exp")

        val track = TrackRepo.getById(trackId) ?: notFound()
        if (!AccessCryptoUtil.validateSignature(track.checksum, trackExp, trackSig)) {
            unauthorized("Invalid or expired track signature")
        }

        val format = call.getStringOrDefault("format", "mp3").trim().lowercase()
        val bitrate = call.getIntOrDefault("bitrate", 128)
        val start = call.getLongOrDefault("start", 0L)

        val file = PathUtils.getTrackFileFromPath(track.path)
        if (!file.exists()) {
            notFound()
        }

        if (format == "original") {
            call.response.header(HttpHeaders.CacheControl, "no-cache, no-store, must-revalidate")
            return@get call.respondFile(file)
        }

        val ffmpegArgs = when(format) {
            "mp3" -> listOf("-hide_banner", "-loglevel", "error", "-ss", start.toString(), "-i", file.absolutePath, "-vn", "-c:a", "libmp3lame", "-b:a", "${bitrate}k", "-f", "mp3", "-")
            "opus" -> listOf("-hide_banner", "-loglevel", "error", "-ss", start.toString(), "-i", file.absolutePath, "-vn", "-c:a", "libopus", "-b:a", "${bitrate}k", "-f", "webm", "-")
            "aac" -> listOf("-hide_banner", "-loglevel", "error", "-ss", start.toString(), "-i", file.absolutePath, "-vn", "-c:a", "aac", "-b:a", "${bitrate}k", "-f", "adts", "-")
            else -> invalidParameter("format", "mp3", "opus", "aac")
        }

        val contentType = when(format) {
            "mp3" -> ContentType.parse("audio/mpeg")
            "opus" -> ContentType.parse("audio/webm")
            "aac" -> ContentType.parse("audio/aac")
            else -> ContentType.Audio.Any
        }

        log.info("Starting stream for track ID $trackId in format $format at bitrate ${bitrate}kbps from position $start seconds")

        val processBuilder = ProcessBuilder(listOf("ffmpeg") + ffmpegArgs)
            .redirectErrorStream(true)
        val process = processBuilder.start()

        call.response.header(HttpHeaders.CacheControl, "no-cache, no-store, must-revalidate")

        try {
            call.respondOutputStream(contentType) {
                withContext(Dispatchers.IO) {
                    process.inputStream.use { inStream ->
                        inStream.copyTo(this@respondOutputStream)
                        flush()
                    }
                }
                process.waitFor(5, TimeUnit.SECONDS)
            }
        }
        finally {
            if (process.isAlive) {
                process.destroyForcibly()
            }
        }

        call.respondFile(file)
    }.describe {
        operationId = "streamTrack"
        summary = "Stream a track"
        tag("Streaming")
        parameters {
            q("trackId", "1,2,3", required = true, description = "Comma-separated list of track Ids")
            q("sig", "2WLmw57JNDQXrQhIR7s3DfrtWY=", required = true, description = "Track signature")
            q("exp", "1782511260", required = true, description = "Expiration timestamp")
            q("format", "mp3", required = true, description = "Transcoding format: `mp3`, `opus`, `aac`, `original` (no transcoding)")
            q("bitrate", 320, required = false, description = "Transcoding bitrate")
            q("start", 0, required = false, description = "Transcoding start offset in seconds")
        }
        authResponses(PermissionType.STREAM_TRACKS) {
            successResponse<ByteArray>("Audio data")
            notFoundResponse("Track")
            unauthorizedResponse("Invalid or expired track signature")
        }
    }

}