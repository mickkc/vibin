package wtf.ndu.vibin.parsing

import java.io.File

abstract class BaseMetadataParser {

    /**
     * Parses the metadata from the given audio file.
     *
     * @param file The audio file to parse.
     * @return The extracted TrackMetadata, or null if parsing fails.
     */
    abstract suspend fun parseFile(file: File): TrackMetadata?
}