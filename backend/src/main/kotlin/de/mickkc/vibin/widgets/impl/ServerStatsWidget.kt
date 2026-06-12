package de.mickkc.vibin.widgets.impl

import de.mickkc.vibin.repos.*
import de.mickkc.vibin.widgets.BaseWidget
import de.mickkc.vibin.widgets.WidgetContext
import de.mickkc.vibin.widgets.components.statCard
import kotlinx.html.div
import kotlinx.html.h2
import kotlinx.html.stream.appendHTML

class ServerStatsWidget(ctx: WidgetContext) : BaseWidget(ctx) {

    override fun render(interactive: Boolean): String {

        val totalDurationSeconds = TrackRepo.getTotalRuntimeSeconds()
        val totalListenDurationSeconds = ListenTrackingRepo.getGlobalListenDuration()

        return buildString {

            appendHTML(prettyPrint = false).div("widget-body") {

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
                        formatDuration(totalDurationSeconds),
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
                        formatDuration(totalListenDurationSeconds),
                        t("widgets.server_stats.listen_duration"
                        )
                    )
                }
            }
        }
    }

    private fun formatDuration(totalSeconds: Long): String = buildString {
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