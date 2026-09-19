package de.mickkc.vibin.parsing.parsers

import de.mickkc.vibin.db.UserEntity
import de.mickkc.vibin.parsing.TrackInfoMetadata

interface FileParser : BaseMetadataProvider {

    suspend fun parse(data: PreparseData, user: UserEntity?): TrackInfoMetadata?

}