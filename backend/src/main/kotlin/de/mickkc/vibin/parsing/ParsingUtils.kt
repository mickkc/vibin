package de.mickkc.vibin.parsing

import de.mickkc.vibin.settings.Settings
import de.mickkc.vibin.settings.server.ArtistNameDelimiters
import de.mickkc.vibin.settings.server.MetadataLimit

object ParsingUtils {

    /**
     * Splits a string of artist names into a list of individual artist names based on configured delimiters.
     *
     * @param artistNames The string containing artist names.
     * @return A list of individual artist names.
     */
    fun splitArtistNames(artistNames: String): List<String> {

        val delimiters = Settings.get(ArtistNameDelimiters)
        val split = artistNames.split(*delimiters.toTypedArray(), ignoreCase = true)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return split
    }

    val limit
        get() =  Settings.get(MetadataLimit).takeIf { it > 0 }
}