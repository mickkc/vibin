package de.mickkc.vibin.scrobbling

import de.mickkc.vibin.db.EnabledScrobblerTable
import de.mickkc.vibin.dto.scrobbling.ScrobblerDto
import de.mickkc.vibin.repos.TrackRepo
import de.mickkc.vibin.scrobbling.lastfm.LastFmScrobbler
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.gson.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

object ScrobblingManager {

    private val logger = LoggerFactory.getLogger(ScrobblingManager::class.java)

    private val scope = CoroutineScope(
SupervisorJob()
        + Dispatchers.IO
    )

    private val client = HttpClient {
        install(ContentNegotiation) {
            gson {  }
        }
    }

    val scrobblers = listOf(
        LastFmScrobbler(client)
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

    fun toDto(scrobbler: Scrobbler, userId: Long?): ScrobblerDto {
        return ScrobblerDto(
            id = scrobbler.id,
            name = scrobbler.name,
            available = userId?.let { scrobbler.isAvailable(it) } ?: true,
            status = userId?.let { scrobbler.getStatus(it) }
        )
    }
}