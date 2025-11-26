package de.mickkc.vibin.db.tracks

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object TrackRelationTable : LongIdTable("track_relation") {
    val track = reference("track_id", TrackTable).index()
    val relatedTrack = reference("related_track_id", TrackTable).index()
    val description = varchar("description", 255).default("")
    val mutual = bool("mutual").default(false)
    val reverseDescription = varchar("reverse_description", 255).nullable()
}

/**
 * Entity representing a relation between two tracks.
 *
 * @property track The primary track in the relation.
 * @property relatedTrack The related track in the relation.
 * @property description Description of the relation from the perspective of the primary track.
 * @property mutual Indicates if the relation is mutual. (related track is also related to the primary track)
 * @property reverseDescription Description of the relation from the perspective of the related track.
 */
class TrackRelationEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<TrackRelationEntity>(TrackRelationTable)

    var track by TrackEntity.Companion referencedOn TrackRelationTable.track
    var relatedTrack by TrackEntity.Companion referencedOn TrackRelationTable.relatedTrack
    var description by TrackRelationTable.description
    var mutual by TrackRelationTable.mutual
    var reverseDescription by TrackRelationTable.reverseDescription
}