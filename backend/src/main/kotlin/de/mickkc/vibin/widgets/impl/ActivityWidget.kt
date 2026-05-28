package de.mickkc.vibin.widgets.impl

import de.mickkc.vibin.repos.ListenTrackingRepo
import de.mickkc.vibin.utils.DateTimeUtils
import de.mickkc.vibin.widgets.BaseWidget
import de.mickkc.vibin.widgets.WidgetContext
import de.mickkc.vibin.widgets.WidgetUtils
import de.mickkc.vibin.widgets.components.statCard
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.*
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ActivityWidget(widgetContext: WidgetContext, val extended: Boolean = true) : BaseWidget(widgetContext) {

    private fun secondsToTimeStr(seconds: Long): String = buildList {
        val duration = seconds.seconds
        if (duration >= 1.days) add("${duration.inWholeDays}d")
        if (duration >= 1.hours) add("${duration.inWholeHours % 24}h")
        if (duration >= 1.minutes) add("${duration.inWholeMinutes % 60}m")
        add("${duration.inWholeSeconds % 60}s")
    }.joinToString(" ")

    override fun render(interactive: Boolean): String {

        val start = DateTimeUtils.startOfYear()
        val startLocalDate = DateTimeUtils.toLocalDate(start)

        val durationsPerDay = ListenTrackingRepo.getListenDurationPerDay(ctx.userId, start)
        val longestDurationPerDay = durationsPerDay.maxOfOrNull { it.second } ?: 0

        val end = LocalDate.now()

        return buildString {
            appendHTML(prettyPrint = false).div("widget-body") {

                h2 {
                    +t("widgets.activity.title")
                }

                // "g" prefix to ensure the ID starts with a letter
                val gridId = "g" + UUID.randomUUID().toString().substringBefore("-")

                div("activity-grid") {
                    this.id = gridId
                    style =
                        "grid-template-columns: repeat(${ceil((end.dayOfYear - startLocalDate.dayOfYear + 1) / 7f).roundToInt()}, 1fr);"

                    for (day in 1..<startLocalDate.dayOfWeek.value) {
                        div {}
                    }

                    for (day in startLocalDate.dayOfYear..end.dayOfYear) {
                        val duration = durationsPerDay.find { it.first.dayOfYear == day }?.second ?: 0
                        val intensity = if (longestDurationPerDay > 0) duration.toFloat() / longestDurationPerDay else 0f

                        val ratio = 0.1f + (intensity * 0.9f)
                        val bgColor = WidgetUtils.blendColors(ctx.backgroundColor, ctx.accentColor, ratio)

                        div {
                            style = "background-color: ${WidgetUtils.colorToHex(bgColor)};"
                        }
                    }
                }

                if (extended) {
                    val durationThisYear = durationsPerDay.sumOf { it.second }
                    val durationThisMonth = durationsPerDay.filter { it.first.monthValue == end.monthValue }.sumOf { it.second }
                    val durationThisWeek = durationsPerDay.filter { it.first >= end.with(DayOfWeek.MONDAY) }.sumOf { it.second }
                    val durationToday = durationsPerDay.find { it.first.dayOfYear == end.dayOfYear }?.second ?: 0

                    div("stats") {
                        statCard(this@ActivityWidget, secondsToTimeStr(durationThisYear), t("widgets.activity.year"))
                        statCard(this@ActivityWidget, secondsToTimeStr(durationThisMonth), t("widgets.activity.month"))
                        statCard(this@ActivityWidget, secondsToTimeStr(durationThisWeek), t("widgets.activity.week"))
                        statCard(this@ActivityWidget, secondsToTimeStr(durationToday), t("widgets.activity.today"))
                    }
                }

                // Auto-scroll to the end of the grid to show the most recent activity
                unsafe {
                    +"""
                        <script>
                            let $gridId = document.getElementById("$gridId");
                            $gridId.scrollLeft = $gridId.scrollWidth;
                        </script>
                    """.trimIndent()
                }
            }
        }
    }

}