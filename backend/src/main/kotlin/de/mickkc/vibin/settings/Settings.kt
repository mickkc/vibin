package de.mickkc.vibin.settings

import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.PermissionRepo
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
    fun <T>get(setting: ServerSetting<T>): T {
        val stringValue = SettingsRepo.getServerSettingValue(setting.key)
        val parsedValue = stringValue?.let { setting.parser(it) }
        return parsedValue ?: setting.defaultValue
    }

    fun <T>getOrNull(setting: ServerSetting<T>): T? {
        val stringValue = SettingsRepo.getServerSettingValue(setting.key)
        val parsedValue = stringValue?.let { setting.parser(it) }
        return parsedValue
    }

    fun <T>get(userSetting: UserSetting<T>, userId: Long): T {
        val stringValue = SettingsRepo.getUserSettingValue(userSetting.key, userId)
        val parsedValue = stringValue?.let { userSetting.parser(it) }
        return parsedValue ?: userSetting.defaultValue
    }

    fun <T>getOrNull(userSetting: UserSetting<T>, userId: Long): T? {
        val stringValue = SettingsRepo.getUserSettingValue(userSetting.key, userId)
        val parsedValue = stringValue?.let { userSetting.parser(it) }
        return parsedValue
    }

    /**
     * Gets a setting either from a user (if specified) or the server (if the user has the optional [permissionType])
     * as a fallback.
     *
     * @param userSetting The setting to try loading first.
     * @param serverSetting The fallback server setting used when the client setting is not set or an empty string.
     * @param userId The id of the user to load the setting for. If null, the server setting is returned.
     * @param permissionType An optional permission required for the user to be able to use the server settings, e.g.
     *                       [PermissionType.USE_SERVER_API_CREDENTIALS].
     * @param isValid Used to check if the value is valid before returning/using fallback.
     * @return The loaded user setting, server setting, or null if none of the two are set and valid.
     */
    fun <T>getUserOrServer(userSetting: UserSetting<T>, serverSetting: ServerSetting<T>, userId: Long?, permissionType: PermissionType? = null, isValid: (T) -> Boolean): T? {
        return userId
            ?.let { uId -> getOrNull(userSetting, uId) }
            ?.takeIf { isValid(it) }
            ?: if (permissionType != null && userId != null && !PermissionRepo.hasPermission(userId, permissionType))
                null
            else
                getOrNull(serverSetting)?.takeIf { isValid(it) }
    }

    fun <T>set(setting: ServerSetting<T>, value: T) {
        val stringValue = setting.serializer(value)
        SettingsRepo.updateServerSetting(setting, stringValue)
    }
}