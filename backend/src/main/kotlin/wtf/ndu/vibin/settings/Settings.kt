package wtf.ndu.vibin.settings

import wtf.ndu.vibin.repos.SettingsRepo

object Settings {

    /**
     * Retrieves the value of a setting.
     *
     * @param setting The setting to retrieve.
     * @return The value of the setting, or its default value if not found.
     */
    fun <T>get(setting: Setting<T>): T {
        val stringValue = SettingsRepo.getSetting(setting.key)
        val parsedValue = stringValue?.let { setting.parser(it) }
        return parsedValue ?: setting.defaultValue
    }
}