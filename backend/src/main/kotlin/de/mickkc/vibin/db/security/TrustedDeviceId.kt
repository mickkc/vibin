package de.mickkc.vibin.db.security

import de.mickkc.vibin.db.UserTable
import de.mickkc.vibin.utils.DateTimeUtils
import org.jetbrains.exposed.dao.id.LongIdTable

object TrustedDeviceIdTable : LongIdTable("trusted_device") {
    val deviceIdHash = binary("device_id_hash", 256).index()
    val userId = reference("user_id", UserTable)
    val deviceName = varchar("device_name", 255)
    val trustedAt = long("trusted_at").clientDefault { DateTimeUtils.now() }

    init {
        uniqueIndex("device_user", deviceIdHash, userId, deviceName)
    }
}