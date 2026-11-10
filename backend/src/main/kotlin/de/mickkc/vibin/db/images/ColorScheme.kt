package de.mickkc.vibin.db.images

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

object ColorSchemeTable : LongIdTable() {
    val primary = varchar("primary", 7)
    val light = varchar("light", 7)
    val dark = varchar("dark", 7)
}

/**
 * Color scheme entity representing the color scheme of an image.
 *
 * @property primary The primary color in HEX format.
 * @property light The light color in HEX format.
 * @property dark The dark color in HEX format.
 */
class ColorSchemeEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ColorSchemeEntity>(ColorSchemeTable)

    var primary by ColorSchemeTable.primary
    var light by ColorSchemeTable.light
    var dark by ColorSchemeTable.dark
}