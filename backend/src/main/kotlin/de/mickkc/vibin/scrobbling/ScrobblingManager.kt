package de.mickkc.vibin.scrobbling

import de.mickkc.vibin.db.EnabledScrobblerTable
import de.mickkc.vibin.dto.scrobbling.ScrobblerDto
import de.mickkc.vibin.scrobbling.lastfm.LastFmScrobbler
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.gson.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

object ScrobblingManager {

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

    fun toDto(scrobbler: Scrobbler, userId: Long?): ScrobblerDto {
        return ScrobblerDto(
            id = scrobbler.id,
            name = scrobbler.name,
            available = userId?.let { scrobbler.isAvailable(it) } ?: true
        )
    }
}