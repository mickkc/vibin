package wtf.ndu.vibin.db.tags

import org.jetbrains.exposed.sql.Table
import wtf.ndu.vibin.db.artists.ArtistTable

/**
 * Connection table between artists and tags.
 *
 * @property artist Reference to the artist.
 * @property tag Reference to the tag.
 * @property primaryKey Composite primary key consisting of artist and tag.
 */
object ArtistTagConnection : Table("artist_tag") {
    val artist = reference("artist_id", ArtistTable)
    val tag = reference("tag_id", TagTable)

    override val primaryKey = PrimaryKey(artist, tag, name = "PK_ArtistTag")
}