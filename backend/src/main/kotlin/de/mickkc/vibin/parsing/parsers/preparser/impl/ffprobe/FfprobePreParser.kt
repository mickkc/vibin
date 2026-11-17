package de.mickkc.vibin.parsing.parsers.preparser.impl.ffprobe

import com.google.gson.Gson
import de.mickkc.vibin.parsing.parsers.PreparseData
import de.mickkc.vibin.parsing.parsers.preparser.PreParseException
import de.mickkc.vibin.parsing.parsers.preparser.PreParser
import de.mickkc.vibin.parsing.parsers.preparser.impl.ffprobe.data.FfProbeResult
import de.mickkc.vibin.utils.ProcessUtil
import org.slf4j.LoggerFactory
import java.io.File

object FfprobePreParser : PreParser {

    private val gson = Gson()
    private val logger = LoggerFactory.getLogger(FfprobePreParser::class.java)

    override suspend fun preParse(file: File): PreparseData {
        val result = ProcessUtil.execute(arrayOf(
            "ffprobe",
            "-v", "quiet",
            "-print_format", "json",
            "-show_format",
            "-show_streams",
            file.absolutePath
        ), file.parentFile)

        if (!result.isSuccess) {
            throw PreParseException("Error preparsing file ${file.absolutePath} with ffprobe.\n${result.error}")
        }

        try {
            val parsed = gson.fromJson(result.output, FfProbeResult::class.java)
            val audioStream = parsed.streams.firstOrNull { it.codec_type == "audio" }
                ?: throw PreParseException("File ${file.absolutePath} does not contain any audio streams.")

            val duration = (audioStream.duration ?: parsed.format.duration)?.toDouble()?.times(1000)?.toLong()
                ?: throw PreParseException("File ${file.absolutePath} does not have a duration.")

            return PreparseData(
                durationMs = duration,
                bitrate = audioStream.bit_rate ?: parsed.format.bit_rate,
                sampleRate = audioStream.sample_rate,
                channels = audioStream.channels,
                file = file
            )
        }
        catch (e: Exception) {
            logger.error("Error preparsing file ${file.absolutePath} with Ffprobe.", e)
            throw PreParseException("Error preparsing file ${file.absolutePath} with Ffprobe.", e)
        }
    }
}