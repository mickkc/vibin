package de.mickkc.vibin.config

object EnvUtil {

    private val overrides: MutableMap<String, String> = mutableMapOf()

    /**
     * Retrieves the value of an environment variable.
     *
     * @param key The name of the environment variable.
     * @return The value of the environment variable, or null if it is not set.
     */
    fun get(key: String): String? {
        return overrides[key] ?: System.getenv(key)
    }

    /**
     * Retrieves the value of an environment variable or throws an exception if it is not set.
     *
     * @param key The name of the environment variable.
     * @return The value of the environment variable.
     * @throws IllegalStateException if the environment variable is not set.
     */
    fun getOrError(key: String): String {
        return overrides[key] ?: System.getenv(key) ?: throw IllegalStateException("Environment variable $key is not set")
    }

    /**
     * Retrieves the value of an environment variable or returns a default value if it is not set.
     *
     * @param key The name of the environment variable.
     * @param default The default value to return if the environment variable is not set.
     * @return The value of the environment variable, or the default value if it is not set.
     */
    fun getOrDefault(key: String, default: String): String {
        return overrides[key] ?: System.getenv(key) ?: default
    }

    /**
     * Adds an override for an environment variable (used for testing).
     *
     * @param key The name of the environment variable.
     * @param value The value to override with.
     */
    fun addOverride(key: String, value: String) {
        overrides[key] = value
    }

    const val DB_HOST = "DB_HOST"
    const val DB_PORT = "DB_PORT"
    const val DB_NAME = "DB_NAME"
    const val DB_USER = "DB_USER"
    const val DB_PASSWORD = "DB_PASSWORD"

    const val MUSIC_DIR = "MUSIC_DIR"
    const val DEFAULT_MUSIC_DIR = "/opt/vibin/music"
    const val UPLOADS_DIR = "UPLOADS_DIR"
    const val DEFAULT_UPLOADS_DIR = "/opt/vibin/uploads"

    const val THUMBNAIL_DIR = "THUMBNAIL_DIR"
    const val DEFAULT_THUMBNAIL_DIR = "/opt/vibin/thumbnails"
    const val FRONTEND_DIR = "FRONTEND_DIR"

    const val IMAGE_CACHE_DIR = "IMAGE_CACHE_DIR"
    const val DEFAULT_IMAGE_CACHE_DIR = "/opt/vibin/image_cache"
    const val WIDGET_IMAGE_CACHE_DIR = "WIDGET_IMAGE_CACHE_DIR"
    const val DEFAULT_WIDGET_IMAGE_CACHE_DIR = "/opt/vibin/widget_image_cache"

    const val WIDGET_CACHE_EXPIRATION_MINUTES = "WIDGET_CACHE_EXPIRATION_MINUTES"
    const val DEFAULT_WIDGET_CACHE_EXPIRATION_MINUTES = "20"

    const val WIDGET_IMAGE_CACHE_RATE_LIMIT_REQUESTS = "WIDGET_IMAGE_CACHE_RATE_LIMIT_REQUESTS"
    const val DEFAULT_WIDGET_IMAGE_CACHE_RATE_LIMIT_REQUESTS = "5"
    const val CHROME_BINARY_PATH = "CHROME_BINARY_PATH"
    const val DEFAULT_CHROME_BINARY_PATH = "/usr/bin/chromium"
}