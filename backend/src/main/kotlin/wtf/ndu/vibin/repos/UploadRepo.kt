package wtf.ndu.vibin.repos

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction
import wtf.ndu.vibin.db.tracks.TrackEntity
import wtf.ndu.vibin.db.uploads.PendingArtistEntity
import wtf.ndu.vibin.db.uploads.PendingArtistTable
import wtf.ndu.vibin.db.uploads.PendingUploadEntity
import wtf.ndu.vibin.db.uploads.PendingUploadTable
import wtf.ndu.vibin.dto.PendingUploadDto
import wtf.ndu.vibin.dto.tracks.TrackEditDto
import wtf.ndu.vibin.parsing.Parser
import wtf.ndu.vibin.parsing.TrackInfoMetadata
import wtf.ndu.vibin.parsing.TrackMetadata
import wtf.ndu.vibin.parsing.parsers.preparser.PreParser
import wtf.ndu.vibin.processing.ThumbnailProcessor
import wtf.ndu.vibin.settings.Settings
import wtf.ndu.vibin.settings.UploadPath
import wtf.ndu.vibin.utils.ChecksumUtil
import wtf.ndu.vibin.utils.DateTimeUtils
import wtf.ndu.vibin.utils.PathUtils
import java.io.File
import java.nio.file.Files

object UploadRepo {

    /**
     * Adds a new pending upload.
     *
     * @param data The byte array of the file to upload.
     * @param fileName The name of the file.
     * @param userId The ID of the user uploading the file.
     * @return The created PendingUploadEntity, or null if the user does not exist.
     * @throws FileAlreadyExistsException if a track with the same checksum already exists.
     */
    suspend fun addUpload(data: ByteArray, fileName: String, userId: Long): PendingUploadEntity? {

        val user = UserRepo.getById(userId) ?: return null

        val checksum = ChecksumUtil.getChecksum(data)

        val filePath = PathUtils.sanitizePath("$userId${File.separator}$fileName")

        val file = PathUtils.getUploadFileFromPath(filePath)

        if (TrackRepo.getByChecksum(checksum) != null) {
            throw FileAlreadyExistsException(file)
        }
        file.parentFile.mkdirs()

        file.writeBytes(data)

        try {
            val parsed = Parser.parse(file)

            val pendingUpload = transaction {

                val upload = PendingUploadEntity.new {
                    this.filePath = filePath
                    title = parsed.trackInfo.title
                    album = parsed.trackInfo.albumName ?: "Unknown Album"
                    explicit = parsed.trackInfo.explicit ?: false
                    trackNumber = parsed.trackInfo.trackNumber
                    trackCount = parsed.trackInfo.trackCount
                    discNumber = parsed.trackInfo.discNumber
                    discCount = parsed.trackInfo.discCount
                    year = parsed.trackInfo.year
                    comment = parsed.trackInfo.comment ?: ""
                    coverUrl = parsed.trackInfo.coverImageUrl
                    uploader = user
                    tags = SizedCollection(parsed.trackInfo.tags?.mapNotNull { TagRepo.getByName(it) } ?: emptyList())
                    lyrics = parsed.trackInfo.lyrics
                }

                parsed.trackInfo.artistNames?.forEach {
                    PendingArtistEntity.new {
                        this.name = it
                        this.upload = upload
                    }
                }

                return@transaction upload
            }

            return pendingUpload
        }
        catch (e: Exception) {
            file.delete()
            throw e
        }
    }

    fun setMetadata(upload: PendingUploadEntity, metadata: TrackEditDto): PendingUploadEntity = transaction {

        upload.title = metadata.title ?: upload.title
        upload.album = metadata.albumName ?: upload.album
        upload.explicit = metadata.explicit ?: upload.explicit
        upload.trackNumber = metadata.trackNumber
        upload.trackCount = metadata.trackCount
        upload.discNumber = metadata.discNumber
        upload.discCount = metadata.discCount
        upload.year = metadata.year
        upload.comment = metadata.comment ?: upload.comment
        upload.coverUrl = metadata.imageUrl ?: upload.coverUrl
        upload.lyrics = metadata.lyrics ?: upload.lyrics

        if (metadata.tagIds != null)
            upload.tags = SizedCollection(metadata.tagIds.mapNotNull { TagRepo.getById(it) })

        if (metadata.artistNames != null) {

            PendingArtistEntity.find { PendingArtistTable.uploadId eq upload.id.value }.forEach { it.delete() }
            metadata.artistNames.forEach {
                PendingArtistEntity.new {
                    this.name = it
                    this.upload = upload
                }
            }
        }

        upload.updatedAt = DateTimeUtils.now()

        return@transaction upload
    }

