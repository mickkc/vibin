package de.mickkc.vibin.db.security

import de.mickkc.vibin.db.UserTable
import de.mickkc.vibin.utils.DateTimeUtils
import org.jetbrains.exposed.dao.id.LongIdTable

object TrustedDeviceIdTable : LongIdTable("trusted_device") {
    val deviceId = varchar("device_id", 32)
    val userId = reference("user_id", UserTable)
    val trustedAt = long("trusted_at").clientDefault { DateTimeUtils.now() }
}