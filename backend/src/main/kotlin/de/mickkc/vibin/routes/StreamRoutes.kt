package de.mickkc.vibin.routes

import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.TrackRepo
import de.mickkc.vibin.streaming.StreamBuilder
import de.mickkc.vibin.streaming.TeeSession
import de.mickkc.vibin.streaming.TranscodeCache
import de.mickkc.vibin.utils.AccessCryptoUtil
import de.mickkc.vibin.utils.PathUtils
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

/**
 * True for a request with no Range header, or a range request from 0 to end (`bytes=0-`) which is sent when playback
 * begins. If this is true, the output from ffmpeg can be sent back to the client while being written.
 */
private fun isPlainStartRequest(rangeHeader: String?): Boolean {
    if (rangeHeader == null) return true
    if (!rangeHeader.startsWith("bytes=")) return false

    val spec = rangeHeader.removePrefix("bytes=").split(",").first().trim()
    val parts = spec.split("-")
    if (parts.size != 2) return false

    return parts[0].trim() == "0" && parts[1].trim().isEmpty()
}

val allowedBitrates = arrayOf(128, 192, 256, 320, 0)

private val logger = LoggerFactory.getLogger("StreamRoutes")

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

        val format = call.getEnum("format", TranscodeCache.Format.mp3)
        val bitrate = call.getIntOrDefault("bitrate", 0)

        val file = PathUtils.getTrackFileFromPath(track.path)
        if (!file.exists()) {
            notFound()
        }

        call.response.header(HttpHeaders.CacheControl, "no-cache, no-store, must-revalidate")

        // Respond with the original file. (Ranges are handled by Ktor)
        if (format == TranscodeCache.Format.original) {
            return@get call.respondFile(file)
        }

        if (bitrate !in allowedBitrates) {
            invalidParameter("bitrate", "128", "192", "256", "512", "0")
        }

        val contentType = when (format) {
            TranscodeCache.Format.mp3 -> ContentType.parse("audio/mpeg")
            TranscodeCache.Format.opus -> ContentType.parse("audio/webm")
            TranscodeCache.Format.aac -> ContentType.parse("audio/aac")
            else -> ContentType.Audio.Any
        }

        val cachedFile = TranscodeCache.getCachedFile(track, format, bitrate)
        if (cachedFile != null) {
            return@get call.respond(LocalFileContent(cachedFile, contentType))
        }

        val rangeHeader = call.request.headers[HttpHeaders.Range]

        if (!isPlainStartRequest(rangeHeader)) {
            // Client is trying to seek, which can't be responded to while transcoding.
            // So, the file must be transcoded first before the correct range can be read from the cached file.
            logger.info("Serving track ID $trackId in format $format at bitrate ${bitrate}kbps (blocking transcode for range request)")
            val fullFile = withContext(Dispatchers.IO) {
                TranscodeCache.getOrCreate(track, file, format, bitrate)
            }
            // .webm (extension for ogg) would be recognized as video/webm instead of audio/webm, so the content type
            // needs to be set explicitly
            return@get call.respond(LocalFileContent(fullFile, contentType))
        }

        // No cache exists, but the client is playing from the start. This means that the output from ffmpeg can be sent
        // back directly while also writing to the cached file.
        logger.info("Serving track ID $trackId in format $format at bitrate ${bitrate}kbps (streaming while transcoding)")

        // Start transcoding to the cached file
        val session = TranscodeCache.streamAndCache(track, file, format, bitrate)
        call.response.header(HttpHeaders.AcceptRanges, "bytes")

        call.respondOutputStream(contentType) {
            withContext(Dispatchers.IO) {
                // "subscribe" to the transcoding process and redirect to client
                while (true) {
                    val chunk = session.queue.take()
                    if (chunk === TeeSession.EOF) {
                        // End reached, transcoding finished or failed
                        break
                    }
                    write(chunk)
                }
                flush()
            }
        }

        session.failure?.let {
            log.warn("Transcode stream for track $trackId ended with an error after the response had already started", it)
        }
    }

}
