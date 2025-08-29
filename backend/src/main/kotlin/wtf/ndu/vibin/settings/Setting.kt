package wtf.ndu.vibin.settings

open class Setting<T>(
    val key: String,
    val parser: (String) -> T,
    val serializer: (T) -> String,
    val defaultValue: T,
    val generator: (() -> Collection<T>)? = null
)