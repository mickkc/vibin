package de.mickkc.vibin.processing

import de.mickkc.vibin.db.tracks.TrackEntity
import de.mickkc.vibin.repos.TrackRepo
import de.mickkc.vibin.utils.PathUtils
import de.mickkc.vibin.utils.ProcessUtil
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

object VolumeDetector {

    private val logger: Logger = LoggerFactory.getLogger(VolumeDetector::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun detectVolumeLevel(audioFile: File): VolumeLevels? {

        logger.info("Detecting volume levels for file: ${audioFile.absolutePath}")

        val cmd = arrayOf(
            "ffmpeg", "-hide_banner", "-nostats",
            "-i", audioFile.absolutePath,
            "-af", "loudnorm=I=-14:TP=-1.5:LRA=11:print_format=json",
            "-f", "null",
            "-"
        )

        val result = ProcessUtil.execute(cmd)

        if (result.isError) {
            logger.error("Error executing ffmpeg for volume detection: ${result.error}")
            return null
        }

        // FFMPEG outputs the loudnorm stats to stderr
        val output = result.error.substringAfter("[Parsed_loudnorm").substringAfter("]").trim()
        val jsonStart = output.indexOf("{")
        val jsonEnd = output.lastIndexOf("}")

        if (jsonStart == -1 || jsonEnd == -1 || jsonEnd <= jsonStart) {
            logger.error("Could not find JSON output in ffmpeg result")
            return null
        }

        val jsonString = output.substring(jsonStart, jsonEnd + 1)
        return try {
            json.decodeFromString<VolumeLevels>(jsonString)
        } catch (e: Exception) {
            logger.error("Error parsing volume levels JSON: $jsonString, ${e.message}", e)
            null
        }

    }

    @Serializable(with = VolumeLevelsSerializer::class)
    data class VolumeLevels (
        val inputI: Double,
        val inputTP: Double,
        val inputLRA: Double,
        val inputThresh: Double,
        val outputI: Double,
        val outputTP: Double,
        val outputLRA: Double,
        val outputThresh: Double,
        val normalizationType: String,
        val targetOffset: Double
    )

    object VolumeLevelsSerializer : KSerializer<VolumeLevels> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("de.mickkc.vibin.processing.VolumeLevels", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: VolumeLevels) {
            throw UnsupportedOperationException("Serialization is not supported for VolumeLevels")
        }

        override fun deserialize(decoder: Decoder): VolumeLevels {
            val jsonDecoder = decoder as JsonDecoder
            val jsonObject = jsonDecoder.decodeJsonElement().jsonObject

            return VolumeLevels(
                inputI = jsonObject["input_i"]?.jsonPrimitive?.double ?: throw IllegalArgumentException("Missing input_i"),
                inputTP = jsonObject["input_tp"]?.jsonPrimitive?.double ?: throw IllegalArgumentException("Missing input_tp"),
                inputLRA = jsonObject["input_lra"]?.jsonPrimitive?.double ?: throw IllegalArgumentException("Missing input_lra"),
                inputThresh = jsonObject["input_thresh"]?.jsonPrimitive?.double ?: throw IllegalArgumentException("Missing input_thresh"),
                outputI = jsonObject["output_i"]?.jsonPrimitive?.double ?: throw IllegalArgumentException("Missing output_i"),
                outputTP = jsonObject["output_tp"]?.jsonPrimitive?.double ?: throw IllegalArgumentException("Missing output_tp"),
                outputLRA = jsonObject["output_lra"]?.jsonPrimitive?.double ?: throw IllegalArgumentException("Missing output_lra"),
                outputThresh = jsonObject["output_thresh"]?.jsonPrimitive?.double ?: throw IllegalArgumentException("Missing output_thresh"),
                normalizationType = jsonObject["normalization_type"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing normalization_type"),
                targetOffset = jsonObject["target_offset"]?.jsonPrimitive?.double ?: throw IllegalArgumentException("Missing target_offset")
            )
        }
    }

    /**
     * Detects the volume level of the given track and updates its volumeOffset in the database.
     *
     * This function updates the TrackEntity based on the track ID and not the entity instance in case it
     * has been modified elsewhere while processing.
     */
    suspend fun detectVolumeLevel(track: TrackEntity) {
        val file = PathUtils.getTrackFileFromPath(track.path)
        val levels = detectVolumeLevel(file)
        if (levels != null) {
            TrackRepo.update(track.id.value) {
                volumeOffset = levels.targetOffset
            }
        }
        else {
            logger.error("Could not detect volume levels for track id=${track.id.value}, path=${track.path}")
        }
    }
}