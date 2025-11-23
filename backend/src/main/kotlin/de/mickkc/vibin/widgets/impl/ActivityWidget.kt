package de.mickkc.vibin.widgets.impl

import de.mickkc.vibin.repos.ListenRepo
import de.mickkc.vibin.utils.DateTimeUtils
import de.mickkc.vibin.widgets.BaseWidget
import de.mickkc.vibin.widgets.WidgetContext
import de.mickkc.vibin.widgets.WidgetUtils
import de.mickkc.vibin.widgets.components.statCard
import kotlinx.html.div
import kotlinx.html.h2
import kotlinx.html.stream.appendHTML
import kotlinx.html.style
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.roundToInt

class ActivityWidget(widgetContext: WidgetContext, val extended: Boolean = true) : BaseWidget(widgetContext) {

    override fun render(): String {

        val start = DateTimeUtils.startOfYear()
        val startLocalDate = DateTimeUtils.toLocalDate(start)

        val listensPerDay = ListenRepo.getListensPerDay(ctx.userId, start)
        val maxListensPerDay = listensPerDay.maxOfOrNull { it.second } ?: 0

        val end = LocalDate.now()

        return buildString {
            appendHTML().div("widget-body") {

                h2 {
                    +t("widgets.activity.title")
                }

                div("activity-grid") {
                    style =
                        "grid-template-columns: repeat(${ceil((end.dayOfYear - startLocalDate.dayOfYear + 1) / 7f).roundToInt()}, 1fr);"

                    for (day in 1..<startLocalDate.dayOfWeek.value) {
                        div {}
                    }

                    for (day in startLocalDate.dayOfYear..end.dayOfYear) {
                        val listens = listensPerDay.find { it.first.dayOfYear == day }?.second ?: 0
                        val intensity = if (maxListensPerDay > 0) listens.toFloat() / maxListensPerDay else 0f

                        val ratio = 0.1f + (intensity * 0.9f)
                        val bgColor = WidgetUtils.blendColors(ctx.backgroundColor, ctx.accentColor, ratio)

                        div {
                            style = "background-color: ${WidgetUtils.colorToHex(bgColor)};"
                        }
                    }
                }

                if (extended) {
                    val listensThisYear = listensPerDay.sumOf { it.second }
                    val listensThisMonth = listensPerDay.filter { it.first.monthValue == end.monthValue }.sumOf { it.second }
                    val listensThisWeek = listensPerDay.filter { it.first >= end.minusDays(7) }.sumOf { it.second }
                    val listensToday = listensPerDay.find { it.first.dayOfYear == end.dayOfYear }?.second ?: 0

                    div("stats") {
                        statCard(this@ActivityWidget, listensThisYear.toString(), t("widgets.activity.year"))
                        statCard(this@ActivityWidget, listensThisMonth.toString(), t("widgets.activity.month"))
                        statCard(this@ActivityWidget, listensThisWeek.toString(), t("widgets.activity.week"))
                        statCard(this@ActivityWidget, listensToday.toString(), t("widgets.activity.today"))
                    }
                }
            }
        }
    }

}