package wtf.ndu.vibin.uploads

import wtf.ndu.vibin.db.tracks.TrackEntity
import wtf.ndu.vibin.dto.IdOrNameDto
import wtf.ndu.vibin.dto.tracks.TrackEditDto
import wtf.ndu.vibin.parsing.Parser
import wtf.ndu.vibin.parsing.TrackInfoMetadata
import wtf.ndu.vibin.parsing.TrackMetadata
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
                album = parsed.trackInfo.album ?: IdOrNameDto.nameWithFallback("Unknown Album"),
                artists = parsed.trackInfo.artists ?: mutableListOf(),
                tags = parsed.trackInfo.tags ?: mutableListOf(),
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
        }
        catch (e: Exception) {
            file.delete()
            storage.remove(id)
            throw e
        }
    }

    /**
     * Sets the metadata for a pending upload.
     *
     * @param upload The PendingUploadEntity to update.
     * @param metadata The TrackEditDto containing the new metadata.
     * @return The updated PendingUploadEntity.
     */
    fun setMetadata(upload: PendingUpload, metadata: TrackEditDto): PendingUpload {

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

        refresh(upload)
        return upload
    }

    /**
     * Applies the pending upload by moving the file to its final location and creating a TrackEntity.
     *
     * @param upload The PendingUploadEntity to apply.
     * @return The created TrackEntity, or null if the operation failed.
     * @throws FileAlreadyExistsException if the target file already exists.
     */
    suspend fun apply(upload: PendingUpload): TrackEntity {

        refresh(upload)

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
            metadata = TrackMetadata(
                fileInfo = fileInfo,
                trackInfo = TrackInfoMetadata(
                    title = upload.title,
                    album = upload.album,
                    artists = upload.artists,
                    explicit = upload.explicit,
                    trackNumber = upload.trackNumber,
                    trackCount = upload.trackCount,
                    discNumber = upload.discNumber,
                    discCount = upload.discCount,
                    year = upload.year,
                    comment = upload.comment,
                    coverImageUrl = upload.coverUrl,
                    lyrics = upload.lyrics,
                    tags = upload.tags,
                ),
            ),
            cover = cover,
            uploader = UserRepo.getById(upload.uploaderId)
        )

        storage.remove(upload.id)

        return track
    }

    fun delete(upload: PendingUpload) {
        val file = PathUtils.getUploadFileFromPath(upload.filePath)
        if (file.exists()) {
            file.delete()
        }
        storage.remove(upload.id)
    }

    fun refresh(upload: PendingUpload) {

        upload.album = AlbumRepo.refreshAlbumName(upload.album) ?: IdOrNameDto.nameWithFallback("Unknown Album")
        upload.artists = ArtistRepo.refreshArtistNames(upload.artists)
        upload.tags = TagRepo.refreshTagNames(upload.tags)

        upload.lastUpdated = DateTimeUtils.now()
        storage[upload.id] = upload
    }

    private fun getTargetFile(pendingUploadEntity: PendingUpload): File {
        val pathTemplate = Settings.get(UploadPath)

        val uploadedFile = PathUtils.getUploadFileFromPath(pendingUploadEntity.filePath)

        val replacedPath = pathTemplate
            .replace("{uploaderId}", pendingUploadEntity.uploaderId.toString())
            .replace("{album}", pendingUploadEntity.album.name)
            .replace("{title}", pendingUploadEntity.title)
            .replace("{artist}", pendingUploadEntity.artists.joinToString(", ") { it.name })
            .replace("{artists}", pendingUploadEntity.artists.joinToString(", ") { it.name })
            .replace("{name}", uploadedFile.nameWithoutExtension)
            .replace("{ext}", uploadedFile.extension)
            .replace("{sep}", File.separator)

        val safeReplacedPath = PathUtils.sanitizePath(replacedPath)
        return PathUtils.getTrackFileFromPath(safeReplacedPath)
    }

    fun getUploadsByUser(userId: Long): List<PendingUpload> {
        return storage.values.filter { it.uploaderId == userId }.onEach { refresh(it) }
    }

    fun getById(id: String): PendingUpload? {
        return storage[id]?.apply {
            refresh(this)
        }
    }
}