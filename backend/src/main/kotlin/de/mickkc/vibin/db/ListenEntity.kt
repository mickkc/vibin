package de.mickkc.vibin.db

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

object ListenTable : LongIdTable("listen") {
    val user = reference("user_id", UserTable)
    val entityId = long("entity_id")
    val type = enumeration("type", ListenType::class)
    val listenedAt = long("listened_at")
}

enum class ListenType {
    TRACK, ALBUM, ARTIST, PLAYLIST
}

class ListenEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ListenEntity>(ListenTable)

    var user by UserEntity referencedOn ListenTable.user
    var entityId by ListenTable.entityId
    var type by ListenTable.type
    var listenedAt by ListenTable.listenedAt
}