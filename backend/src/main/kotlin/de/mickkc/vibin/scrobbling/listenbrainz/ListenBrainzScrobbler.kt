package de.mickkc.vibin.scrobbling.listenbrainz

import de.mickkc.vibin.db.tracks.TrackEntity
import de.mickkc.vibin.repos.TrackRepo
import de.mickkc.vibin.scrobbling.Scrobbler
import de.mickkc.vibin.scrobbling.ScrobblerStatus
import de.mickkc.vibin.scrobbling.listenbrainz.data.ListenBrainzAdditionalInfo
import de.mickkc.vibin.scrobbling.listenbrainz.data.ListenBrainzPayload
import de.mickkc.vibin.scrobbling.listenbrainz.data.ListenBrainzRequest
import de.mickkc.vibin.scrobbling.listenbrainz.data.ListenBrainzTrackMetadata
import de.mickkc.vibin.scrobbling.listenbrainz.settings.ListenBrainzToken
import de.mickkc.vibin.settings.Setting
import de.mickkc.vibin.settings.Settings
import de.mickkc.vibin.utils.http.RateLimitedClient
import de.mickkc.vibin.utils.http.RatelimitConfig
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.slf4j.LoggerFactory

class ListenBrainzScrobbler(client: HttpClient) : Scrobbler(client) {

    private val rateLimiter = RateLimitedClient(
        RatelimitConfig(10, 5000, 1000)
    )

    private val logger = LoggerFactory.getLogger(ListenBrainzScrobbler::class.java)

    override val id: String
        get() = "listenbrainz"

    override val name: String
        get() = "ListenBrainz"

    override val settings: List<Setting<*>>
        get() = listOf(ListenBrainzToken)

    override suspend fun scrobble(userId: Long, track: TrackEntity, duration: Long, timestamp: Long) {

        // "Listens should be submitted for tracks when the user has listened to half the track or 4 minutes of the
        // track, whichever is lower."
        // https://listenbrainz.readthedocs.io/en/latest/users/api/core.html#post--1-submit-listens
        if (duration < 240 && track.duration != null && track.duration!! / 1000 / 2 > duration) {
            logger.info("Scrobble of $duration seconds is too short for ListenBrainz.")
            return
        }

        val token = Settings.get(ListenBrainzToken, userId).takeIf { it.isNotEmpty() } ?: return

        val response = rateLimiter.waitAndRun {
            client.post("https://api.listenbrainz.org/1/submit-listens") {
                headers[HttpHeaders.Authorization] = "Token $token"
                headers[HttpHeaders.ContentType] = ContentType.Application.Json.toString()
                setBody(ListenBrainzRequest(
                    listen_type = "single",
                    payload = listOf(
                        ListenBrainzPayload(
                            listened_at = timestamp,
                            track_metadata = ListenBrainzTrackMetadata(
                                artist_name = TrackRepo.getArtists(track).firstOrNull()?.name ?: "",
                                track_name = track.title,
                                release_name = TrackRepo.getAlbum(track).title,
                                additional_info = ListenBrainzAdditionalInfo(
                                    duration_ms = track.duration,
                                    duration_played = duration,
                                    music_service_name = "Vibin'"
                                )
                            )
                        )
                    )
                ))
            }
        }

        if (!response.status.isSuccess()) {
            val message = "An error occurred while scrobbling to ListenBrainz. " +
                    "${response.status.value} - ${response.status.description}:\n${response.bodyAsText()}"
            logger.error(message)
            setStatus(userId, ScrobblerStatus.error(message))
        }
        else {
            setStatus(userId, ScrobblerStatus.success())
        }
    }

    override suspend fun setNowPlaying(userId: Long, track: TrackEntity) {
        val token = Settings.get(ListenBrainzToken, userId).takeIf { it.isNotEmpty() } ?: return

        val response = rateLimiter.waitAndRun {
            client.post("https://api.listenbrainz.org/1/submit-listens") {
                headers[HttpHeaders.Authorization] = "Token $token"
                headers[HttpHeaders.ContentType] = ContentType.Application.Json.toString()
                setBody(ListenBrainzRequest(
                    listen_type = "playing_now",
                    payload = listOf(
                        ListenBrainzPayload(
                            listened_at = null,
                            track_metadata = ListenBrainzTrackMetadata(
                                artist_name = TrackRepo.getArtists(track).firstOrNull()?.name ?: "",
                                track_name = track.title,
                                release_name = TrackRepo.getAlbum(track).title,
                                additional_info = ListenBrainzAdditionalInfo(
                                    duration_ms = track.duration,
                                    duration_played = null,
                                    music_service_name = "Vibin'"
                                )
                            )
                        )
                    )
                ))
            }
        }

        if (!response.status.isSuccess()) {
            val message = "An error occurred while updating the currently playing track on ListenBrainz. " +
                    "${response.status.value} - ${response.status.description}:\n${response.bodyAsText()}"
            logger.error(message)
            setStatus(userId, ScrobblerStatus.error(message))
        }
        else {
            setStatus(userId, ScrobblerStatus.success())
        }
    }
}