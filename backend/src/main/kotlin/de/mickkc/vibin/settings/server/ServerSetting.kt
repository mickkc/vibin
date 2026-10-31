package de.mickkc.vibin.settings.server

import de.mickkc.vibin.settings.Setting

open class ServerSetting<T>(
    key: String,
    parser: (String) -> T?,
    serializer: (T) -> String,
    defaultValue: T,
    validationRegex: Regex? = null,
    secret: Boolean = false
) : Setting<T>(
    key,
    parser,
    serializer,
    defaultValue,
    validationRegex,
    secret
)