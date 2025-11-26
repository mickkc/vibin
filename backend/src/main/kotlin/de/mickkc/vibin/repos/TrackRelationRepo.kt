package de.mickkc.vibin.repos

import de.mickkc.vibin.db.tracks.TrackEntity
import de.mickkc.vibin.db.tracks.TrackRelationEntity
import de.mickkc.vibin.db.tracks.TrackRelationTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.transaction

object TrackRelationRepo {

    fun deleteAllRelationsForTrack(trackId: Long) = transaction {
        TrackRelationEntity.find {
            (TrackRelationTable.track eq trackId) or (TrackRelationTable.relatedTrack eq trackId)
        }.forEach { it.delete() }
    }

    fun getRelatedTracks(trackId: Long): List<Pair<TrackEntity, String>> = transaction {
        val relations = TrackRelationEntity.find {
            (TrackRelationTable.track eq trackId) or
                    (TrackRelationTable.mutual and (TrackRelationTable.relatedTrack eq trackId))
        }
        return@transaction relations.map {
            if (it.track.id.value == trackId) {
                Pair(it.relatedTrack, it.description)
            } else {
                Pair(it.track, it.reverseDescription ?: it.description)
            }
        }
    }

    fun removeRelationBetweenTracks(trackId1: Long, trackId2: Long) = transaction {
        TrackRelationEntity.find {
            ((TrackRelationTable.track eq trackId1) and (TrackRelationTable.relatedTrack eq trackId2)) or
            ((TrackRelationTable.track eq trackId2) and (TrackRelationTable.relatedTrack eq trackId1))
        }.forEach { it.delete() }
    }

    fun doesRelationExist(fromTrackId: Long, toTrackId: Long): Boolean = transaction {
        return@transaction TrackRelationEntity.find {
            ((TrackRelationTable.track eq fromTrackId) and (TrackRelationTable.relatedTrack eq toTrackId)) or
            ((TrackRelationTable.track eq toTrackId) and (TrackRelationTable.relatedTrack eq fromTrackId) and TrackRelationTable.mutual)
        }.any()
    }

    fun createRelation(
        track: TrackEntity,
        relatedTrack: TrackEntity,
        description: String,
        mutual: Boolean = false,
        reverseDescription: String? = null
    ): TrackRelationEntity = transaction {
        return@transaction TrackRelationEntity.new {
            this.track = track
            this.relatedTrack = relatedTrack
            this.description = description
            this.mutual = mutual
            this.reverseDescription = reverseDescription
        }
    }
}