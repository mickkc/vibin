package de.mickkc.vibin.repos

import de.mickkc.vibin.db.images.ColorSchemeEntity
import de.mickkc.vibin.dto.ColorSchemeDto

object ColorSchemeRepo {

    fun toDto(entity: ColorSchemeEntity): ColorSchemeDto /*= transaction*/ {
        return ColorSchemeDto(
            primary = entity.primary,
            light = entity.light,
            dark = entity.dark
        )
    }


    fun createColorSchemeInternal(primary: String, light: String, dark: String): ColorSchemeEntity {
        return ColorSchemeEntity.new {
            this.primary = primary
            this.light = light
            this.dark = dark
        }
    }
}