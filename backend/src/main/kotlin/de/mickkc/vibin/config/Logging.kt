package de.mickkc.vibin.config

import de.mickkc.vibin.config.EnvUtil.LOG_LEVEL
import io.ktor.server.application.Application
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.Level
import io.ktor.util.logging.Logger

fun Application.configureLogging() {

    val globalLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger

    val logLevelStr = EnvUtil.getOrError(LOG_LEVEL)
    val logLevel = when (logLevelStr.lowercase()) {
        "trace" -> Level.TRACE
        "debug" -> Level.DEBUG
        "info" -> Level.INFO
        "warn", "warning" -> Level.WARN
        "error" -> Level.ERROR
        "off" -> Level.OFF
        else -> {
            globalLogger.warn("$logLevelStr is not a valid loglevel. Valid values are: trace, debug, info, warn, error or off. Using default \"info\".")
            Level.INFO
        }
    }

    globalLogger.level = logLevel
}