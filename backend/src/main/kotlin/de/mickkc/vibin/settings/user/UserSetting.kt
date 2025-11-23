package de.mickkc.vibin.settings.user

import de.mickkc.vibin.settings.Setting

open class UserSetting<T>(
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