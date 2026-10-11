package de.mickkc.vibin.widgets.impl

import de.mickkc.vibin.repos.ListenTrackingRepo
import de.mickkc.vibin.utils.DateTimeUtils
import de.mickkc.vibin.widgets.BaseWidget
import de.mickkc.vibin.widgets.WidgetContext
import de.mickkc.vibin.widgets.WidgetUtils
import de.mickkc.vibin.widgets.components.statCard
import kotlinx.html.*
import java.time.DayOfWeek
import java.util.*

class ActivityWidget(widgetContext: WidgetContext, val extended: Boolean = true) : BaseWidget(widgetContext) {

    override fun FlowContent.apply() {
        val startLocalDate = DateTimeUtils.nowLocalDate().minusYears(1)

        val durationsPerDay = ListenTrackingRepo.getListenDurationPerDay(ctx.userId, DateTimeUtils.localDateToTimestamp(startLocalDate))
        val longestDurationPerDay = durationsPerDay.maxOfOrNull { it.second } ?: 0

        val end = DateTimeUtils.nowLocalDate()

        div("widget-body") {

            h2 {
                +t("widgets.activity.title")
            }

            // "g" prefix to ensure the ID starts with a letter
            val gridId = "g" + UUID.randomUUID().toString().substringBefore("-")

            div("activity-grid") {
                this.id = gridId

                for (day in 1..<startLocalDate.dayOfWeek.value) {
                    div {}
                }

                val dateStream = startLocalDate.datesUntil(end.plusDays(1))

                for (day in dateStream) {
                    val duration = durationsPerDay.find { it.first.isEqual(day) }?.second ?: 0
                    val intensity = if (longestDurationPerDay > 0) duration.toFloat() / longestDurationPerDay else 0f

                    val ratio = 0.05f + (if (duration > 0) 0.1f else 0f) + (intensity * 0.85f)
                    val bgColor = WidgetUtils.blendColors(ctx.backgroundColor, ctx.accentColor, ratio)

                    div {
                        style = "background-color: ${WidgetUtils.colorToHex(bgColor)};"
                    }
                }
            }

            if (extended) {
                val durationThisYear = durationsPerDay.sumOf { it.second }
                val durationThisMonth = durationsPerDay.filter { it.first.monthValue == end.monthValue && it.first.year == end.year }.sumOf { it.second }
                val durationThisWeek = durationsPerDay.filter { it.first >= end.with(DayOfWeek.MONDAY) }.sumOf { it.second }
                val durationToday = durationsPerDay.find { it.first.dayOfYear == end.dayOfYear && it.first.year == end.year }?.second ?: 0

                div("stats") {
                    statCard(this@ActivityWidget, DateTimeUtils.formatDuration(durationThisYear), t("widgets.activity.year"))
                    statCard(this@ActivityWidget, DateTimeUtils.formatDuration(durationThisMonth), t("widgets.activity.month"))
                    statCard(this@ActivityWidget, DateTimeUtils.formatDuration(durationThisWeek), t("widgets.activity.week"))
                    statCard(this@ActivityWidget, DateTimeUtils.formatDuration(durationToday), t("widgets.activity.today"))
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