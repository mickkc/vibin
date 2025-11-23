package de.mickkc.vibin.db.artists

import org.jetbrains.exposed.sql.Table
import de.mickkc.vibin.db.tracks.TrackTable

/**
 * Connection table between tracks and artists.
 *
 * @property track Reference to the track.
 * @property artist Reference to the artist.
 * @primaryKey Composite primary key consisting of track and artist.
 */
object TrackArtistConnection : Table("track_artist") {
    val track = reference("track_id", TrackTable)
    val artist = reference("artist_id", ArtistTable)

    override val primaryKey = PrimaryKey(track, artist, name = "PK_TrackArtist")
}