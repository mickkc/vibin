package de.mickkc.vibin.uploads

import io.ktor.server.plugins.*
import kotlinx.coroutines.sync.Mutex
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import de.mickkc.vibin.db.tracks.TrackEntity
import de.mickkc.vibin.dto.PendingUploadDto
import de.mickkc.vibin.dto.tracks.TrackEditDto
import de.mickkc.vibin.parsing.Parser
import de.mickkc.vibin.parsing.parsers.preparser.PreParser
import de.mickkc.vibin.processing.ThumbnailProcessor
import de.mickkc.vibin.repos.*
import de.mickkc.vibin.settings.Settings
import de.mickkc.vibin.settings.server.UploadPath
import de.mickkc.vibin.utils.ChecksumUtil
import de.mickkc.vibin.utils.DateTimeUtils
import de.mickkc.vibin.utils.PathUtils
import java.io.File
import java.nio.file.Files
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object UploadManager {

    private val storage = ConcurrentHashMap<String, PendingUpload>()
    private val mutexMap = ConcurrentHashMap<String, Mutex>()
    private val logger: Logger = LoggerFactory.getLogger(UploadManager::class.java)

    /**
     * Adds a new pending upload.
     *
     * @param data The byte array of the file to upload.
     * @param fileName The name of the file.
     * @param userId The ID of the user uploading the file.
     * @return The created PendingUploadEntity, or null if the user does not exist.
     * @throws FileAlreadyExistsException if a track with the same checksum already exists.
     */
    suspend fun addUpload(data: ByteArray, fileName: String, userId: Long): PendingUpload? {

        val id = UUID.randomUUID().toString()

        val mutex = getMutex(id)

        mutex.lock()

        try {
            val user = UserRepo.getById(userId) ?: return null

            val checksum = ChecksumUtil.getChecksum(data)

            val filePath = PathUtils.sanitizePath("$userId${File.separator}$fileName")

            val file = PathUtils.getUploadFileFromPath(filePath)

            if (TrackRepo.getByChecksum(checksum) != null) {
                throw FileAlreadyExistsException(file)
            }
            file.parentFile.mkdirs()

            file.writeBytes(data)

            var coverFile: File? = null

            try {
                val parsed = Parser.parse(file)

                if (parsed.trackInfo.coverImageUrl != null) {
                    val coverData = Parser.downloadCoverImage(parsed.trackInfo.coverImageUrl)
                    if (coverData != null) {
                        coverFile = PathUtils.getUploadFileFromPath("$filePath.cover.jpg")
                        coverFile.writeBytes(coverData)
                    }
                }

                val pendingUpload = PendingUpload(
                    id = id,
                    filePath = filePath,
                    title = parsed.trackInfo.title,
                    album = (
                            if (parsed.trackInfo.album != null)
                                AlbumRepo.getOrCreateAlbum(parsed.trackInfo.album, parsed.trackInfo.artists?.firstOrNull())
                            else
                                AlbumRepo.getUnknownAlbum()
                            ).id.value,
                    artists = parsed.trackInfo.artists?.map { ArtistRepo.getOrCreateArtist(it).id.value } ?: emptyList(),
                    tags = parsed.trackInfo.tags?.map { TagRepo.getOrCreateTag(it).id.value } ?: mutableListOf(),
                    explicit = parsed.trackInfo.explicit ?: false,
                    trackNumber = parsed.trackInfo.trackNumber,
                    trackCount = parsed.trackInfo.trackCount,
                    discNumber = parsed.trackInfo.discNumber,
                    discCount = parsed.trackInfo.discCount,
                    year = parsed.trackInfo.year,
                    comment = parsed.trackInfo.comment ?: "",
                    lyrics = parsed.trackInfo.lyrics ?: Parser.searchLyricsAuto(parsed),
                    uploaderId = user.id.value,
                    lastUpdated = DateTimeUtils.now()
                )

                storage[id] = pendingUpload

                return pendingUpload
            } catch (e: Exception) {
                logger.error("Error parsing uploaded file for user ID $userId: ${e.message}", e)
                file.delete()
                coverFile?.delete()
                storage.remove(id)
                mutexMap.remove(id)
                throw e
            }
        }
        finally {
            mutex.unlock()
        }
    }

    /**
     * Sets the metadata for a pending upload.
     *
     * @param id The ID of the pending upload.
     * @param metadata The TrackEditDto containing the new metadata.
     * @return The updated PendingUploadEntity.
     */
    suspend fun setMetadata(id: String, metadata: TrackEditDto): PendingUpload {

        val mutex = getMutex(id)
        mutex.lock()

        try {
            val upload = storage[id] ?: throw NotFoundException()

            upload.title = metadata.title ?: upload.title
            upload.album = metadata.album ?: upload.album
            upload.artists = metadata.artists ?: upload.artists
            upload.tags = metadata.tags ?: upload.tags
            upload.explicit = metadata.explicit ?: upload.explicit
            upload.trackNumber = metadata.trackNumber
            upload.trackCount = metadata.trackCount
            upload.discNumber = metadata.discNumber
            upload.discCount = metadata.discCount
            upload.year = metadata.year
            upload.comment = metadata.comment ?: upload.comment
            upload.lyrics = metadata.lyrics ?: upload.lyrics

            if (metadata.imageUrl != null) {
                if (metadata.imageUrl.isEmpty()) {
                    // Reset cover if empty string is provided
                    val coverFile = getCoverFile(upload)
                    if (coverFile != null && coverFile.exists()) {
                        coverFile.delete()
                    }
                }
                else {
                    // Download & Save cover image
                    Parser.downloadCoverImage(metadata.imageUrl)?.let {
                        val coverFile = PathUtils.getUploadFileFromPath("${upload.filePath}.cover.jpg")
                        coverFile.writeBytes(it)
                    }
                }
            }

            return upload
        }
        finally {
            mutex.unlock()
        }
    }

    /**
     * Applies the pending upload by moving the file to its final location and creating a TrackEntity.
     *
     * @param id The ID of the pending upload.
     * @return The created TrackEntity, or null if the operation failed.
     * @throws FileAlreadyExistsException if the target file already exists.
     */
    suspend fun apply(id: String): TrackEntity {

        val mutex = getMutex(id)
        mutex.lock()

        try {

            val upload = storage[id] ?: throw NotFoundException()

            val file = PathUtils.getUploadFileFromPath(upload.filePath)

            if (!file.exists()) {
                throw IllegalStateException("Upload file does not exist: ${file.absolutePath}")
            }

            val fileInfo = PreParser.preParse(file)

            val targetFile = getTargetFile(upload)
            targetFile.parentFile.mkdirs()

            if (targetFile.exists()) {
                throw FileAlreadyExistsException(targetFile)
            }

            Files.move(file.toPath(), targetFile.toPath())

            val coverFile = getCoverFile(upload)

            val cover = coverFile?.let { file ->
                ThumbnailProcessor.getImage(file.readBytes()).also {
                    file.delete()
                }
            }

            val track = TrackRepo.createTrack(
                file = targetFile,
                preparseData = fileInfo,
                upload = upload,
                cover = cover
            )

            storage.remove(upload.id)

            return track
        }
        finally {
            mutex.unlock()
        }
    }

    /**
     * Deletes a pending upload and its associated file.
     *
     * @param id The ID of the pending upload.
     */
    suspend fun delete(id: String) {

        val mutex = getMutex(id)
        mutex.lock()

        try {
            val upload = storage[id] ?: throw NotFoundException()

            val file = PathUtils.getUploadFileFromPath(upload.filePath)
            if (file.exists()) {
                file.delete()
            }

            val coverFile = getCoverFile(upload)
            if (coverFile != null && coverFile.exists()) {
                coverFile.delete()
            }

            storage.remove(upload.id)
        }
        finally {
            mutex.unlock()
            mutexMap.remove(id)
        }
    }

    /**
     * Generates the target file path for a pending upload based on the upload path template.
     *
     * @param pendingUploadEntity The pending upload entity.
     * @return The target File object representing the final location of the uploaded track.
     */
    private fun getTargetFile(pendingUploadEntity: PendingUpload): File {
        val pathTemplate = Settings.get(UploadPath)

        val uploadedFile = PathUtils.getUploadFileFromPath(pendingUploadEntity.filePath)

        val replacedPath = pathTemplate
            .replace("{uploaderId}", pendingUploadEntity.uploaderId.toString())
            .replace("{album}", AlbumRepo.getById(pendingUploadEntity.album)?.title ?: AlbumRepo.UNKNOWN_ALBUM_NAME)
            .replace("{title}", pendingUploadEntity.title)
            .replace("{artist}", ArtistRepo.idsToDisplayString(pendingUploadEntity.artists))
            .replace("{artists}", ArtistRepo.idsToDisplayString(pendingUploadEntity.artists))
            .replace("{name}", uploadedFile.nameWithoutExtension)
            .replace("{ext}", uploadedFile.extension)
            .replace("{sep}", File.separator)

        val safeReplacedPath = PathUtils.sanitizePath(replacedPath)
        return PathUtils.getTrackFileFromPath(safeReplacedPath)
    }

    fun getCoverFile(pendingUploadEntity: PendingUpload): File? {
        return PathUtils.getUploadFileFromPath("${pendingUploadEntity.filePath}.cover.jpg").takeIf {
            it.exists()
        }
    }

    /**
     * Retrieves or creates a mutex for the given upload ID.
     *
     * @param id The ID of the pending upload.
     * @return The Mutex associated with the given ID.
     */
    private fun getMutex(id: String): Mutex {
        return mutexMap.computeIfAbsent(id) { Mutex() }
    }

    /**
     * Retrieves all pending uploads for a specific user.
     *
     * @param userId The ID of the user.
     * @return A list of PendingUploadEntity objects uploaded by the specified user.
     */
    fun getUploadsByUser(userId: Long): List<PendingUpload> {
        return storage.values.filter { it.uploaderId == userId }
    }

    /**
     * Retrieves a pending upload by its ID.
     *
     * @param id The ID of the pending upload.
     * @return The PendingUploadEntity if found, otherwise null.
     */
    fun getById(id: String): PendingUpload? {
        return storage[id]
    }

    fun toDto(upload: PendingUpload): PendingUploadDto {
        return PendingUploadDto(
            id = upload.id,
            filePath = upload.filePath,
            title = upload.title,
            album = AlbumRepo.getById(upload.album)?.let {
                AlbumRepo.toDto(it)
            },
            artists = upload.artists.mapNotNull {
                ArtistRepo.getById(it)
            }.let {
                ArtistRepo.toDto(it)
            },
            tags = upload.tags.mapNotNull {
                TagRepo.getById(it)
            }.let {
                    TagRepo.toDto(it)
            },
            explicit = upload.explicit,
            trackNumber = upload.trackNumber,
            trackCount = upload.trackCount,
            discNumber = upload.discNumber,
            discCount = upload.discCount,
            year = upload.year,
            comment = upload.comment,
            lyrics = upload.lyrics,
            uploaderId = upload.uploaderId,
            lastUpdated = upload.lastUpdated
        )
    }
}