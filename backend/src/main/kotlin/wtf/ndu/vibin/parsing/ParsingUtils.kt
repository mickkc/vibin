package wtf.ndu.vibin.parsing

import wtf.ndu.vibin.settings.server.ArtistNameDelimiters
import wtf.ndu.vibin.settings.Settings
import wtf.ndu.vibin.settings.server.ExtendedMetadata
import wtf.ndu.vibin.settings.server.MetadataLimit

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