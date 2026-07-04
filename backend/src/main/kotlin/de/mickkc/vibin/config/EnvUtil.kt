package de.mickkc.vibin.config

import java.io.File

data class EnvKey<T>(
    val name: String,
    val default: T?,
    val parser: (String) -> T?
)

object EnvUtil {

    val overrides: MutableMap<String, String> = mutableMapOf()

    /**
     * Retrieves the value of an environment variable.
     *
     * @param key The name of the environment variable.
     * @return The value of the environment variable, or null if it is not set and has no default value.
     */
    fun<T> getOrNull(key: EnvKey<T>): T? {
        val strValue = overrides[key.name] ?: System.getenv(key.name)
        if (strValue == null) {
            return key.default
        }
        return key.parser(strValue)
    }

    /**
     * Retrieves the value of an environment variable or throws an exception if it is not set and has no default value.
     *
     * @param key The name of the environment variable.
     * @return The value of the environment variable.
     * @throws IllegalStateException if the environment variable is not set.
     */
    inline fun<reified T> getOrError(key: EnvKey<T>): T {
        val strValue = overrides[key.name] ?: System.getenv(key.name)
        if (strValue == null) {
            if (key.default != null) return key.default
            throw IllegalStateException("Required environment variable ${key.name} is not set.")

        }
        return key.parser(strValue) ?: throw IllegalStateException(
            "The value \"$strValue\" of the environment variable ${key.name} could not be parsed to ${T::class.java.name}."
        )
    }

    /**
     * Adds an override for an environment variable (used for testing).
     *
     * @param key The name of the environment variable.
     * @param value The value to override with.
     */
    fun addOverride(key: EnvKey<*>, value: String) {
        overrides[key.name] = value
    }

    private fun fileParser(s: String): File = File(s)

    val DB_HOST = EnvKey("DB_HOST", null, String::trim)
    val DB_PORT = EnvKey("DB_PORT", null, String::toIntOrNull)
    val DB_NAME = EnvKey("DB_NAME", null, String::trim)
    val DB_USER = EnvKey("DB_USER", null, String::trim)
    val DB_PASSWORD = EnvKey("DB_PASSWORD", null, String::trim)

    val MUSIC_DIR = EnvKey("MUSIC_DIR", File("/opt/vibin/music"), ::fileParser)
    val UPLOADS_DIR = EnvKey("UPLOADS_DIR", File("/opt/vibin/uploads"), ::fileParser)

    val THUMBNAIL_DIR = EnvKey("THUMBNAIL_DIR", File("/opt/vibin/thumbnails"), ::fileParser)
    val FRONTEND_DIR = EnvKey("FRONTEND_DIR", null, ::fileParser)

    val IMAGE_CACHE_DIR = EnvKey("IMAGE_CACHE_DIR", File("/opt/vibin/image_cache"), ::fileParser)
    val WIDGET_IMAGE_CACHE_DIR = EnvKey("WIDGET_IMAGE_CACHE_DIR", File("/opt/vibin/widget_image_cache"), ::fileParser)

    val WIDGET_CACHE_EXPIRATION_MINUTES = EnvKey("WIDGET_CACHE_EXPIRATION_MINUTES", 20, String::toIntOrNull)

    val WIDGET_IMAGE_CACHE_RATE_LIMIT_REQUESTS = EnvKey("WIDGET_IMAGE_CACHE_RATE_LIMIT_REQUESTS", 5, String::toIntOrNull)
    val CHROME_BINARY_PATH = EnvKey("CHROME_BINARY_PATH", "/usr/bin/chromium", String::trim)

    val SMTP_HOST = EnvKey("SMTP_HOST", null, String::trim)
    val SMTP_PORT = EnvKey("SMTP_PORT", null, String::toIntOrNull)
    val SMTP_STARTTLS = EnvKey("SMT_STARTTLS", false, String::toBooleanStrictOrNull)
    val SMTP_STARTTLS_REQUIRED = EnvKey("SMTP_STARTTLS_REQUIRED", true, String::toBooleanStrictOrNull)
    val SMTP_SSL = EnvKey("SMTP_SSL", true, String::toBooleanStrictOrNull)

    val SMTP_AUTH = EnvKey("SMTP_AUTH", true, String::toBooleanStrictOrNull)
    val SMTP_USERNAME = EnvKey("SMTP_USERNAME", null, String::trim)
    val SMTP_PASSWORD = EnvKey("SMTP_PASSWORD", null, String::trim)
    val SMTP_FROM = EnvKey("SMTP_FROM", null, String::trim)

    val REAL_IP_HEADER = EnvKey("REAL_IP_HEADER", "X-Forwarded-For", String::trim)

    val SERVER_HOST = EnvKey("SERVER_HOST", "0.0.0.0", String::trim)
    val SERVER_PORT = EnvKey("SERVER_PORT", 8080, String::toIntOrNull)
}