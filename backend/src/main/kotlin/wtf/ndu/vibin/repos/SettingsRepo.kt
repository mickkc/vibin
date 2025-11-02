package wtf.ndu.vibin.repos

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction
import wtf.ndu.vibin.db.SettingsEntity
import wtf.ndu.vibin.db.SettingsTable
import wtf.ndu.vibin.settings.Setting
import wtf.ndu.vibin.settings.serverSettings

/**
 * Repository for managing [wtf.ndu.vibin.db.SettingsEntity] instances.
 */
object SettingsRepo {

    /**
     * Retrieves a settings value by its key.
     *
     * @param key The key of the setting to retrieve.
     * @return The value of the setting if found, otherwise null.
     */
    fun getSetting(key: String): String?  = transaction {
        SettingsEntity.find { SettingsTable.key eq key }.firstOrNull()?.value
    }

    fun getAllValues(keys: List<Setting<out Any>>): Map<String, @Serializable @Contextual Any> = transaction {
        keys.associate { setting ->
            setting.key to (SettingsEntity.find { SettingsTable.key eq setting.key }.firstOrNull()?.value?.let { setting.parser(it) }
                ?: setting.defaultValue)
        }
    }

    fun getServerSetting(key: String): Setting<out Any>? = serverSettings.firstOrNull { it.key == key }

    fun updateSetting(setting: Setting<*>, value: String) = transaction {
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
}