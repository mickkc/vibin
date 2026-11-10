package de.mickkc.vibin.repos

import de.mickkc.vibin.db.SettingsEntity
import de.mickkc.vibin.db.SettingsTable
import de.mickkc.vibin.db.UserSettingsEntity
import de.mickkc.vibin.db.UserSettingsTable
import de.mickkc.vibin.settings.SettingsCrypto
import de.mickkc.vibin.settings.server.ServerSetting
import de.mickkc.vibin.settings.server.serverSettings
import de.mickkc.vibin.settings.user.UserSetting
import de.mickkc.vibin.settings.user.userSettings
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Repository for managing [de.mickkc.vibin.db.SettingsEntity] instances.
 */
object SettingsRepo {

    /**
     * Retrieves a settings value by its key.
     *
     * @param key The key of the setting to retrieve.
     * @return The value of the setting if found, otherwise null.
     */
    fun getServerSettingValue(key: String): String?  = transaction {
        val value = SettingsEntity.find { SettingsTable.key eq key }.firstOrNull()?.value
        return@transaction value?.let { if (SettingsCrypto.isEncrypted(it)) SettingsCrypto.decrypt(it) else it }
    }

    /**
     * Retrieves a user-specific settings value by its key and user ID.
     *
     * @param key The key of the setting to retrieve.
     * @param userId The ID of the user.
     * @return The value of the setting if found, otherwise null.
     */
    fun getUserSettingValue(key: String, userId: Long): String?  = transaction {
        val value = UserSettingsEntity.find {
            (UserSettingsTable.key eq key) and (UserSettingsTable.userId eq userId)
        }.firstOrNull()?.value
        return@transaction value?.let { if (SettingsCrypto.isEncrypted(it)) SettingsCrypto.decrypt(it) else it }
    }

    fun getServerSetting(key: String): ServerSetting<out Any>? = serverSettings.firstOrNull { it.key == key }
    fun getUserSetting(key: String): UserSetting<out Any>? = userSettings.firstOrNull { it.key == key }

    fun updateServerSetting(setting: ServerSetting<*>, value: String) = transaction {
        val stored = if (setting.secret) SettingsCrypto.encrypt(value) else value
        val settingEntity = SettingsEntity.find { SettingsTable.key eq setting.key }.firstOrNull()
        if (settingEntity != null) {
            settingEntity.value = stored
        } else {
            SettingsEntity.new {
                this.key = setting.key
                this.value = stored
            }
        }
    }

    fun updateUserSetting(setting: UserSetting<*>, userId: Long, value: String) = transaction {
        val stored = if (setting.secret) SettingsCrypto.encrypt(value) else value
        val settingEntity = UserSettingsEntity.find {
            (UserSettingsTable.key eq setting.key) and (UserSettingsTable.userId eq userId)
        }.firstOrNull()
        if (settingEntity != null) {
            settingEntity.value = stored
        } else {
            UserSettingsEntity.new {
                this.key = setting.key
                this.userId = userId
                this.value = stored
            }
        }
    }

    fun clearUserSetting(setting: UserSetting<*>, userId: Long) = transaction {
        UserSettingsTable.deleteWhere { (UserSettingsTable.key eq setting.key) and (UserSettingsTable.userId eq userId) }
    }
}