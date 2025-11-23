package de.mickkc.vibin.settings.server

import de.mickkc.vibin.settings.Setting

open class ServerSetting<T>(
    key: String,
    parser: (String) -> T,
    serializer: (T) -> String,
    defaultValue: T,
) : Setting<T>(
    key,
    parser,
    serializer,
    defaultValue,
)