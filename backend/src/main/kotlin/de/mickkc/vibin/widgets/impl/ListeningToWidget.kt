package de.mickkc.vibin.widgets.impl

import de.mickkc.vibin.repos.ControlRepo
import de.mickkc.vibin.repos.TrackRepo
import de.mickkc.vibin.widgets.BaseWidget
import de.mickkc.vibin.widgets.WidgetContext
import de.mickkc.vibin.widgets.WidgetUtils
import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.h2
import kotlinx.html.id
import kotlinx.html.img
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.style
import kotlinx.html.title

class ListeningToWidget(ctx: WidgetContext) : BaseWidget(ctx) {

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
                        +t("widgets.listening_to.empty_state")
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
        }

        if (interactive) {
            script(src = "/api/js/listeningToUpdater.js") {}
        }
    }
}