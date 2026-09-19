package de.mickkc.vibin.parsing.parsers

import de.mickkc.vibin.db.UserEntity

interface BaseMetadataProvider {
    fun isAvailable(user: UserEntity?): Boolean = true
}