package wtf.ndu.vibin.parsing.parsers.metadata

import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.slf4j.LoggerFactory
import wtf.ndu.vibin.parsing.ParsingUtils
import wtf.ndu.vibin.parsing.TrackInfoMetadata
import wtf.ndu.vibin.parsing.parsers.FileParser
import wtf.ndu.vibin.parsing.parsers.PreparseData
import wtf.ndu.vibin.settings.server.LyricFilePathTemplate
import wtf.ndu.vibin.settings.Settings
import java.io.File
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class MetadataProvider : FileParser {

    private val logger = LoggerFactory.getLogger(MetadataProvider::class.java)

    fun Tag.getFirstNonEmpty(vararg keys: FieldKey): String? {
        for (key in keys) {
            val value = this.getFirst(key)
            if (value.isNotBlank()) {
                return value
            }
        }
        return null
    }

    fun Tag.getAllNonEmpty(vararg keys: FieldKey): List<String> {
        val results = mutableListOf<String>()
        for (key in keys) {

            val values = this.getFields(key).flatMap { field ->

                var value = field.toString()
                textTagRegex.find(value.trim())?.let { match ->
                    value = match.groups["value"]?.value?.filter { it.code != 0 } ?: value
                }

                value.split(",").map { it.trim() }
            }
            results.addAll(values)
        }
        return results.distinct()
    }

    private val textTagRegex = Regex("^Text=\"(?<value>.*)\";$")

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun parse(data: PreparseData): TrackInfoMetadata? {

        try {
            val tag = data.audioFile.tag

            val artist = tag.getFirstNonEmpty(FieldKey.ARTISTS, FieldKey.ARTIST, FieldKey.ORIGINAL_ARTIST, FieldKey.ALBUM_ARTISTS, FieldKey.ALBUM_ARTIST)
            val album = tag.getFirstNonEmpty(FieldKey.ALBUM, FieldKey.ORIGINAL_ALBUM)
            val title = tag.getFirstNonEmpty(FieldKey.TITLE)
            val track = tag.getFirstNonEmpty(FieldKey.TRACK, FieldKey.SINGLE_DISC_TRACK_NO)
            val tracks = tag.getFirstNonEmpty(FieldKey.TRACK_TOTAL)
            val discNo = tag.getFirstNonEmpty(FieldKey.DISC_NO)
            val discCount = tag.getFirstNonEmpty(FieldKey.DISC_TOTAL)
            val year = tag.getFirstNonEmpty(FieldKey.YEAR)
            val tags = tag.getAllNonEmpty(FieldKey.GENRE, FieldKey.LANGUAGE, FieldKey.MOOD, FieldKey.QUALITY, FieldKey.TAGS)
            val comment = tag.getFirstNonEmpty(FieldKey.COMMENT, FieldKey.SUBTITLE, FieldKey.DISC_SUBTITLE)
            val rating = tag.getFirst(FieldKey.RATING).takeIf { it.isNotBlank() }
            var lyrics = tag.getFirstNonEmpty(FieldKey.LYRICS)?.takeIf { it.isNotBlank() }

            if (lyrics == null) {
                val lyricsPath = Settings.get(LyricFilePathTemplate)
                    .replace("{artist}", artist ?: "")
                    .replace("{album}", album ?: "")
                    .replace("{title}", title ?: "")
                    .replace("{parentPath}", data.audioFile.file.parentFile.absolutePath)
                    .replace("{name}", data.audioFile.file.nameWithoutExtension)
                    .replace("{ext}", data.audioFile.file.extension)
                    .replace("{sep}", File.separator)
                    .trim()

                val lyricsFile = File(lyricsPath)
                if (lyricsFile.exists() && lyricsFile.isFile) {
                    try {
                        lyrics = lyricsFile.readText().takeIf { it.isNotBlank() }
                    } catch (e: Exception) {
                        logger.warn("Failed to read lyrics file at $lyricsPath: ${e.message}", e)
                    }
                }
            }

            val cover = tag.firstArtwork?.binaryData

            // 2025-08-24 -> 2025
            val parsedYear = year?.let { date -> date.split("-").firstOrNull { it.length == 4 }?.toInt() }

            // 1/5 -> 1, 5
            val parsedTracks = track?.split("/")?.mapNotNull { it.toIntOrNull() }
            val parsedTrackNo = parsedTracks?.firstOrNull()
            val parsedTrackCount = parsedTracks?.getOrNull(1) ?: tracks?.toIntOrNull()

            // 1/3 -> 1, 3
            val parsedDiscs = discNo?.split("/")?.mapNotNull { it.toIntOrNull() }
            val parsedDiscNo = parsedDiscs?.firstOrNull()
            val parsedDiscCount = parsedDiscs?.getOrNull(1) ?: discCount?.toIntOrNull()

            if (title == null) {
                logger.info("No useful metadata found in file ${data.audioFile.file.absolutePath}, skipping.")
                return null
            }

            val base64Cover = cover?.let { Base64.encode(it) }

            return TrackInfoMetadata(
                title = title,
                artists = artist?.let { ParsingUtils.splitArtistNames(it) },
                album = album,
                trackNumber = parsedTrackNo,
                trackCount = parsedTrackCount,
                discNumber = parsedDiscNo,
                discCount = parsedDiscCount,
                year = parsedYear,
                tags = tags.distinct(),
                comment = comment,
                coverImageUrl = "data:${tag.firstArtwork?.mimeType};base64,$base64Cover",
                explicit = rating?.lowercase() == "explicit",
                lyrics = lyrics
            )
        }
        catch (e: Exception) {
            logger.error("Error parsing file ${data.audioFile.file.absolutePath}", e)
            return null
        }
    }
}