package de.mickkc.vibin.widgets.impl

import de.mickkc.vibin.db.ListenType
import de.mickkc.vibin.db.albums.AlbumEntity
import de.mickkc.vibin.db.artists.ArtistEntity
import de.mickkc.vibin.db.images.ImageEntity
import de.mickkc.vibin.db.tracks.TrackEntity
import de.mickkc.vibin.dto.albums.AlbumDto
import de.mickkc.vibin.dto.artists.ArtistDto
import de.mickkc.vibin.dto.tracks.TrackDto
import de.mickkc.vibin.repos.*
import de.mickkc.vibin.utils.DateTimeUtils
import de.mickkc.vibin.widgets.BaseWidget
import de.mickkc.vibin.widgets.WidgetContext
import de.mickkc.vibin.widgets.components.itemSection
import kotlinx.html.FlowContent

abstract class ItemsWidget<T>(
    ctx: WidgetContext,
    val titleKey: String,
    val getter: () -> List<T>,
    val showPlace: Boolean = false
) : BaseWidget(ctx) {

    val items = getter()

    private fun getCover(item: T): ImageEntity? {
        val imageId = when (item) {
            is TrackDto -> item.coverId
            is AlbumDto -> item.coverId
            is ArtistDto -> item.imageId
            else -> null
        }

        return imageId?.let { ImageRepo.getById(it) } ?: when (item) {
            is TrackEntity -> TrackRepo.getCover(item)
            is AlbumEntity -> AlbumRepo.getAlbumCover(item)
            is ArtistEntity -> ArtistRepo.getImage(item)
            else -> null
        }
    }

    private fun getTitle(item: T): String {
        return when (item) {
            is TrackDto -> item.title
            is AlbumDto -> item.title
            is ArtistDto -> item.name
            is TrackEntity -> item.title
            is AlbumEntity -> item.title
            is ArtistEntity -> item.name
            else -> ""
        }
    }

    private fun getSubtitle(item: T): String {
        return when (item) {
            is TrackDto -> item.artists.joinToString { it.name }
            is AlbumEntity, is AlbumDto -> "Album"
            is ArtistEntity, is ArtistDto -> "Artist"
            is TrackEntity -> TrackRepo.getArtists(item).joinToString { it.name }
            else -> ""
        }
    }

    private fun getType(item: T): String {
        return when (item) {
            is TrackEntity, is TrackDto -> "track"
            is AlbumEntity, is AlbumDto -> "album"
            is ArtistEntity, is ArtistDto -> "artist"
            else -> ""
        }
    }

    override fun FlowContent.apply() {
        itemSection(
            widget = this@ItemsWidget,
            title = t(titleKey),
            items = items,
            getType = ::getType,
            getCover = ::getCover,
            getTitle = ::getTitle,
            getSubtitle = ::getSubtitle,
            showPlace = showPlace
        )
    }

}

class RecentTracksWidget(ctx: WidgetContext) : ItemsWidget<TrackEntity>(
    ctx,
    titleKey = "widgets.recent_tracks.title",
    getter = { ListenRepo.getRecentTracks(ctx.userId, DateTimeUtils.startOfYear(), 10) }
)

class RecentAlbumsWidget(ctx: WidgetContext) : ItemsWidget<AlbumEntity>(
    ctx,
    titleKey = "widgets.recent_albums.title",
    getter = { ListenRepo.getRecentAlbums(ctx.userId, DateTimeUtils.startOfYear(), 10) }
)

class RecentArtistsWidget(ctx: WidgetContext) : ItemsWidget<ArtistEntity>(
    ctx,
    titleKey = "widgets.recent_artists.title",
    getter = { ListenRepo.getRecentArtistsFromTracks(ctx.userId, DateTimeUtils.startOfYear(), 10) }
)

class RecentlyListenedWidget(ctx: WidgetContext) : ItemsWidget<Any>(
    ctx,
    titleKey = "widgets.recently_listened.title",
    getter = {
        ListenRepo.getRecentNonTrackDtos(
            ctx.userId,
            limit = 10,
            allowedTypes = listOf(ListenType.TRACK, ListenType.ALBUM, ListenType.ARTIST)
        ).map { it.value }
    }
)

class MostListenedTracksWidget(ctx: WidgetContext) : ItemsWidget<TrackEntity>(
    ctx,
    titleKey = "widgets.most_listened_tracks.title",
    getter = {
        ListenRepo
            .getMostListenedTracks(ctx.userId, DateTimeUtils.startOfYear())
            .toList()
            .sortedByDescending { it.second }
            .map { it.first }
    },
    showPlace = true
)

class MostListenedAlbumsWidget(ctx: WidgetContext) : ItemsWidget<AlbumEntity>(
    ctx,
    titleKey = "widgets.most_listened_albums.title",
    getter = {
        ListenRepo
            .getMostListenedAlbums(ctx.userId, DateTimeUtils.startOfYear())
            .toList()
            .sortedByDescending { it.second }
            .map { it.first }
    },
    showPlace = true
)

class MostListenedArtistsWidget(ctx: WidgetContext) : ItemsWidget<ArtistEntity>(
    ctx,
    titleKey = "widgets.most_listened_artists.title",
    getter = {
        ListenRepo
            .getMostListenedArtistsByTracks(ctx.userId, DateTimeUtils.startOfYear())
            .toList()
            .sortedByDescending { it.second }
            .map { it.first }
    },
    showPlace = true
)

class MostListenedWidget(ctx: WidgetContext) : ItemsWidget<Any>(
    ctx,
    titleKey = "widgets.most_listened.title",
    getter = {
        ListenRepo.getMostListenedToAsDtos(
            ctx.userId,
            DateTimeUtils.startOfYear(),
            false,
            listOf(ListenType.TRACK, ListenType.ARTIST, ListenType.ALBUM)
        )
        .toList()
        .sortedByDescending { it.second }
        .map { it.first.value }
    },
    showPlace = true
)