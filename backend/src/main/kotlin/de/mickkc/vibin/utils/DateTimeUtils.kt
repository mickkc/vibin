package de.mickkc.vibin.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

object DateTimeUtils {

    val logger: Logger = LoggerFactory.getLogger(DateTimeUtils::class.java)

    fun now(): Long = System.currentTimeMillis() / 1000

    fun nowLocalDate(): LocalDate = LocalDate.now()

    fun startOfMonth(): Long {
        val now = LocalDate.now()
        val startOfMonth = now.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
        return startOfMonth
    }

    fun startOfYear(): Long {
        val now = LocalDate.now()
        val startOfYear = now.withDayOfYear(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond()
        return startOfYear
    }

    fun toLocalDate(epochSeconds: Long): LocalDate {
        return LocalDate.ofEpochDay(epochSeconds / 86400)
    }

    fun localDateToTimestamp(localDate: LocalDate): Long {
        return localDate.toEpochSecond(LocalTime.MIN, ZoneOffset.UTC)
    }

    fun formatDuration(totalSeconds: Long): String = buildString {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        if (hours > 0) {
            append("${hours}h ")
        }
        if (minutes > 0 || hours > 0) {
            append("${minutes}m ")
        }
        append("${seconds}s")
    }

    private val secondsMap = mapOf(
        'y' to 31_556_952L,
        'M' to 2_629_746L,
        'w' to 604_800L,
        'd' to 86_400L,
        'h' to 3_600L,
        'm' to 60L,
        's' to 1
    )

    private val keysDesc = listOf('y', 'M', 'w', 'd', 'h', 'm', 's')

    fun parseDuration(durationStr: String): Long? {

        val cleanedDurationStr = durationStr
            .replace(Regex("(?<=[a-zA-Z])[a-zA-Z]+|\\s"), "")

        if (cleanedDurationStr != durationStr)
            logger.trace("Cleaned up duration string: '$durationStr' -> '$cleanedDurationStr'.")

        if (cleanedDurationStr.isEmpty()) {
            logger.warn("Could not parse '$durationStr' as a duration: Empty string after processing.")
            return null
        }

        var numberBuffer = ""
        var durationSeconds = 0L

        cleanedDurationStr.forEach { c ->
            if (c.isDigit())
                numberBuffer += c
            else {
                if (numberBuffer.isEmpty()) {
                    logger.warn("Could not parse '$durationStr' as a duration: Unit without amount: $c.")
                    return null
                }

                durationSeconds += when (c) {
                    'M' -> numberBuffer.toLong() * secondsMap[c]!!
                    else -> {
                        val seconds = secondsMap[c.lowercaseChar()]
                        if (seconds == null) {
                            logger.warn("Could not parse '$durationStr' as a duration: Invalid unit: $c.")
                            return null
                        }
                        numberBuffer.toLong() * seconds
                    }
                }
                numberBuffer = ""
            }
        }

        if (numberBuffer.isNotEmpty()) {
            logger.warn("Could not parse '$durationStr' as a duration: Amount without value: $numberBuffer.")
            return null
        }

        return durationSeconds
    }

    fun formatDurationFull(duration: Long): String {

        if (duration == 0L) return "0s"

        var remainingDuration = duration
        var output = ""

        do {
            for (key in keysDesc) {
                val value = secondsMap[key]!!
                if (value > remainingDuration) continue

                val amount = remainingDuration / value
                output += "${amount}${key} "

                remainingDuration -= amount * value
                break
            }
        } while (remainingDuration > 0)

        return output.trim()
    }
}