package de.mickkc.vibin.utils

import java.time.LocalDate
import java.time.ZoneId

object DateTimeUtils {

    fun now(): Long = System.currentTimeMillis() / 1000

    fun startOfMonth(): Long {
        val now = LocalDate.now()
        val startOfMonth = now.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
        return startOfMonth
    }
}