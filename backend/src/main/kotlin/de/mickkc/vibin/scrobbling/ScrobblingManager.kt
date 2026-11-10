package de.mickkc.vibin.scrobbling

import de.mickkc.vibin.db.EnabledScrobblerTable
import de.mickkc.vibin.db.tracks.TrackVisibility
import de.mickkc.vibin.dto.scrobbling.ScrobblerDto
import de.mickkc.vibin.repos.TrackRepo
import de.mickkc.vibin.scrobbling.lastfm.LastFmScrobbler
import de.mickkc.vibin.scrobbling.listenbrainz.ListenBrainzScrobbler
import de.mickkc.vibin.utils.DateTimeUtils
import de.mickkc.vibin.utils.http.UserAgentBuilder
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.gson.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

object ScrobblingManager {

    private data class SetCurrentTrackRequest(
        var trackId: Long,
        var timestamp: Long
    )

    private val logger = LoggerFactory.getLogger(ScrobblingManager::class.java)

    const val SET_CURRENT_DEBOUNCE_SECONDS = 5L

    private val setCurrentTrackRequestMap = ConcurrentHashMap<Long, SetCurrentTrackRequest>()

    private val scope = CoroutineScope(
SupervisorJob()
        + Dispatchers.IO
    )

    private val client = HttpClient(CIO) {
        UserAgentBuilder.configureUserAgent(this, "Scrobbling")
        install(ContentNegotiation) {
            gson {  }
        }
    }

    val scrobblers = listOf(
        LastFmScrobbler(client),
        ListenBrainzScrobbler(client)
    )

    fun getScrobblerById(id: String): Scrobbler? = scrobblers.firstOrNull { it.id == id }

    fun getEnabledScrobblers(userId: Long): List<Scrobbler> = transaction {
        val enabled = EnabledScrobblerTable
            .select(EnabledScrobblerTable.scrobblerId)
            .where { EnabledScrobblerTable.userId eq userId }
            .toList()
            .map { it[EnabledScrobblerTable.scrobblerId] }
        scrobblers.filter { it.id in enabled && it.isAvailable(userId) }
    }

    fun isScrobblerEnabled(userId: Long, scrobblerId: String): Boolean = transaction {
        EnabledScrobblerTable
            .select(EnabledScrobblerTable.scrobblerId)
            .where { (EnabledScrobblerTable.userId eq userId) and (EnabledScrobblerTable.scrobblerId eq scrobblerId) }
            .any()
    }

    fun setScrobblerEnabled(userId: Long, scrobblerId: String, enabled: Boolean) = transaction {
        val wasEnabled = isScrobblerEnabled(userId, scrobblerId)
        if (!enabled && wasEnabled) {
            EnabledScrobblerTable.deleteWhere {
                (EnabledScrobblerTable.userId eq userId) and
                (EnabledScrobblerTable.scrobblerId eq scrobblerId)
            }
        }
        else if (enabled && !wasEnabled) {
            EnabledScrobblerTable.insert {
                it[EnabledScrobblerTable.userId] = userId
                it[EnabledScrobblerTable.scrobblerId] = scrobblerId
            }
        }
    }

    fun scrobble(userId: Long, trackId: Long, duration: Long, start: Long) {
        val track = TrackRepo.getById(trackId) ?: return
        if (duration <= 0) return
        if (track.visibility != TrackVisibility.Public) return

        scope.launch {
            getEnabledScrobblers(userId).forEach { scrobbler ->
                try {
                    if (scrobbler.isAvailable(userId))
                        scrobbler.scrobble(userId, track, duration, start)
                    else
                        logger.warn("Scrobbler ${scrobbler.name} is no longer available for user $userId. Skipping scrobble...")
                }
                catch (e: Exception) {
                    logger.error("Error scrobbling to ${scrobbler.name}.", e)
                }
            }
        }
    }

    fun setCurrentlyPlaying(userId: Long, trackId: Long) {
        if (!checkAndRecordSetCurrentRequest(userId, trackId))
            return

        val track = TrackRepo.getById(trackId) ?: return
        if (track.visibility != TrackVisibility.Public) return

        scope.launch {
            getEnabledScrobblers(userId).forEach { scrobbler ->
                try {
                    if (scrobbler.isAvailable(userId))
                        scrobbler.setNowPlaying(userId, track)
                    else
                        logger.warn("Scrobbler ${scrobbler.name} is no longer available for user $userId. Skip setting the currently playing track...")
                }
                catch (e: Exception) {
                    logger.error("Error setting the current track on ${scrobbler.name}.", e)
                }
            }
        }
    }

    /**
     * Checks whether a set-currently-playing request for the same user and track was recently sent, and records this
     * request if not.
     *
     * @return true if the request should be sent, false if it was debounced
     */
    internal fun checkAndRecordSetCurrentRequest(userId: Long, trackId: Long, now: Long = DateTimeUtils.now()): Boolean {
        val existing = setCurrentTrackRequestMap[userId]
        if (existing != null && existing.trackId == trackId && now - existing.timestamp < SET_CURRENT_DEBOUNCE_SECONDS) {
            return false
        }
        setCurrentTrackRequestMap[userId] = SetCurrentTrackRequest(trackId, now)
        return true
    }

    internal fun clearSetCurrentTrackRequests() = setCurrentTrackRequestMap.clear()

    fun toDto(scrobbler: Scrobbler, userId: Long?): ScrobblerDto {
        return ScrobblerDto(
            id = scrobbler.id,
            name = scrobbler.name,
            available = userId?.let { scrobbler.isAvailable(it) } ?: true,
            status = userId?.let { scrobbler.getStatus(it) }
        )
    }
}