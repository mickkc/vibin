package de.mickkc.vibin.parsing.parsers.preparser.impl

import de.mickkc.vibin.parsing.parsers.PreparseData
import de.mickkc.vibin.parsing.parsers.preparser.PreParseException
import de.mickkc.vibin.parsing.parsers.preparser.PreParser
import org.jaudiotagger.audio.AudioFileIO
import org.slf4j.LoggerFactory
import java.io.File

object JAudioTaggerPreParser : PreParser {

    private val logger = LoggerFactory.getLogger(JAudioTaggerPreParser::class.java)

    override suspend fun preParse(file: File): PreparseData {
        try {
            val audioFile = AudioFileIO.read(file)

            val header = audioFile.audioHeader
            val duration = header.preciseTrackLength
            val bitrate = header.bitRate
            val sampleRate = header.sampleRate
            val channels = header.channels

            return PreparseData(
                durationMs = (duration * 1000f).toLong(),
                bitrate = bitrate,
                sampleRate = sampleRate,
                channels = channels,
                file = file
            )
        }
        catch (e: Exception) {
            logger.error("Failed to pre-parse file ${file.absolutePath}: ${e.message}", e)
            throw PreParseException("Failed to pre-parse file ${file.absolutePath}", e)
        }
    }
}