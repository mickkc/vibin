package wtf.ndu.vibin.parsing.parsers.preparser

import org.jaudiotagger.audio.AudioFileIO
import org.slf4j.LoggerFactory
import wtf.ndu.vibin.parsing.parsers.PreparseData
import java.io.File

object PreParser {

    private val logger = LoggerFactory.getLogger(PreParser::class.java)

    fun preParse(file: File): PreparseData? {
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
                audioFile = audioFile
            )
        }
        catch (e: Exception) {
            logger.error("Failed to pre-parse file ${file.absolutePath}: ${e.message}", e)
            return null
        }
    }
}