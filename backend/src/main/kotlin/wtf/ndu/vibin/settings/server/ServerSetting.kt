package wtf.ndu.vibin.settings.server

import wtf.ndu.vibin.settings.Setting

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