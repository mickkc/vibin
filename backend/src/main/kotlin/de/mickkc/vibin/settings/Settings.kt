package de.mickkc.vibin.settings

import de.mickkc.vibin.dto.SettingDto
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

    fun isValid(serverSetting: ServerSetting<*>): Boolean {
        if (serverSetting.validationRegex == null) return true
        val value = SettingsRepo.getServerSettingValue(serverSetting.key) ?: serverSetting.serializer(serverSetting.defaultValue)

        return serverSetting.validationRegex.matches(value)
    }

    fun <T>get(userSetting: UserSetting<T>, userId: Long): T {
        val stringValue = SettingsRepo.getUserSettingValue(userSetting.key, userId)
        val parsedValue = stringValue?.let { userSetting.parser(it) }
        return parsedValue ?: userSetting.defaultValue
    }

    fun isValid(userSetting: UserSetting<*>, userId: Long): Boolean {
        if (userSetting.validationRegex == null) return true
        val value = SettingsRepo.getUserSettingValue(userSetting.key, userId) ?: userSetting.serializer(userSetting.defaultValue)

        return userSetting.validationRegex.matches(value)
    }

    fun <T>getOrNull(userSetting: UserSetting<T>, userId: Long): T? {
        val stringValue = SettingsRepo.getUserSettingValue(userSetting.key, userId)
        val parsedValue = stringValue?.let { userSetting.parser(it) }
        return parsedValue
    }

    fun allValid(settings: List<Setting<*>>, userId: Long): Boolean {
        return settings.all { setting ->
            if (setting is ServerSetting<*>) {
                return@all isValid(setting)
            }
            else if (setting is UserSetting<*>) {
                return@all isValid(setting, userId)
            }
            return@all false
        }
    }

    enum class SettingSource {
        Server,
        User,
        None
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
     * @return The source where the setting was loaded from (server, user or none) and its value.
     */
    fun <T>getUserOrServer(userSetting: UserSetting<T>, serverSetting: ServerSetting<T>, userId: Long?, permissionType: PermissionType? = null, isValid: (T) -> Boolean): Pair<SettingSource, T?> {
        return userId
            ?.let { uId -> getOrNull(userSetting, uId) }
            ?.takeIf { isValid(it) }
            ?.let { SettingSource.User to it }
            ?: if (permissionType != null && userId != null && !PermissionRepo.hasPermission(userId, permissionType))
                SettingSource.None to null
            else
                getOrNull(serverSetting)?.takeIf { isValid(it) }?.let { SettingSource.Server to it }
                    ?: (SettingSource.None to null)
    }

    fun <T>set(setting: ServerSetting<T>, value: T) {
        val stringValue = setting.serializer(value)
        SettingsRepo.updateServerSetting(setting, stringValue)
    }

    fun<T> toDto(setting: Setting<T>, userId: Long): SettingDto<T> {
        return SettingDto(
            key = setting.key,
            value = when (setting) {
                is ServerSetting<T> -> get(setting)
                is UserSetting<T> -> get(setting, userId)
                else -> throw IllegalArgumentException("Invalid setting type: $setting")
            },
            validationRegex = setting.validationRegex?.pattern
        )
    }
}