package wtf.ndu.vibin.parsing.parsers.tika

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.slf4j.LoggerFactory
import wtf.ndu.vibin.parsing.BaseMetadataParser
import wtf.ndu.vibin.parsing.ParsingUtils
import wtf.ndu.vibin.parsing.TrackMetadata
import java.io.File
import kotlin.time.Duration.Companion.seconds

class MetadataParser : BaseMetadataParser() {

    private val logger = LoggerFactory.getLogger(MetadataParser::class.java)

    override suspend fun parseFile(file: File): TrackMetadata? {

        try {
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tag

            val artist = tag.getFirst(FieldKey.ARTISTS).ifBlank { tag.getFirst(FieldKey.ARTIST) }
            val album = tag.getFirst(FieldKey.ALBUM).takeIf { it.isNotBlank() }
            val title = tag.getFirst(FieldKey.TITLE).takeIf { it.isNotBlank() }
            val track = tag.getFirst(FieldKey.TRACK).takeIf { it.isNotBlank() }
            val discNo = tag.getFirst(FieldKey.DISC_NO).takeIf { it.isNotBlank() }
            val year = tag.getFirst(FieldKey.YEAR).takeIf { it.isNotBlank() }
            val genre = tag.getFirst(FieldKey.GENRE).takeIf { it.isNotBlank() }
            val comment = tag.getFirst(FieldKey.COMMENT).takeIf { it.isNotBlank() }

            val cover = tag.firstArtwork?.binaryData
            val duration = audioFile.audioHeader.trackLength.seconds

            // 2025-08-24 -> 2025
            val parsedYear = year?.let { it.split("-").firstOrNull { it.length == 4 }?.toInt() }

            // 1/5 -> 1, 5
            val parsedTracks = track?.split("/")?.map { it.toInt() }
            val parsedTrackNo = parsedTracks?.firstOrNull()
            val parsedTrackCount = parsedTracks?.getOrNull(1)

            // 1/3 -> 1, 3
            val parsedDiscs = discNo?.split("/")?.map { it.toInt() }
            val parsedDiscNo = parsedDiscs?.firstOrNull()
            val parsedDiscCount = parsedDiscs?.getOrNull(1)

            if (title == null) {
                logger.info("No useful metadata found in file ${file.name}, skipping.")
                return null
            }

            return TrackMetadata(
                title = title,
                artistNames = artist?.let { ParsingUtils.splitArtistNames(it) },
                albumName = album,
                trackNumber = parsedTrackNo,
                trackCount = parsedTrackCount,
                discNumber = parsedDiscNo,
                discCount = parsedDiscCount,
                year = parsedYear,
                genre = genre,
                durationMs = duration.inWholeMilliseconds,
                comment = comment,
                coverImageData = cover
            )
        }
        catch (e: Exception) {
            logger.error("Error parsing file ${file.name}", e)
            return null
        }
    }
}