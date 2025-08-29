package wtf.ndu.vibin.parsing

import wtf.ndu.vibin.parsing.parsers.deezer.DeezerParser
import wtf.ndu.vibin.parsing.parsers.itunes.ItunesParser
import wtf.ndu.vibin.parsing.parsers.tika.MetadataParser
import wtf.ndu.vibin.settings.FallbackMetadataSource
import wtf.ndu.vibin.settings.PrimaryMetadataSource
import wtf.ndu.vibin.settings.Settings
import java.io.File

object Parser {

    val parsers = mapOf(
        "Metadata" to MetadataParser(),
        "Deezer" to DeezerParser(),
        "iTunes" to ItunesParser()
    )

    /**
     * Tries to parse the given file using the configured primary and fallback metadata sources.
     *
     * @param file The audio file to parse.
     * @return The parsed TrackMetadata, or null if parsing failed with both sources.
     */
    suspend fun parse(file: File): TrackMetadata {

        val sources = listOf(Settings.get(PrimaryMetadataSource), Settings.get(FallbackMetadataSource))

        for (source in sources) {
            val metadata = parsers[source]?.parseFile(file)
            if (metadata != null) {
                return metadata
            }
        }

        return TrackMetadata(
            title = file.nameWithoutExtension,
            artistNames = emptyList(),
            albumName = "Unknown Album",
            durationMs = null,
            explicit = false,
            coverImageData = null
        )
    }
}