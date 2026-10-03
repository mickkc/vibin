package de.mickkc.vibin.widgets.impl

import de.mickkc.vibin.config.EnvUtil
import de.mickkc.vibin.config.EnvUtil.EXTERNAL_URL
import de.mickkc.vibin.repos.ControlRepo
import de.mickkc.vibin.repos.SessionPlayState
import de.mickkc.vibin.repos.TrackRepo
import de.mickkc.vibin.utils.DateTimeUtils
import de.mickkc.vibin.widgets.BaseWidget
import de.mickkc.vibin.widgets.WidgetContext
import de.mickkc.vibin.widgets.WidgetUtils
import kotlinx.html.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class ListeningToWidget(ctx: WidgetContext) : BaseWidget(ctx) {

    fun getRelativeDate(ts: Long): String {
        val now = DateTimeUtils.nowLocalDate()
        val date = DateTimeUtils.toLocalDate(ts)

        val diff = ChronoUnit.DAYS.between(date, now)

        return if (diff == 0L) t("widgets.listening_to.date.today")
        else if (diff == 1L) t("widgets.listening_to.date.yesterday")
        else if (diff < 7L) t("widgets.listening_to.date.days_ago", diff)
        else if (diff < 30L) t("widgets.listening_to.date.weeks_ago", diff / 7L)
        else date.format(DateTimeFormatter.ofPattern(t("widgets.listening_to.date.format")))
    }

    val baseUrl = run {
        if (ctx.external) {
            return@run EnvUtil.getOrNull(EXTERNAL_URL) ?: "https://external-url-not-configured.example.com"
        }
        return@run ""
    }

    override fun FlowContent.apply() {

        val initialSession = ControlRepo.getCurrentSession(ctx.userId)
            ?: ControlRepo.getLatestSession(ctx.userId)
        val initialTrack = initialSession?.currentTrackId?.let { TrackRepo.getById(it) }
        val initialAlbum = initialTrack?.let { TrackRepo.getAlbum(it) }
        val initialArtists = initialTrack?.let { TrackRepo.getArtists(it) }?.joinToString { it.name }
        val coverUrl = WidgetUtils.getImageUrl(initialTrack?.let { TrackRepo.getCover(it) }, "track", 128, ctx.external)

        div("widget-body listening-to-widget") {
            if (interactive) {
                attributes["data-ws-url"] = "$baseUrl/api/widgets/${ctx.widgetId}/listeningto/ws"
                attributes["data-lang"] = buildJsonObject {
                    putJsonObject("date") {
                        put("today", t("widgets.listening_to.date.today"))
                        put("yesterday", t("widgets.listening_to.date.yesterday"))
                        put("daysAgo", t("widgets.listening_to.date.days_ago", "{0}"))
                        put("weeksAgo", t("widgets.listening_to.date.weeks_ago", "{0}"))
                    }
                    putJsonObject("state") {
                        put("empty", t("widgets.listening_to.state.empty"))
                        put("offline", t("widgets.listening_to.state.offline", "{0}"))
                        put("playing", t("widgets.listening_to.state.playing"))
                        put("paused", t("widgets.listening_to.state.paused"))
                        put("stopped", t("widgets.listening_to.state.stopped"))
                    }
                }.toString()
            }

            h2 {
                +t("widgets.listening_to.title")
            }
            div("listening-to") {
                img(t("widgets.listening_to.album_cover"), coverUrl, classes = "cover") {
                    title = initialAlbum?.title ?: t("widgets.listening_to.unknown")
                }
                div("details") {
                    p("title") {
                        +(initialTrack?.title ?: t("widgets.listening_to.unknown"))
                    }
                    p("artists") {
                        +(initialArtists ?: t("widgets.listening_to.unknown"))
                    }
                    p("state") {
                        if (initialSession != null) {
                            unsafe {
                                if (initialSession.removed) {
                                    +t("widgets.listening_to.state.offline", getRelativeDate(initialSession.lastUpdate))
                                }
                                else +when (initialSession.state) {
                                    SessionPlayState.PLAYING -> t("widgets.listening_to.state.playing")
                                    SessionPlayState.PAUSED -> t("widgets.listening_to.state.paused")
                                    SessionPlayState.STOPPED -> t("widgets.listening_to.state.stopped")
                                    else -> ""
                                }
                            }
                        }
                        else {
                            +t("widgets.listening_to.state.empty")
                        }
                    }
                    div("progress") {
                        if (initialSession == null || initialSession.removed || !interactive) {
                            style = "visibility: hidden"
                        }

                        p("position") {
                            +"0:00"
                        }

                        div("progress-container") {
                            val bgColor = WidgetUtils.blendColors(ctx.backgroundColor, ctx.accentColor, 0.3f)

                            style = "background: ${WidgetUtils.colorToHex(bgColor)}"

                            div("progress-bar") {
                                style = "background: $accentColor"
                            }
                        }

                        p("duration") {
                            +"3:00"
                        }
                    }
                }
            }

            if (interactive) {
                script(src = "$baseUrl/api/js/listeningToUpdater.js") {}
            }
        }
    }
}