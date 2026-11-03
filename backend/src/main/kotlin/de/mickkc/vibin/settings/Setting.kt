package de.mickkc.vibin.settings

open class Setting<T>(
    val key: String,
    val parser: (String) -> T?,
    val serializer: (T) -> String,
    val defaultValue: T,
    val validationRegex: Regex? = null,
    val secret: Boolean = false
)