    /**
     * Applies the pending upload by moving the file to its final location and creating a TrackEntity.
     *
     * @param upload The PendingUploadEntity to apply.
     * @return The created TrackEntity, or null if the operation failed.
     * @throws FileAlreadyExistsException if the target file already exists.
     */
    fun apply(upload: PendingUploadEntity): TrackEntity = transaction {

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

        val uploadedTrack = TrackRepo.createTrack(
            file = targetFile,
            metadata = TrackMetadata(
                fileInfo = fileInfo,
                trackInfo = TrackInfoMetadata(
                    title = upload.title,
                    albumName = upload.album,
                    artistNames = upload.artists.map { it.name },
                    explicit = upload.explicit,
                    trackNumber = upload.trackNumber,
                    trackCount = upload.trackCount,
                    discNumber = upload.discNumber,
                    discCount = upload.discCount,
                    year = upload.year,
                    comment = upload.comment,
                    coverImageUrl = upload.coverUrl,
                    lyrics = upload.lyrics,
                    tags = upload.tags.map { it.name },
                )
            ),
            album = AlbumRepo.getOrCreateAlbum(upload.album),
            artists = upload.artists.map { ArtistRepo.getOrCreateArtist(it.name) },
            uploader = upload.uploader
        )

        val cover = upload.coverUrl?.let {
            val data = runBlocking { Parser.downloadCoverImage(it) }
            data?.let { ThumbnailProcessor.getImage(it) }
        }

        uploadedTrack.cover = cover

        upload.delete()

        return@transaction uploadedTrack
    }

    fun delete(upload: PendingUploadEntity) = transaction {
        upload.delete()
    }

    private fun getTargetFile(pendingUploadEntity: PendingUploadEntity): File {
        val pathTemplate = Settings.get(UploadPath)

        val uploadedFile = PathUtils.getUploadFileFromPath(pendingUploadEntity.filePath)

        val replacedPath = pathTemplate
            .replace("{uploaderId}", pendingUploadEntity.uploader.id.value.toString())
            .replace("{album}", pendingUploadEntity.album)
            .replace("{title}", pendingUploadEntity.title)
            .replace("{artist}", pendingUploadEntity.artists.joinToString(", ") { it.name })
            .replace("{artists}", pendingUploadEntity.artists.joinToString(", ") { it.name })
            .replace("{name}", uploadedFile.nameWithoutExtension)
            .replace("{ext}", uploadedFile.extension)
            .replace("{sep}", File.separator)

        val safeReplacedPath = PathUtils.sanitizePath(replacedPath)
        return PathUtils.getTrackFileFromPath(safeReplacedPath)
    }

    fun getUploadsByUser(userId: Long): List<PendingUploadEntity> = transaction {
        return@transaction PendingUploadEntity.find {
            PendingUploadTable.uploaderId eq userId
        }.toList()
    }

    fun checkUploader(upload: PendingUploadEntity, userId: Long): Boolean = transaction {
        return@transaction upload.uploader.id.value == userId
    }

    fun getById(id: Long): PendingUploadEntity? = transaction {
        return@transaction PendingUploadEntity.findById(id)
    }

    fun toDto(entity: PendingUploadEntity): PendingUploadDto = transaction {
        return@transaction toDtoInternal(entity)
    }

    fun toDto(entities: List<PendingUploadEntity>): List<PendingUploadDto> = transaction {
        return@transaction entities.map { toDtoInternal(it) }
    }

    private fun toDtoInternal(entity: PendingUploadEntity): PendingUploadDto {
        return PendingUploadDto(
            id = entity.id.value,
            filePath = entity.filePath,
            title = entity.title,
            album = entity.album,
            artists = entity.artists.map { it.name },
            explicit = entity.explicit,
            trackNumber = entity.trackNumber,
            trackCount = entity.trackCount,
            discNumber = entity.discNumber,
            discCount = entity.discCount,
            year = entity.year,
            comment = entity.comment,
            coverUrl = entity.coverUrl,
            uploader = UserRepo.toDto(entity.uploader),
            tags = entity.tags.map { TagRepo.toDto(it) }
        )
    }
}