package de.mickkc.vibin.db

import org.jetbrains.exposed.sql.Table

object FavoriteTable : Table("favorite") {
    val userId = reference("user_id", UserTable.id)
    val entityType = enumerationByName<FavoriteType>("entity_type", 10)
    val entityId = long("entity_id")
    val place = integer("place")

    override val primaryKey = PrimaryKey(userId, entityType, place, name = "PK_Favorite_User_Entity")
}

enum class FavoriteType {
    TRACK,
    ALBUM,
    ARTIST,
}