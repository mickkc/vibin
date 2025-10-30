package wtf.ndu.vibin.uploads

import io.ktor.server.plugins.*
import kotlinx.coroutines.sync.Mutex
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import wtf.ndu.vibin.db.tracks.TrackEntity
import wtf.ndu.vibin.dto.PendingUploadDto
import wtf.ndu.vibin.dto.tracks.TrackEditDto
import wtf.ndu.vibin.parsing.Parser
import wtf.ndu.vibin.parsing.parsers.preparser.PreParser
import wtf.ndu.vibin.processing.ThumbnailProcessor
import wtf.ndu.vibin.repos.*
import wtf.ndu.vibin.settings.Settings
import wtf.ndu.vibin.settings.UploadPath
import wtf.ndu.vibin.utils.ChecksumUtil
import wtf.ndu.vibin.utils.DateTimeUtils
import wtf.ndu.vibin.utils.PathUtils
import java.io.File
import java.nio.file.Files
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object UploadManager {

    private val storage = ConcurrentHashMap<String, PendingUpload>()
    private val mutex = Mutex()
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

            val id = UUID.randomUUID().toString()

            try {
                val parsed = Parser.parse(file)

                val pendingUpload = PendingUpload(
                    id = id,
                    filePath = filePath,
                    title = parsed.trackInfo.title,
                    album = AlbumRepo.getOrCreateAlbum(parsed.trackInfo.album ?: AlbumRepo.UNKNOWN_ALBUM_NAME).id.value,
                    artists = parsed.trackInfo.artists?.map { ArtistRepo.getOrCreateArtist(it).id.value } ?: emptyList(),
                    tags = parsed.trackInfo.tags?.map { TagRepo.getOrCreateTag(it).id.value } ?: mutableListOf(),
                    explicit = parsed.trackInfo.explicit ?: false,
                    trackNumber = parsed.trackInfo.trackNumber,
                    trackCount = parsed.trackInfo.trackCount,
                    discNumber = parsed.trackInfo.discNumber,
                    discCount = parsed.trackInfo.discCount,
                    year = parsed.trackInfo.year,
                    comment = parsed.trackInfo.comment ?: "",
                    lyrics = parsed.trackInfo.lyrics,
                    coverUrl = parsed.trackInfo.coverImageUrl,
                    uploaderId = user.id.value,
                    lastUpdated = DateTimeUtils.now()
                )

                storage[id] = pendingUpload

                return pendingUpload
            } catch (e: Exception) {
                logger.error("Error parsing uploaded file for user ID $userId: ${e.message}", e)
                file.delete()
                storage.remove(id)
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
     * @param upload The PendingUploadEntity to update.
     * @param metadata The TrackEditDto containing the new metadata.
     * @return The updated PendingUploadEntity.
     */
    suspend fun setMetadata(id: String, metadata: TrackEditDto): PendingUpload {

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
            upload.coverUrl = metadata.imageUrl ?: upload.coverUrl
            upload.lyrics = metadata.lyrics ?: upload.lyrics

            return upload
        }
        finally {
            mutex.unlock()
        }
    }

    /**
     * Applies the pending upload by moving the file to its final location and creating a TrackEntity.
     *
     * @param upload The PendingUploadEntity to apply.
     * @return The created TrackEntity, or null if the operation failed.
     * @throws FileAlreadyExistsException if the target file already exists.
     */
    suspend fun apply(id: String): TrackEntity {

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

            val cover = upload.coverUrl?.let { url ->
                val data = Parser.downloadCoverImage(url)
                data?.let { ThumbnailProcessor.getImage(it) }
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

    suspend fun delete(id: String) {

        mutex.lock()

        try {
            val upload = storage[id] ?: throw NotFoundException()

            val file = PathUtils.getUploadFileFromPath(upload.filePath)
            if (file.exists()) {
                file.delete()
            }
            storage.remove(upload.id)
        }
        finally {
            mutex.unlock()
        }
    }

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

    fun getUploadsByUser(userId: Long): List<PendingUpload> {
        return storage.values.filter { it.uploaderId == userId }
    }

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
            coverUrl = upload.coverUrl,
            uploaderId = upload.uploaderId,
            lastUpdated = upload.lastUpdated
        )
    }
}