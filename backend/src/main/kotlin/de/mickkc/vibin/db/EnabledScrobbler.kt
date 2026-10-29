package de.mickkc.vibin.db

import org.jetbrains.exposed.dao.id.LongIdTable

object EnabledScrobblerTable: LongIdTable("enabled_scrobbler") {
    val userId = reference("user_id", UserTable)
    val scrobblerId = varchar("scrobbler_id", 255)
}