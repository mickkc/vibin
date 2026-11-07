package de.mickkc.vibin.scrobbling.lastfm

import de.mickkc.vibin.auth.CryptoUtil
import de.mickkc.vibin.db.tracks.TrackEntity
import de.mickkc.vibin.repos.SettingsRepo
import de.mickkc.vibin.repos.TrackRepo
import de.mickkc.vibin.scrobbling.Scrobbler
import de.mickkc.vibin.scrobbling.ScrobblerStatus
import de.mickkc.vibin.scrobbling.lastfm.settings.LastFmUserCredentialsPassword
import de.mickkc.vibin.scrobbling.lastfm.settings.LastFmUserCredentialsUsername
import de.mickkc.vibin.scrobbling.lastfm.settings.LastFmUserSessionKey
import de.mickkc.vibin.settings.Setting
import de.mickkc.vibin.settings.Settings
import de.mickkc.vibin.settings.user.LastFmUserApiKey
import de.mickkc.vibin.settings.user.LastFmUserSharedSecret
import de.mickkc.vibin.utils.http.RateLimitedClient
import de.mickkc.vibin.utils.http.RatelimitConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.slf4j.LoggerFactory

class LastFmScrobbler(client: HttpClient) : Scrobbler(client) {

    private val rateLimiter = RateLimitedClient(
        RatelimitConfig(5, 1000, 200)
    )

    private val logger = LoggerFactory.getLogger(LastFmScrobbler::class.java)
    private val baseUrl = "https://ws.audioscrobbler.com/2.0/"

    override val id: String
        get() = "last_fm"

    override val name: String
        get() = "Last.fm"

    override val settings: List<Setting<*>> = listOf(
        LastFmUserApiKey,
        LastFmUserSharedSecret,
        LastFmUserSessionKey,
        LastFmUserCredentialsUsername,
        LastFmUserCredentialsPassword
    )

    /**
     * Gets the signature for a list of parameters.
     *
     * @see <a href="https://www.last.fm/api/webauth#_6-sign-your-calls">Last.fm docs</a>.
     * @param userId The id of the user executing the request. Used to load the [LastFmUserSharedSecret].
     * @param params A map of parameters included in the signature.
     * @return The signature, hashed using MD5.
     */
    private fun sign(userId: Long, params: Map<String, String>): String {
        val secret = Settings.get(LastFmUserSharedSecret, userId)

        val raw = params
            .filterKeys { it != "format" && it != "callback" }
            .toSortedMap()
            .entries.joinToString("") { (k, v) -> k + v } + secret

        return CryptoUtil.hashMd5(raw)
    }

    /**
     * Gets a cached session key or creates a new one using `getMobileSession`.
     *
     * @see <a href="https://www.last.fm/api/show/auth.getMobileSession">auth.getMobileSession</a>
     * @see <a href="https://www.last.fm/api/mobileauth">Mobile auth</a>.
     * @param userId The id of the user executing the request. Used to load [LastFmUserCredentialsUsername] and
     *               [LastFmUserCredentialsPassword].
     * @return The session key or null if an error occurred (logged).
     */
    private suspend fun getSessionKey(userId: Long): String? {
        val username = Settings.get(LastFmUserCredentialsUsername, userId)
        val password = Settings.get(LastFmUserCredentialsPassword, userId)

        val credentialsHash = CryptoUtil.hashMd5(username + password)

        val cachedKey = Settings.get(LastFmUserSessionKey, userId).takeIf { it.isNotBlank() }
        if (cachedKey != null && cachedKey.startsWith("$credentialsHash:"))
            return cachedKey.substringAfter(":")

        return call<GetMobileSessionResponse>(userId, buildMap {
            put("method", "auth.getMobileSession")
            put("username", username)
            put("password", password)
        })?.session?.key?.also {
            SettingsRepo.updateUserSetting(LastFmUserSessionKey, userId, "$credentialsHash:$it")
        }
    }

    /**
     * Calls a Last.fm method, adding the [LastFmUserApiKey] and the signature to the request before sending it.
     *
     * @param userId The id of the user executing the request. Used to load [LastFmUserApiKey].
     * @param params A map of parameters included in the signature and sent to the client. Must include a `method` field.
     * @return The serialized result or null of an error ocurred (logged).
     */
    private suspend inline fun<reified T> call(userId: Long, params: Map<String, String>): T? {
        val apiKey = Settings.get(LastFmUserApiKey, userId)
        val paramsWithKey = params + ("api_key" to apiKey)
        val signed = paramsWithKey + ("api_sig" to sign(userId, paramsWithKey)) + ("format" to "json")

        val response = rateLimiter.waitAndRun {
            client.submitForm(baseUrl, parameters {
                signed.forEach { (k,v) -> append(k, v) }
            })
        }

        if (!response.status.isSuccess()) {
            if (response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden) {
                SettingsRepo.clearUserSetting(LastFmUserSessionKey, userId)
            }

            val message = "An error occurred while requesting method ${params["method"]} from Last.fm. " +
                    "${response.status.value} - ${response.status.description}:\n${response.bodyAsText()}"
            logger.error(message)
            setStatus(userId, ScrobblerStatus.error(message))
            return null
        }

        return response.body<T>().also {
            setStatus(userId, ScrobblerStatus.success())
        }
    }

    override suspend fun scrobble(userId: Long, track: TrackEntity, duration: Long, timestamp: Long) {
        // See https://www.last.fm/api/show/track.scrobble
        if (duration < 240 && track.duration != null && track.duration!! / 1000 / 2 > duration) {
            logger.info("Scrobble of $duration seconds is too short for Last.fm.")
            return
        }

        val sessionKey = getSessionKey(userId) ?: return
        call<Any>(userId, buildMap {
            put("method", "track.scrobble")
            put("sk", sessionKey)
            put("artist", TrackRepo.getArtists(track).firstOrNull()?.name ?: "")
            put("track", track.title)
            put("timestamp", timestamp.toString())
            put("album", TrackRepo.getAlbum(track).title)
            track.duration?.let {
                put("duration", (it / 1000).toString())
            }
        })
    }

    override suspend fun setNowPlaying(userId: Long, track: TrackEntity) {
        // See https://www.last.fm/api/show/track.updateNowPlaying
        val sessionKey = getSessionKey(userId) ?: return
        call<Any>(userId, buildMap {
            put("method", "track.updateNowPlaying")
            put("sk", sessionKey)
            put("artist", TrackRepo.getArtists(track).firstOrNull()?.name ?: "")
            put("track", track.title)
            put("album", TrackRepo.getAlbum(track).title)
            track.duration?.let {
                put("duration", (it / 1000).toString())
            }
        })
    }
}