package de.mickkc.vibin.utils

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

object DateTimeUtils {

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
}