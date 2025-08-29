package wtf.ndu.vibin.parsing

import java.io.File

abstract class BaseMetadataProvider {

    data class SupportedMethods(
        val fromFile: Boolean,
        val searchTrack: Boolean,
        val searchArtist: Boolean
    )

    abstract val supportedMethods: SupportedMethods

    /**
     * Parses the metadata from the given audio file.
     *
     * @param file The audio file to parse.
     * @return The extracted TrackMetadata, or null if parsing fails.
     */
    open suspend fun fromFile(file: File): TrackMetadata? = null

    open suspend fun searchTrack(query: String): List<TrackMetadata>? = null
    open suspend fun searchArtist(query: String): List<ArtistMetadata>? = null
}