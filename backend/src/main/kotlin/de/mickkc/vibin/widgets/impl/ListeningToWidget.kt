package de.mickkc.vibin.widgets.impl

import de.mickkc.vibin.repos.ControlRepo
import de.mickkc.vibin.repos.TrackRepo
import de.mickkc.vibin.utils.DateTimeUtils
import de.mickkc.vibin.widgets.BaseWidget
import de.mickkc.vibin.widgets.WidgetContext
import de.mickkc.vibin.widgets.WidgetUtils
import kotlinx.html.*
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

    override fun FlowContent.apply() {

        val initialSession = ControlRepo.getCurrentSession(ctx.userId)
            ?: ControlRepo.getLatestSession(ctx.userId)
        val initialTrack = initialSession?.currentTrackId?.let { TrackRepo.getById(it) }
        val initialAlbum = initialTrack?.let { TrackRepo.getAlbum(it) }
        val initialArtists = initialTrack?.let { TrackRepo.getArtists(it) }?.joinToString { it.name }
        val coverUrl = WidgetUtils.getImageUrl(initialTrack?.let { TrackRepo.getCover(it) }, "track", 128)

        div("widget-body") {
            h2 {
                +t("widgets.listening_to.title")
            }
            div("listening-to") {
                img(t("widgets.listening_to.album_cover"), coverUrl, classes = "cover") {
                    id = "listeningToAlbumCover"
                    title = initialAlbum?.title ?: t("widgets.listening_to.unknown")
                }
                div("details") {
                    p("title") {
                        id = "listeningToTitle"
                        +(initialTrack?.title ?: t("widgets.listening_to.unknown"))
                    }
                    p("artists") {
                        id = "listeningToArtists"
                        +(initialArtists ?: t("widgets.listening_to.unknown"))
                    }
                    p("state") {
                        id = "listeningToState"
                        if (initialSession != null) {
                            unsafe {
                                +t("widgets.listening_to.state.offline", getRelativeDate(initialSession.lastUpdate))

                            }
                        }
                        else {
                            +t("widgets.listening_to.state.empty")
                        }
                    }
                    div("progress") {
                        id = "listeningToProgressRow"

                        p {
                            id = "listeningToPosition"
                            +"0:00"
                        }

                        div("progress-container") {
                            val bgColor = WidgetUtils.blendColors(ctx.backgroundColor, ctx.accentColor, 0.3f)

                            style = "background: ${WidgetUtils.colorToHex(bgColor)}"

                            div("progress-bar") {
                                style = "background: $accentColor"
                                id = "listeningToProgress"
                            }
                        }

                        p {
                            id = "listeningToDuration"
                            +"3:00"
                        }
                    }
                }
            }

            if (interactive) {
                script {
                    unsafe {
                        +"""
                            const lang = {
                                "date": {
                                    "today": "${t("widgets.listening_to.date.today")}",
                                    "yesterday": "${t("widgets.listening_to.date.yesterday")}",
                                    "daysAgo": "${t("widgets.listening_to.date.days_ago", "{0}")}",
                                    "weeksAgo": "${t("widgets.listening_to.date.weeks_ago", "{0}")}"
                                },
                                "state": {
                                    "empty": "${t("widgets.listening_to.state.empty")}",
                                    "offline": "${t("widgets.listening_to.state.offline", "{0}")}",
                                    "playing": "${t("widgets.listening_to.state.playing")}",
                                    "paused": "${t("widgets.listening_to.state.paused")}",
                                    "stopped": "${t("widgets.listening_to.state.stopped")}"
                                }
                            };
                        """.trimIndent()
                    }
                }

                script(src = "/api/js/listeningToUpdater.js") {}
            }
        }
    }
}