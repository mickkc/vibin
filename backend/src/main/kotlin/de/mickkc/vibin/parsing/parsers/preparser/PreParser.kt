package de.mickkc.vibin.parsing.parsers.preparser

import de.mickkc.vibin.parsing.parsers.BaseMetadataProvider
import de.mickkc.vibin.parsing.parsers.PreparseData
import java.io.File

interface PreParser : BaseMetadataProvider {

    /**
     * @throws PreParseException preparsing failed
     */
    suspend fun preParse(file: File): PreparseData

}