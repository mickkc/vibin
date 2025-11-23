package de.mickkc.vibin.repos

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import de.mickkc.vibin.db.SettingsEntity
import de.mickkc.vibin.db.SettingsTable
import de.mickkc.vibin.db.UserSettingsEntity
import de.mickkc.vibin.db.UserSettingsTable
import de.mickkc.vibin.settings.server.ServerSetting
import de.mickkc.vibin.settings.server.serverSettings
import de.mickkc.vibin.settings.user.UserSetting
import de.mickkc.vibin.settings.user.userSettings

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
        SettingsEntity.find { SettingsTable.key eq key }.firstOrNull()?.value
    }

    /**
     * Retrieves a user-specific settings value by its key and user ID.
     *
     * @param key The key of the setting to retrieve.
     * @param userId The ID of the user.
     * @return The value of the setting if found, otherwise null.
     */
    fun getUserSettingValue(key: String, userId: Long): String?  = transaction {
        UserSettingsEntity.find {
            (UserSettingsTable.key eq key) and (UserSettingsTable.userId eq userId)
        }.firstOrNull()?.value
    }

    fun getAllValues(keys: List<ServerSetting<out Any>>): Map<String, @Serializable @Contextual Any> = transaction {
        keys.associate { setting ->
            setting.key to (SettingsEntity.find { SettingsTable.key eq setting.key }.firstOrNull()?.value?.let { setting.parser(it) }
                ?: setting.defaultValue)
        }
    }

    fun getAllValues(userId: Long, keys: List<UserSetting<out Any>>): Map<String, @Serializable @Contextual Any> = transaction {
        keys.associate { setting ->
            setting.key to (UserSettingsEntity.find {
                (UserSettingsTable.key eq setting.key) and (UserSettingsTable.userId eq userId)
            }.firstOrNull()?.value?.let { setting.parser(it) }
                ?: setting.defaultValue)
        }
    }

    fun getServerSetting(key: String): ServerSetting<out Any>? = serverSettings.firstOrNull { it.key == key }
    fun getUserSetting(key: String): UserSetting<out Any>? = userSettings.firstOrNull { it.key == key }

    fun updateServerSetting(setting: ServerSetting<*>, value: String) = transaction {
        val settingEntity = SettingsEntity.find { SettingsTable.key eq setting.key }.firstOrNull()
        if (settingEntity != null) {
            settingEntity.value = value
        } else {
            SettingsEntity.new {
                this.key = setting.key
                this.value = value
            }
        }
    }

    fun updateUserSetting(setting: UserSetting<*>, userId: Long, value: String) = transaction {
        val settingEntity = UserSettingsEntity.find {
            (UserSettingsTable.key eq setting.key) and (UserSettingsTable.userId eq userId)
        }.firstOrNull()
        if (settingEntity != null) {
            settingEntity.value = value
        } else {
            UserSettingsEntity.new {
                this.key = setting.key
                this.userId = userId
                this.value = value
            }
        }
    }
}