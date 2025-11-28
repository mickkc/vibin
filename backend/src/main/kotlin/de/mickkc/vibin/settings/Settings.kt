package de.mickkc.vibin.settings

import de.mickkc.vibin.repos.SettingsRepo
import de.mickkc.vibin.settings.server.ServerSetting
import de.mickkc.vibin.settings.user.UserSetting

object Settings {

    /**
     * Retrieves the value of a setting.
     *
     * @param setting The setting to retrieve.
     * @return The value of the setting, or its default value if not found.
     */
    fun <T>get(setting: Setting<T>): T {
        val stringValue = SettingsRepo.getServerSettingValue(setting.key)
        val parsedValue = stringValue?.let { setting.parser(it) }
        return parsedValue ?: setting.defaultValue
    }

    fun <T>get(userSetting: UserSetting<T>, userId: Long): T {
        val stringValue = SettingsRepo.getUserSettingValue(userSetting.key, userId)
        val parsedValue = stringValue?.let { userSetting.parser(it) }
        return parsedValue ?: userSetting.defaultValue
    }

    fun <T>set(setting: ServerSetting<T>, value: T) {
        val stringValue = setting.serializer(value)
        SettingsRepo.updateServerSetting(setting, stringValue)
    }
}