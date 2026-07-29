package de.mickkc.vibin.widgets.impl

import de.mickkc.vibin.repos.*
import de.mickkc.vibin.utils.DateTimeUtils
import de.mickkc.vibin.widgets.BaseWidget
import de.mickkc.vibin.widgets.WidgetContext
import de.mickkc.vibin.widgets.components.statCard
import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.h2

class ServerStatsWidget(ctx: WidgetContext) : BaseWidget(ctx) {

    override fun FlowContent.apply() {
        val totalDurationSeconds = TrackRepo.getTotalRuntimeSeconds()
        val totalListenDurationSeconds = ListenTrackingRepo.getGlobalListenDuration()

        div("widget-body") {

            h2 {
                +t("widgets.server_stats.title")
            }

            div("stats") {
                statCard(
                    this@ServerStatsWidget,
                    TrackRepo.count().toString(),
                    t("widgets.server_stats.tracks")
                )
                statCard(
                    this@ServerStatsWidget,
                    DateTimeUtils.formatDuration(totalDurationSeconds),
                    t("widgets.server_stats.total_duration")
                )
                statCard(
                    this@ServerStatsWidget,
                    AlbumRepo.count().toString(),
                    t("widgets.server_stats.albums")
                )
                statCard(
                    this@ServerStatsWidget,
                    ArtistRepo.count().toString(),
                    t("widgets.server_stats.artists")
                )
                statCard(
                    this@ServerStatsWidget,
                    PlaylistRepo.count().toString(),
                    t("widgets.server_stats.playlists")
                )
                statCard(
                    this@ServerStatsWidget,
                    UserRepo.count().toString(),
                    t("widgets.server_stats.users")
                )
                statCard(
                    this@ServerStatsWidget,
                    DateTimeUtils.formatDuration(totalListenDurationSeconds),
                    t("widgets.server_stats.listen_duration"
                    )
                )
            }
        }
    }
}