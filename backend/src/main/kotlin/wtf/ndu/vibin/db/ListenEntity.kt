package wtf.ndu.vibin.db

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import wtf.ndu.vibin.db.tracks.TrackEntity
import wtf.ndu.vibin.db.tracks.TrackTable

object ListenTable : LongIdTable() {
    val user = reference("user_id", UserTable)
    val track = reference("track_id", TrackTable)
    val listenedAt = long("listened_at")
}

class ListenEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ListenEntity>(ListenTable)

    var user by UserEntity referencedOn ListenTable.user
    var track by TrackEntity referencedOn ListenTable.track
    var listenedAt by ListenTable.listenedAt
}