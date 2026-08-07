package de.mickkc.vibin.db.security

import de.mickkc.vibin.db.StringIdTable
import de.mickkc.vibin.db.UserTable
import de.mickkc.vibin.utils.DateTimeUtils

object TrustedDeviceIdTable : StringIdTable("trusted_device_id", columnName = "device_id", columnLength = 32) {
    val userId = reference("user_id", UserTable)
    val trustedAt = long("trusted_at").clientDefault { DateTimeUtils.now() }
}